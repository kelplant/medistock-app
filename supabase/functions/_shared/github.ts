import { crypto } from 'https://deno.land/std@0.208.0/crypto/mod.ts'
import { encodeHex } from 'https://deno.land/std@0.208.0/encoding/hex.ts'

const GITHUB_RAW_BASE = 'https://raw.githubusercontent.com'
const MIGRATIONS_PATH = 'app/src/main/assets/migrations'

/**
 * Get GitHub configuration from environment
 */
function getGitHubConfig() {
  return {
    repo: Deno.env.get('GITHUB_REPO') ?? 'medistock/medistock-app',
    branch: Deno.env.get('GITHUB_BRANCH') ?? 'main',
    token: Deno.env.get('GITHUB_TOKEN'), // Optional, for private repos
  }
}

/**
 * Calculate SHA-256 checksum of a string
 * Note: SHA-256 is used instead of MD5 for better cryptographic security
 */
export async function calculateChecksum(content: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(content)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  return encodeHex(new Uint8Array(hashBuffer))
}

/**
 * Fetch a migration file from GitHub
 */
export async function fetchMigrationFromGitHub(migrationName: string): Promise<string | null> {
  const config = getGitHubConfig()
  const url = `${GITHUB_RAW_BASE}/${config.repo}/${config.branch}/${MIGRATIONS_PATH}/${migrationName}.sql`

  const headers: Record<string, string> = {}
  if (config.token) {
    headers['Authorization'] = `token ${config.token}`
  }

  try {
    const response = await fetch(url, { headers })

    if (!response.ok) {
      console.error(`Failed to fetch migration from GitHub: ${response.status} ${response.statusText}`)
      return null
    }

    return await response.text()
  } catch (error) {
    console.error(`Error fetching migration from GitHub: ${error}`)
    return null
  }
}

/**
 * Verify that a migration SQL matches the official version in GitHub
 * Returns true if the checksum matches, false otherwise
 */
export async function verifyMigrationChecksum(
  migrationName: string,
  submittedSql: string,
  submittedChecksum: string
): Promise<{ valid: boolean; error?: string; officialChecksum?: string }> {
  // Fetch official migration from GitHub
  const officialSql = await fetchMigrationFromGitHub(migrationName)

  if (!officialSql) {
    return {
      valid: false,
      error: `Migration ${migrationName} not found in GitHub repository`,
    }
  }

  // Calculate checksums
  const officialChecksum = await calculateChecksum(officialSql)
  const calculatedChecksum = await calculateChecksum(submittedSql)

  // Verify submitted checksum matches calculated
  if (submittedChecksum !== calculatedChecksum) {
    return {
      valid: false,
      error: 'Submitted checksum does not match submitted SQL',
      officialChecksum,
    }
  }

  // Verify against official
  if (calculatedChecksum !== officialChecksum) {
    return {
      valid: false,
      error: 'Migration SQL does not match official version in repository',
      officialChecksum,
    }
  }

  return { valid: true, officialChecksum }
}

/**
 * List all migrations available in GitHub (for bootstrap)
 * Note: This requires GitHub API access, not just raw content
 */
export async function listMigrationsFromGitHub(): Promise<string[]> {
  const config = getGitHubConfig()
  const url = `https://api.github.com/repos/${config.repo}/contents/${MIGRATIONS_PATH}?ref=${config.branch}`

  const headers: Record<string, string> = {
    'Accept': 'application/vnd.github.v3+json',
  }
  if (config.token) {
    headers['Authorization'] = `token ${config.token}`
  }

  try {
    const response = await fetch(url, { headers })

    if (!response.ok) {
      console.error(`Failed to list migrations from GitHub: ${response.status}`)
      return []
    }

    const files = await response.json()
    return files
      .filter((f: { name: string }) => f.name.endsWith('.sql'))
      .map((f: { name: string }) => f.name.replace('.sql', ''))
      .sort()
  } catch (error) {
    console.error(`Error listing migrations from GitHub: ${error}`)
    return []
  }
}
