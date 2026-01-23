import { createClient, SupabaseClient } from 'https://esm.sh/@supabase/supabase-js@2'

/**
 * Create a Supabase client with the user's JWT token
 * Used for operations that should respect RLS policies
 */
export function createUserClient(authHeader: string | null): SupabaseClient {
  const supabaseUrl = Deno.env.get('SUPABASE_URL')!
  const supabaseAnonKey = Deno.env.get('SUPABASE_ANON_KEY')!

  return createClient(supabaseUrl, supabaseAnonKey, {
    global: {
      headers: authHeader ? { Authorization: authHeader } : {},
    },
  })
}

/**
 * Create a Supabase admin client with service_role key
 * Bypasses RLS - use with caution!
 */
export function createAdminClient(): SupabaseClient {
  const supabaseUrl = Deno.env.get('SUPABASE_URL')!
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

  return createClient(supabaseUrl, serviceRoleKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  })
}

/**
 * Get the authenticated user from the request
 * Returns null if not authenticated
 */
export async function getAuthenticatedUser(req: Request): Promise<{ id: string; email: string } | null> {
  const authHeader = req.headers.get('Authorization')
  if (!authHeader) {
    return null
  }

  const client = createUserClient(authHeader)
  const { data: { user }, error } = await client.auth.getUser()

  if (error || !user) {
    return null
  }

  return { id: user.id, email: user.email ?? '' }
}

/**
 * Check if the authenticated user is an admin
 */
export async function isUserAdmin(userId: string): Promise<boolean> {
  const adminClient = createAdminClient()

  const { data, error } = await adminClient
    .from('users')
    .select('is_admin')
    .eq('id', userId)
    .single()

  if (error || !data) {
    return false
  }

  return data.is_admin === 1
}

/**
 * Require authentication - throws if not authenticated
 */
export async function requireAuth(req: Request): Promise<{ id: string; email: string }> {
  const user = await getAuthenticatedUser(req)
  if (!user) {
    throw new Error('Authentication required')
  }
  return user
}

/**
 * Require admin authentication - throws if not admin
 */
export async function requireAdmin(req: Request): Promise<{ id: string; email: string }> {
  const user = await requireAuth(req)
  const isAdmin = await isUserAdmin(user.id)
  if (!isAdmin) {
    throw new Error('Admin access required')
  }
  return user
}
