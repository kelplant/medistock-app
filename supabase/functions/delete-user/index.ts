import { handleCors, jsonResponse, errorResponse } from '../_shared/cors.ts'
import { createAdminClient, requireAdmin } from '../_shared/supabase.ts'

interface DeleteUserRequest {
  userId: string
  hardDelete?: boolean // If true, permanently delete. If false, just deactivate.
}

Deno.serve(async (req) => {
  // Handle CORS
  const corsResponse = handleCors(req)
  if (corsResponse) return corsResponse

  try {
    // Require admin authentication
    const adminUser = await requireAdmin(req)
    console.log(`Delete user requested by admin: ${adminUser.id}`)

    // Parse request
    const body: DeleteUserRequest = await req.json()
    const { userId, hardDelete = false } = body

    if (!userId) {
      return errorResponse('User ID is required', 400)
    }

    // Prevent self-deletion
    if (userId === adminUser.id) {
      return errorResponse('Cannot delete your own account', 400)
    }

    const adminClient = createAdminClient()

    // Check if user exists
    const { data: existingUser, error: fetchError } = await adminClient
      .from('users')
      .select('id, username, is_admin')
      .eq('id', userId)
      .single()

    if (fetchError || !existingUser) {
      return errorResponse('User not found', 404)
    }

    if (hardDelete) {
      // Hard delete: remove from both Supabase Auth and users table
      console.log(`Hard deleting user: ${existingUser.username}`)

      // Delete from Supabase Auth
      const { error: authError } = await adminClient.auth.admin.deleteUser(userId)
      if (authError) {
        console.error('Failed to delete from Supabase Auth:', authError)
        // Continue anyway - user might not be in Auth
      }

      // Delete site assignments
      await adminClient
        .from('user_sites')
        .delete()
        .eq('user_id', userId)

      // Delete from users table
      const { error: deleteError } = await adminClient
        .from('users')
        .delete()
        .eq('id', userId)

      if (deleteError) {
        return errorResponse(`Failed to delete user: ${deleteError.message}`, 500)
      }

      return jsonResponse({
        success: true,
        message: 'User permanently deleted',
        userId,
        username: existingUser.username,
      })

    } else {
      // Soft delete: just deactivate
      console.log(`Deactivating user: ${existingUser.username}`)

      const { error: updateError } = await adminClient
        .from('users')
        .update({
          is_active: 0,
          updated_at: Date.now(),
          updated_by: adminUser.id,
        })
        .eq('id', userId)

      if (updateError) {
        return errorResponse(`Failed to deactivate user: ${updateError.message}`, 500)
      }

      return jsonResponse({
        success: true,
        message: 'User deactivated',
        userId,
        username: existingUser.username,
      })
    }

  } catch (error) {
    console.error('Delete user error:', error)
    if (error.message === 'Authentication required') {
      return errorResponse('Authentication required', 401)
    }
    if (error.message === 'Admin access required') {
      return errorResponse('Admin access required', 403)
    }
    return errorResponse(error.message ?? 'Unknown error', 500)
  }
})
