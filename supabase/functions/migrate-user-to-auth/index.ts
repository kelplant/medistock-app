import { handleCors, jsonResponse, errorResponse } from '../_shared/cors.ts'
import { createAdminClient } from '../_shared/supabase.ts'
import { verifyPassword, uuidToAuthEmail } from '../_shared/auth.ts'

interface MigrateUserRequest {
  username: string
  password: string
}

Deno.serve(async (req) => {
  // Handle CORS
  const corsResponse = handleCors(req)
  if (corsResponse) return corsResponse

  try {
    // Parse request (no auth required - this IS the login)
    const body: MigrateUserRequest = await req.json()
    const { username, password } = body

    if (!username || !password) {
      return errorResponse('Username and password are required', 400)
    }

    const adminClient = createAdminClient()

    // 1. Find user in local users table
    const { data: user, error: userError } = await adminClient
      .from('app_users')
      .select('id, username, password, full_name, is_admin, is_active, auth_migrated')
      .eq('username', username)
      .single()

    if (userError || !user) {
      // Don't reveal if user exists or not
      console.log('>>> USER LOOKUP FAILED for username:', username)
      console.log('>>> Error:', userError?.message ?? 'User not found in app_users table')
      return errorResponse('Invalid credentials', 401)
    }

    console.log('>>> User found:', user.id, user.username)
    console.log('>>> is_active:', user.is_active, 'auth_migrated:', user.auth_migrated)
    console.log('>>> password hash (first 20 chars):', user.password?.substring(0, 20))

    // 2. Check if user is active
    if (!user.is_active) {
      console.log('>>> USER INACTIVE')
      return errorResponse('Account is deactivated', 403)
    }

    // 3. Verify password against BCrypt hash
    console.log('>>> Verifying password...')
    const passwordValid = await verifyPassword(password, user.password)
    console.log('>>> Password valid:', passwordValid)
    if (!passwordValid) {
      console.log('>>> PASSWORD VERIFICATION FAILED')
      return errorResponse('Invalid credentials', 401)
    }

    console.log(`User ${username} authenticated via BCrypt`)

    // 4. Check if already migrated to Supabase Auth
    const authEmail = uuidToAuthEmail(user.id)

    if (user.auth_migrated === 1 || user.auth_migrated === true) {
      // Already migrated, just sign in
      console.log(`User ${username} already migrated, signing in...`)

      const { data: sessionData, error: signInError } = await adminClient.auth.signInWithPassword({
        email: authEmail,
        password,
      })

      if (signInError) {
        // Auth might be out of sync, try to fix it
        console.log('Sign in failed, attempting to sync auth...')
        await syncUserToAuth(adminClient, user, password)

        // Retry sign in
        const { data: retryData, error: retryError } = await adminClient.auth.signInWithPassword({
          email: authEmail,
          password,
        })

        if (retryError) {
          return errorResponse('Authentication failed. Please contact support.', 500)
        }

        return createSuccessResponse(user, retryData)
      }

      return createSuccessResponse(user, sessionData)
    }

    // 5. Migrate user to Supabase Auth
    console.log(`Migrating user ${username} to Supabase Auth...`)

    // Check if auth user already exists (edge case)
    const { data: existingAuth } = await adminClient.auth.admin.getUserById(user.id)

    if (existingAuth?.user) {
      // Update password to match
      await adminClient.auth.admin.updateUserById(user.id, {
        password,
        email: authEmail,
      })
    } else {
      // Create new auth user with same ID
      const { error: createError } = await adminClient.auth.admin.createUser({
        id: user.id,
        email: authEmail,
        password,
        email_confirm: true,
        user_metadata: {
          username: user.username,
          name: user.full_name,
        },
      })

      if (createError) {
        console.error('Failed to create auth user:', createError)
        return errorResponse(`Migration failed: ${createError.message}`, 500)
      }
    }

    // 6. Mark as migrated
    await adminClient
      .from('app_users')
      .update({
        auth_migrated: 1,
        updated_at: Date.now(),
      })
      .eq('id', user.id)

    // 7. Sign in and return session
    const { data: sessionData, error: signInError } = await adminClient.auth.signInWithPassword({
      email: authEmail,
      password,
    })

    if (signInError) {
      return errorResponse(`Sign in failed after migration: ${signInError.message}`, 500)
    }

    console.log(`User ${username} migrated successfully`)

    return createSuccessResponse(user, sessionData)

  } catch (error) {
    console.error('Migrate user error:', error)
    return errorResponse(error.message ?? 'Unknown error', 500)
  }
})

/**
 * Sync a user to Supabase Auth (fix desync issues)
 */
async function syncUserToAuth(
  adminClient: ReturnType<typeof createAdminClient>,
  user: { id: string; username: string; full_name: string },
  password: string
): Promise<void> {
  const authEmail = uuidToAuthEmail(user.id)

  try {
    // Try to update existing user
    const { error: updateError } = await adminClient.auth.admin.updateUserById(user.id, {
      email: authEmail,
      password,
    })

    if (updateError) {
      // User doesn't exist in auth, create them
      await adminClient.auth.admin.createUser({
        id: user.id,
        email: authEmail,
        password,
        email_confirm: true,
        user_metadata: {
          username: user.username,
          name: user.full_name,
        },
      })
    }
  } catch (error) {
    console.error('Failed to sync user to auth:', error)
    throw error
  }
}

/**
 * Create success response with user and session data
 */
function createSuccessResponse(
  user: { id: string; username: string; full_name: string; is_admin: boolean },
  sessionData: { session: { access_token: string; refresh_token: string; expires_at?: number } | null }
): Response {
  return jsonResponse({
    success: true,
    message: 'Authentication successful',
    user: {
      id: user.id,
      username: user.username,
      name: user.full_name,
      isAdmin: user.is_admin === true,
    },
    session: sessionData.session ? {
      accessToken: sessionData.session.access_token,
      refreshToken: sessionData.session.refresh_token,
      expiresAt: sessionData.session.expires_at,
    } : null,
  })
}
