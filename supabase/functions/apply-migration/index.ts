import { handleCors, jsonResponse, errorResponse } from '../_shared/cors.ts'
import { createAdminClient, requireAuth } from '../_shared/supabase.ts'
import { verifyMigrationChecksum } from '../_shared/github.ts'

interface ApplyMigrationRequest {
  name: string
  sql: string
  checksum: string
}

Deno.serve(async (req) => {
  // Handle CORS
  const corsResponse = handleCors(req)
  if (corsResponse) return corsResponse

  try {
    // Require authentication (any authenticated user can run migrations)
    const user = await requireAuth(req)
    console.log(`Apply migration requested by user: ${user.id}`)

    // Parse request
    const body: ApplyMigrationRequest = await req.json()
    const { name, sql, checksum } = body

    if (!name || !sql || !checksum) {
      return errorResponse('Missing required fields: name, sql, checksum', 400)
    }

    const adminClient = createAdminClient()

    // 1. Check if migration already applied
    const { data: existingMigration } = await adminClient
      .from('schema_migrations')
      .select('name, success')
      .eq('name', name)
      .single()

    if (existingMigration?.success) {
      return jsonResponse({
        success: false,
        alreadyApplied: true,
        message: `Migration ${name} has already been applied`,
      })
    }

    // 2. Verify checksum against GitHub
    console.log(`Verifying migration ${name} against GitHub...`)
    const verification = await verifyMigrationChecksum(name, sql, checksum)

    if (!verification.valid) {
      console.error(`Migration verification failed: ${verification.error}`)
      return errorResponse(verification.error ?? 'Migration verification failed', 403)
    }

    console.log(`Migration ${name} verified successfully`)

    // 3. Execute migration
    const startTime = Date.now()

    try {
      // Use the apply_migration RPC function if available
      const { data: result, error: rpcError } = await adminClient.rpc('apply_migration', {
        p_name: name,
        p_sql: sql,
        p_checksum: checksum,
        p_applied_by: user.id,
      })

      if (rpcError) {
        // If apply_migration doesn't exist, execute directly
        console.log('apply_migration RPC not available, executing directly...')

        // Execute SQL directly (this requires service_role)
        const { error: execError } = await adminClient.rpc('exec_sql', {
          sql_query: sql,
        })

        if (execError) {
          throw new Error(`Failed to execute migration: ${execError.message}`)
        }

        const executionTime = Date.now() - startTime

        // Record migration manually
        await adminClient.from('schema_migrations').upsert({
          name,
          checksum,
          applied_at: Date.now(),
          applied_by: user.id,
          success: true,
          execution_time_ms: executionTime,
        })

        return jsonResponse({
          success: true,
          alreadyApplied: false,
          message: `Migration ${name} applied successfully`,
          executionTimeMs: executionTime,
        })
      }

      // Return result from RPC
      return jsonResponse(result)

    } catch (execError) {
      const executionTime = Date.now() - startTime

      // Record failed migration
      await adminClient.from('schema_migrations').upsert({
        name,
        checksum,
        applied_at: Date.now(),
        applied_by: user.id,
        success: false,
        execution_time_ms: executionTime,
        error_message: execError.message,
      })

      return errorResponse(`Migration failed: ${execError.message}`, 500)
    }

  } catch (error) {
    console.error('Apply migration error:', error)
    if (error.message === 'Authentication required') {
      return errorResponse('Authentication required', 401)
    }
    return errorResponse(error.message ?? 'Unknown error', 500)
  }
})
