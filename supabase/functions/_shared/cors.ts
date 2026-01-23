/**
 * CORS headers for Edge Functions
 *
 * Note: 'Access-Control-Allow-Origin: *' is intentionally permissive because:
 * 1. This API is designed for native mobile apps (iOS/Android) which don't enforce CORS
 * 2. Mobile apps using Capacitor send requests with origins like 'capacitor://localhost'
 * 3. All sensitive endpoints require Supabase Auth JWT tokens for authorization
 * 4. Rate limiting should be implemented at the Supabase/infrastructure level
 *
 * Security is enforced via:
 * - JWT token validation on authenticated endpoints
 * - RLS policies in PostgreSQL
 * - Edge Function-level authorization checks
 */
export const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
}

/**
 * Handle CORS preflight request
 */
export function handleCors(req: Request): Response | null {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }
  return null
}

/**
 * Create a JSON response with CORS headers
 */
export function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...corsHeaders,
      'Content-Type': 'application/json',
    },
  })
}

/**
 * Create an error response with CORS headers
 */
export function errorResponse(message: string, status = 400): Response {
  return jsonResponse({ success: false, error: message }, status)
}
