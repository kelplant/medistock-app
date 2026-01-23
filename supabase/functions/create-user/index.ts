import { handleCors, jsonResponse, errorResponse } from '../_shared/cors.ts'
import { createAdminClient, requireAdmin } from '../_shared/supabase.ts'
import { hashPassword, uuidToAuthEmail, validatePassword, validateUsername } from '../_shared/auth.ts'

interface CreateUserRequest {
  username: string
  password: string
  name: string
  isAdmin?: boolean
  siteIds?: string[]
}

Deno.serve(async (req) => {
  // Handle CORS
  const corsResponse = handleCors(req)
  if (corsResponse) return corsResponse

  try {
    // Require admin authentication
    const adminUser = await requireAdmin(req)
    console.log(`Create user requested by admin: ${adminUser.id}`)

    // Parse request
    const body: CreateUserRequest = await req.json()
    const { username, password, name, isAdmin = false, siteIds = [] } = body

    // Validate inputs
    const usernameError = validateUsername(username)
    if (usernameError) {
      return errorResponse(usernameError, 400)
    }

    const passwordError = validatePassword(password)
    if (passwordError) {
      return errorResponse(passwordError, 400)
    }

    if (!name || name.trim().length === 0) {
      return errorResponse('Name is required', 400)
    }

    const adminClient = createAdminClient()

    // Check if username already exists
    const { data: existingUser } = await adminClient
      .from('users')
      .select('id')
      .eq('username', username)
      .single()

    if (existingUser) {
      return errorResponse('Username already exists', 409)
    }

    // Create user in Supabase Auth
    console.log('Creating user in Supabase Auth...')
    const { data: authData, error: authError } = await adminClient.auth.admin.createUser({
      email: uuidToAuthEmail(crypto.randomUUID()), // Temporary, will be updated
      password,
      email_confirm: true,
      user_metadata: {
        username,
        name,
      },
    })

    if (authError) {
      return errorResponse(`Failed to create auth user: ${authError.message}`, 500)
    }

    const userId = authData.user.id
    const authEmail = uuidToAuthEmail(userId)

    // Update the email to use the actual user ID
    await adminClient.auth.admin.updateUserById(userId, {
      email: authEmail,
    })

    // Create user in users table
    console.log('Creating user in users table...')
    const passwordHash = await hashPassword(password)
    const now = Date.now()

    const { error: insertError } = await adminClient.from('users').insert({
      id: userId,
      username,
      password_hash: passwordHash,
      name: name.trim(),
      is_admin: isAdmin ? 1 : 0,
      is_active: 1,
      auth_migrated: 1,
      created_at: now,
      updated_at: now,
      created_by: adminUser.id,
    })

    if (insertError) {
      // Rollback: delete auth user
      await adminClient.auth.admin.deleteUser(userId)
      return errorResponse(`Failed to create user record: ${insertError.message}`, 500)
    }

    // Assign sites if provided
    if (siteIds.length > 0) {
      const siteAssignments = siteIds.map(siteId => ({
        user_id: userId,
        site_id: siteId,
        created_at: now,
      }))

      const { error: siteError } = await adminClient
        .from('user_sites')
        .insert(siteAssignments)

      if (siteError) {
        console.error('Failed to assign sites:', siteError)
        // Don't fail the whole operation, just log it
      }
    }

    return jsonResponse({
      success: true,
      message: 'User created successfully',
      user: {
        id: userId,
        username,
        name: name.trim(),
        isAdmin,
        siteIds,
      },
    })

  } catch (error) {
    console.error('Create user error:', error)
    if (error.message === 'Authentication required') {
      return errorResponse('Authentication required', 401)
    }
    if (error.message === 'Admin access required') {
      return errorResponse('Admin access required', 403)
    }
    return errorResponse(error.message ?? 'Unknown error', 500)
  }
})
