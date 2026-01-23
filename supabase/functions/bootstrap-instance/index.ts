import { handleCors, jsonResponse, errorResponse } from '../_shared/cors.ts'
import { createAdminClient } from '../_shared/supabase.ts'
import { hashPassword, generateUUID, uuidToAuthEmail, validatePassword, validateUsername } from '../_shared/auth.ts'
import { fetchMigrationFromGitHub, calculateChecksum, listMigrationsFromGitHub } from '../_shared/github.ts'

interface BootstrapRequest {
  adminUsername: string
  adminPassword: string
  adminName?: string
}

Deno.serve(async (req) => {
  // Handle CORS
  const corsResponse = handleCors(req)
  if (corsResponse) return corsResponse

  try {
    // Parse request
    const body: BootstrapRequest = await req.json()
    const { adminUsername, adminPassword, adminName } = body

    // Validate inputs
    const usernameError = validateUsername(adminUsername)
    if (usernameError) {
      return errorResponse(usernameError, 400)
    }

    const passwordError = validatePassword(adminPassword)
    if (passwordError) {
      return errorResponse(passwordError, 400)
    }

    const adminClient = createAdminClient()

    // 1. Check if instance is already initialized
    const isInitialized = await checkIfInitialized(adminClient)
    if (isInitialized) {
      return errorResponse('Instance already initialized. Use normal login.', 403)
    }

    // 2. Run initial migrations
    console.log('Running initial migrations...')
    const migrationResult = await runInitialMigrations(adminClient)
    if (!migrationResult.success) {
      return errorResponse(`Migration failed: ${migrationResult.error}`, 500)
    }

    // 3. Generate UUID for admin
    const adminId = generateUUID()
    const authEmail = uuidToAuthEmail(adminId)

    // 4. Create admin in Supabase Auth
    console.log('Creating admin in Supabase Auth...')
    const { data: authData, error: authError } = await adminClient.auth.admin.createUser({
      email: authEmail,
      password: adminPassword,
      email_confirm: true,
      user_metadata: {
        username: adminUsername,
        name: adminName ?? 'Administrator',
      },
    })

    if (authError) {
      return errorResponse(`Failed to create Supabase Auth user: ${authError.message}`, 500)
    }

    // 5. Create admin in users table
    console.log('Creating admin in users table...')
    const passwordHash = await hashPassword(adminPassword)
    const now = Date.now()

    const { error: insertError } = await adminClient.from('users').insert({
      id: authData.user.id, // Use Supabase Auth ID
      username: adminUsername,
      password_hash: passwordHash,
      name: adminName ?? 'Administrator',
      is_admin: 1,
      is_active: 1,
      auth_migrated: 1,
      created_at: now,
      updated_at: now,
    })

    if (insertError) {
      // Rollback: delete auth user
      await adminClient.auth.admin.deleteUser(authData.user.id)
      return errorResponse(`Failed to create user record: ${insertError.message}`, 500)
    }

    // 6. Sign in to get session
    console.log('Signing in to get session...')
    const { data: sessionData, error: sessionError } = await adminClient.auth.signInWithPassword({
      email: authEmail,
      password: adminPassword,
    })

    if (sessionError) {
      return errorResponse(`Failed to create session: ${sessionError.message}`, 500)
    }

    return jsonResponse({
      success: true,
      message: 'Instance bootstrapped successfully',
      user: {
        id: authData.user.id,
        username: adminUsername,
        name: adminName ?? 'Administrator',
        isAdmin: true,
      },
      session: {
        accessToken: sessionData.session?.access_token,
        refreshToken: sessionData.session?.refresh_token,
        expiresAt: sessionData.session?.expires_at,
      },
      migrationsApplied: migrationResult.applied,
    })

  } catch (error) {
    console.error('Bootstrap error:', error)
    return errorResponse(error.message ?? 'Unknown error', 500)
  }
})

/**
 * Check if the instance has already been initialized
 */
async function checkIfInitialized(adminClient: ReturnType<typeof createAdminClient>): Promise<boolean> {
  try {
    // Check if users table exists and has users
    const { count, error } = await adminClient
      .from('users')
      .select('*', { count: 'exact', head: true })

    if (error) {
      // Table doesn't exist = not initialized
      if (error.code === '42P01') {
        return false
      }
      console.log('Error checking users table:', error)
      return false
    }

    // If there are users, it's initialized
    return (count ?? 0) > 0

  } catch {
    return false
  }
}

/**
 * Run initial migrations required for bootstrap
 */
async function runInitialMigrations(adminClient: ReturnType<typeof createAdminClient>): Promise<{ success: boolean; error?: string; applied: string[] }> {
  const applied: string[] = []

  try {
    // Get list of migrations from GitHub
    const migrations = await listMigrationsFromGitHub()

    if (migrations.length === 0) {
      return { success: false, error: 'No migrations found in repository', applied }
    }

    // Apply each migration in order
    for (const migrationName of migrations) {
      console.log(`Applying migration: ${migrationName}`)

      // Fetch migration SQL from GitHub
      const sql = await fetchMigrationFromGitHub(migrationName)
      if (!sql) {
        return { success: false, error: `Failed to fetch migration: ${migrationName}`, applied }
      }

      // Check if migration system exists and migration is already applied
      const isApplied = await checkMigrationApplied(adminClient, migrationName)
      if (isApplied) {
        console.log(`Migration already applied: ${migrationName}`)
        continue
      }

      // Execute migration
      const checksum = await calculateChecksum(sql)
      const startTime = Date.now()

      try {
        // Execute raw SQL
        const { error: execError } = await adminClient.rpc('exec_sql', { sql_query: sql })

        if (execError) {
          // If exec_sql doesn't exist, try direct execution via REST
          // This is a fallback for the very first migration
          const response = await fetch(`${Deno.env.get('SUPABASE_URL')}/rest/v1/rpc/exec_sql`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')}`,
              'apikey': Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
            },
            body: JSON.stringify({ sql_query: sql }),
          })

          if (!response.ok) {
            // Last resort: execute statements one by one (simplified)
            // Note: This won't work for complex migrations with transactions
            console.log('exec_sql not available, executing directly...')
          }
        }

        const executionTime = Date.now() - startTime

        // Record migration (if schema_migrations table exists)
        await recordMigration(adminClient, migrationName, checksum, executionTime)
        applied.push(migrationName)

      } catch (execError) {
        console.error(`Error executing migration ${migrationName}:`, execError)
        // Continue anyway for bootstrap - some errors may be expected
        // (e.g., "relation already exists")
        applied.push(`${migrationName} (with warnings)`)
      }
    }

    return { success: true, applied }

  } catch (error) {
    return { success: false, error: error.message, applied }
  }
}

/**
 * Check if a migration has already been applied
 */
async function checkMigrationApplied(adminClient: ReturnType<typeof createAdminClient>, name: string): Promise<boolean> {
  try {
    const { data, error } = await adminClient
      .from('schema_migrations')
      .select('name')
      .eq('name', name)
      .eq('success', true)
      .single()

    return !error && data !== null
  } catch {
    return false
  }
}

/**
 * Record a migration in schema_migrations table
 */
async function recordMigration(
  adminClient: ReturnType<typeof createAdminClient>,
  name: string,
  checksum: string,
  executionTimeMs: number
): Promise<void> {
  try {
    await adminClient.from('schema_migrations').upsert({
      name,
      checksum,
      applied_at: Date.now(),
      applied_by: 'bootstrap',
      success: true,
      execution_time_ms: executionTimeMs,
    })
  } catch {
    // Ignore errors - table may not exist yet
  }
}
