import { compareSync, hashSync, genSaltSync } from 'https://deno.land/x/bcrypt@v0.4.1/mod.ts'

/**
 * Verify a password against a BCrypt hash
 * Uses synchronous version to avoid Worker issues in Edge Runtime
 */
export async function verifyPassword(password: string, hash: string): Promise<boolean> {
  try {
    // Use sync version - async version requires Workers which aren't available in Edge Runtime
    return compareSync(password, hash)
  } catch (error) {
    console.error('Error verifying password:', error)
    return false
  }
}

/**
 * Hash a password using BCrypt
 * Uses synchronous version to avoid Worker issues in Edge Runtime
 */
export async function hashPassword(password: string): Promise<string> {
  const salt = genSaltSync(10)
  return hashSync(password, salt)
}

/**
 * Generate a UUID v4
 */
export function generateUUID(): string {
  return crypto.randomUUID()
}

/**
 * Convert a username to the Supabase Auth email format
 * Uses UUID-based email: {uuid}@medistock.local
 */
export function uuidToAuthEmail(uuid: string): string {
  return `${uuid}@medistock.local`
}

/**
 * Validate password strength
 * Returns null if valid, error message if invalid
 */
export function validatePassword(password: string): string | null {
  if (password.length < 8) {
    return 'Password must be at least 8 characters long'
  }
  if (!/[A-Z]/.test(password)) {
    return 'Password must contain at least one uppercase letter'
  }
  if (!/[a-z]/.test(password)) {
    return 'Password must contain at least one lowercase letter'
  }
  if (!/[0-9]/.test(password)) {
    return 'Password must contain at least one digit'
  }
  return null
}

/**
 * Validate username format
 * Returns null if valid, error message if invalid
 */
export function validateUsername(username: string): string | null {
  if (username.length < 3) {
    return 'Username must be at least 3 characters long'
  }
  if (username.length > 50) {
    return 'Username must be at most 50 characters long'
  }
  if (!/^[a-zA-Z0-9._-]+$/.test(username)) {
    return 'Username can only contain letters, numbers, dots, underscores and hyphens'
  }
  return null
}
