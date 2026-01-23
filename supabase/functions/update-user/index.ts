import { handleCors, jsonResponse, errorResponse } from '../_shared/cors.ts'
import { createAdminClient, requireAdmin, requireAuth } from '../_shared/supabase.ts'
import { hashPassword, validatePassword, validateUsername } from '../_shared/auth.ts'

interface UpdateUserRequest {
  userId: string
  username?: string
  password?: string
  name?: string
  isAdmin?: boolean
  isActive?: boolean
  siteIds?: string[]
}

Deno.serve(async (req) => {
  // Handle CORS
  const corsResponse = handleCors(req)
  if (corsResponse) return corsResponse

  try {
    // Parse request first to get userId
    const body: UpdateUserRequest = await req.json()
    const { userId, username, password, name, isAdmin, isActive, siteIds } = body

    if (!userId) {
      return errorResponse('User ID is required', 400)
    }

    // Check authentication
    const authUser = await requireAuth(req)

    // Check permissions: admin can update anyone, user can only update themselves (limited)
    const adminClient = createAdminClient()
    const { data: callerData } = await adminClient
      .from('users')
      .select('is_admin')
      .eq('id', authUser.id)
      .single()

    const isCallerAdmin = callerData?.is_admin === 1
    const isSelfUpdate = authUser.id === userId

    // Non-admin can only update their own password
    if (!isCallerAdmin && !isSelfUpdate) {
      return errorResponse('Admin access required to update other users', 403)
    }

    if (!isCallerAdmin && isSelfUpdate) {
      // Self-update: only password change allowed
      if (username || name || isAdmin !== undefined || isActive !== undefined || siteIds) {
        return errorResponse('Non-admin users can only change their own password', 403)
      }
    }

    // Validate inputs if provided
    if (username) {
      const usernameError = validateUsername(username)
      if (usernameError) {
        return errorResponse(usernameError, 400)
      }

      // Check if username is taken by another user
      const { data: existingUser } = await adminClient
        .from('users')
        .select('id')
        .eq('username', username)
        .neq('id', userId)
        .single()

      if (existingUser) {
        return errorResponse('Username already taken', 409)
      }
    }

    if (password) {
      const passwordError = validatePassword(password)
      if (passwordError) {
        return errorResponse(passwordError, 400)
      }
    }

    // Update Supabase Auth if password changed
    if (password) {
      console.log('Updating password in Supabase Auth...')
      const { error: authError } = await adminClient.auth.admin.updateUserById(userId, {
        password,
      })

      if (authError) {
        return errorResponse(`Failed to update auth password: ${authError.message}`, 500)
      }
    }

    // Build update object for users table
    const updateData: Record<string, unknown> = {
      updated_at: Date.now(),
      updated_by: authUser.id,
    }

    if (username) {
      updateData.username = username
    }

    if (password) {
      updateData.password_hash = await hashPassword(password)
    }

    if (name !== undefined) {
      updateData.name = name.trim()
    }

    if (isAdmin !== undefined && isCallerAdmin) {
      updateData.is_admin = isAdmin ? 1 : 0
    }

    if (isActive !== undefined && isCallerAdmin) {
      updateData.is_active = isActive ? 1 : 0
    }

    // Update users table
    console.log('Updating user in database...')
    const { error: updateError } = await adminClient
      .from('users')
      .update(updateData)
      .eq('id', userId)

    if (updateError) {
      return errorResponse(`Failed to update user: ${updateError.message}`, 500)
    }

    // Update site assignments if provided (admin only)
    if (siteIds !== undefined && isCallerAdmin) {
      // Remove existing assignments
      await adminClient
        .from('user_sites')
        .delete()
        .eq('user_id', userId)

      // Add new assignments
      if (siteIds.length > 0) {
        const siteAssignments = siteIds.map(siteId => ({
          user_id: userId,
          site_id: siteId,
          created_at: Date.now(),
        }))

        const { error: siteError } = await adminClient
          .from('user_sites')
          .insert(siteAssignments)

        if (siteError) {
          console.error('Failed to update site assignments:', siteError)
        }
      }
    }

    // Fetch updated user
    const { data: updatedUser } = await adminClient
      .from('users')
      .select('id, username, name, is_admin, is_active')
      .eq('id', userId)
      .single()

    return jsonResponse({
      success: true,
      message: 'User updated successfully',
      user: updatedUser ? {
        id: updatedUser.id,
        username: updatedUser.username,
        name: updatedUser.name,
        isAdmin: updatedUser.is_admin === 1,
        isActive: updatedUser.is_active === 1,
      } : null,
    })

  } catch (error) {
    console.error('Update user error:', error)
    if (error.message === 'Authentication required') {
      return errorResponse('Authentication required', 401)
    }
    return errorResponse(error.message ?? 'Unknown error', 500)
  }
})
