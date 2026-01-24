# Online-First Authentication Testing Guide

This guide explains how to test the online-first authentication flow in MediStock using Maestro E2E tests.

## Overview

The online-first authentication flow ensures that:
1. **First-time login** requires an internet connection to authenticate with Supabase and sync all data
2. **Subsequent logins** can work offline using local BCrypt password verification
3. All authentication errors are properly handled and displayed to users

## Test Files

- **Android**: `/Users/xarroues/Projects/medistock-app/.maestro/android/15_online_first_auth.yaml`
- **iOS**: `/Users/xarroues/Projects/medistock-app/.maestro/ios/15_online_first_auth.yaml`
- **Shared flows**:
  - `/Users/xarroues/Projects/medistock-app/.maestro/shared/logout.yaml`
  - `/Users/xarroues/Projects/medistock-app/.maestro/shared/clear_app_data.yaml`

## Test Coverage

### 1. First-time Login (Online Required)

#### Test 1.1: First login without network
**Objective**: Verify that first login requires internet connection

**Manual steps** (Maestro doesn't support network control):
```bash
# 1. Clear app data
maestro test .maestro/shared/clear_app_data.yaml

# 2. Enable airplane mode on device/simulator

# 3. Try to login
# Expected: "Première connexion : une connexion internet est requise" (Android)
# Expected: "First login requires an internet connection" (iOS)
```

**Expected behavior**:
- Login button is enabled
- Error message displays: "First login requires an internet connection"
- User remains on login screen

#### Test 1.2: First login with valid credentials (online)
**Objective**: Verify successful first-time authentication

**Automated test**: Included in `15_online_first_auth.yaml`

**Flow**:
1. Clear app state (`clearState: true`)
2. Enter valid credentials (admin/admin)
3. Tap login button
4. Wait for authentication + full sync (15 seconds timeout)
5. Verify navigation to home screen

**Expected behavior**:
- Authentication succeeds via Supabase Edge Function
- User data is synced to local database
- Session tokens are stored in keychain/SharedPreferences
- Full sync is triggered to download all data
- User navigates to home screen
- Screenshot: `android_online_first_02_first_login_success.png`

#### Test 1.3: First login with invalid credentials (online)
**Objective**: Verify error handling for invalid credentials

**Note**: This scenario is tested as part of the user not found test

**Expected behavior**:
- Error message displays: "Nom d'utilisateur ou mot de passe incorrect" (Android)
- Error message displays: "Invalid password" or "User not found" (iOS)
- User remains on login screen

### 2. Subsequent Login (Offline Capable)

#### Test 2.1: Returning user can login offline
**Objective**: Verify offline authentication using local BCrypt

**Manual steps**:
```bash
# 1. Login once with internet (to cache user data)
# 2. Logout
# 3. Enable airplane mode
# 4. Login again with same credentials
# Expected: Success (uses local authentication)
```

**Expected behavior**:
- Authentication succeeds using local BCrypt password verification
- No network request is made
- User navigates to home screen
- Screenshot: `android_online_first_04_subsequent_login_success.png`

#### Test 2.2: Returning user can login online
**Objective**: Verify online authentication with Supabase Auth

**Automated test**: Included in `15_online_first_auth.yaml`

**Flow**:
1. Login with valid credentials (with internet)
2. Logout
3. Login again (with internet)

**Expected behavior**:
- Authentication attempts Supabase Auth first
- If Supabase Auth succeeds, user is logged in
- If Supabase Auth fails, falls back to local authentication
- Screenshot: `android_online_first_04_subsequent_login_success.png`

#### Test 2.3: Invalid password shows error
**Objective**: Verify password validation

**Automated test**: Included in `15_online_first_auth.yaml` (Test 6)

**Flow**:
1. Enter valid username (admin)
2. Enter invalid password (wrongpassword)
3. Tap login button

**Expected behavior**:
- Error message displays: "Nom d'utilisateur ou mot de passe incorrect" (Android)
- Error message displays: "Invalid password" (iOS)
- Screenshot: `android_online_first_05_invalid_password.png`

### 3. Error Scenarios

#### Test 3.1: User not found error
**Objective**: Verify error message for non-existent users

**Automated test**: Included in `15_online_first_auth.yaml` (Test 7)

**Flow**:
1. Enter non-existent username (nonexistentuser)
2. Enter any password
3. Tap login button

**Expected behavior**:
- Error message displays: "Utilisateur non trouvé" (Android)
- Error message displays: "User not found" (iOS)
- Screenshot: `android_online_first_06_user_not_found.png`

#### Test 3.2: Account disabled error
**Objective**: Verify error message for disabled accounts

**Manual test** (requires test data):
```bash
# Prerequisites:
# 1. Create a test user via admin panel
# 2. Disable the user (set is_active = 0)
# 3. Try to login with that user's credentials

# Expected error:
# Android: "Ce compte est désactivé"
# iOS: "This account is disabled. Contact an administrator."
```

**Expected behavior**:
- Error message displays account disabled message
- User remains on login screen

#### Test 3.3: Network error handling
**Objective**: Verify graceful handling of network errors

**Manual test**:
```bash
# 1. Configure invalid Supabase URL in settings
# 2. Try to login
# Expected: Falls back to local authentication or shows network error
```

**Expected behavior**:
- If local user exists: Falls back to local authentication
- If local user doesn't exist: Shows appropriate error message

## Running the Tests

### Run all online-first auth tests

**Android**:
```bash
cd /Users/xarroues/Projects/medistock-app
maestro test .maestro/android/15_online_first_auth.yaml
```

**iOS**:
```bash
cd /Users/xarroues/Projects/medistock-app
maestro test .maestro/ios/15_online_first_auth.yaml
```

### Run both platforms:
```bash
cd /Users/xarroues/Projects/medistock-app
maestro test .maestro/android/15_online_first_auth.yaml
maestro test .maestro/ios/15_online_first_auth.yaml
```

## Manual Testing Checklist

Due to Maestro limitations (no network control), some scenarios require manual testing:

### First-time Login Offline Test
- [ ] Delete and reinstall app (or clear all data)
- [ ] Enable airplane mode
- [ ] Try to login with admin/admin
- [ ] Verify error: "First login requires an internet connection"
- [ ] Disable airplane mode
- [ ] Login successfully
- [ ] Verify full sync completes

### Offline Subsequent Login Test
- [ ] Login once with internet (admin/admin)
- [ ] Logout
- [ ] Enable airplane mode
- [ ] Login again with admin/admin
- [ ] Verify successful login (no network request)
- [ ] Verify app functions correctly offline

### Network Error Recovery Test
- [ ] Login with internet
- [ ] Logout
- [ ] Simulate poor network (e.g., throttle to 2G)
- [ ] Try to login
- [ ] Verify graceful fallback to local auth

### Account Disabled Test
- [ ] Create test user via admin panel (username: test_disabled)
- [ ] Disable the user (is_active = false)
- [ ] Try to login with test_disabled credentials
- [ ] Verify error: "This account is disabled"

## Test Data Requirements

### Default Admin User
- **Username**: admin
- **Password**: admin (hashed with BCrypt)
- **Created by**: LOCAL_SYSTEM_MARKER (auto-created on first launch)

### Test Users (for manual testing)
Create these users via admin panel for comprehensive testing:

1. **test_disabled**
   - Status: Inactive (is_active = false)
   - Purpose: Test disabled account error

2. **test_network_user**
   - Status: Active
   - Not synced to local DB
   - Purpose: Test online-only authentication

## Troubleshooting

### Test fails with "Login button disabled"
- **Cause**: App is still initializing or checking compatibility
- **Solution**: Increase wait timeout or verify app is fully loaded

### Test fails with "Element not found"
- **Cause**: UI elements have changed or animations are still running
- **Solution**: Add `waitForAnimationToEnd` before interactions

### First login doesn't trigger sync
- **Cause**: Supabase not configured or network issue
- **Solution**: Verify Supabase configuration in app settings

### Offline login fails
- **Cause**: User data not in local database
- **Solution**: Ensure user logged in at least once with internet

## Authentication Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    User Login Attempt                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
           ┌─────────────────────┐
           │ Is First-time Login? │
           │ (No local users)     │
           └─────────┬───────────┘
                     │
           ┌─────────┴─────────┐
           │                   │
       YES │                   │ NO
           │                   │
           ▼                   ▼
  ┌────────────────┐   ┌──────────────────┐
  │ Online Required│   │  Standard Flow    │
  │  Check Network │   │  (Multi-step)     │
  └────┬───────────┘   └──────┬───────────┘
       │                      │
       │ Online?              │ Online?
       ▼                      ▼
  ┌────────────┐       ┌──────────────┐
  │ Edge Fn    │       │ Supabase Auth│
  │ Auth       │       │   (try)      │
  └────┬───────┘       └──────┬───────┘
       │                      │
       ▼ Success              ▼ Success/Fail
  ┌────────────┐       ┌──────────────┐
  │ Full Sync  │       │ Fallback to  │
  │            │       │ Local Auth   │
  └────┬───────┘       └──────┬───────┘
       │                      │
       ▼                      ▼
  ┌─────────────────────────────────┐
  │         Login Success           │
  │    (Navigate to Home)           │
  └─────────────────────────────────┘
```

## Expected Error Messages

### Android (French)
- First login requires network: `"Première connexion : une connexion internet est requise"`
- Invalid credentials: `"Nom d'utilisateur ou mot de passe incorrect"`
- User not found: `"Utilisateur non trouvé"`
- Account disabled: `"Ce compte est désactivé"`

### iOS (English)
- First login requires network: `"First login requires an internet connection"`
- Invalid credentials: `"Invalid password"`
- User not found: `"User not found"`
- Account disabled: `"This account is disabled. Contact an administrator."`

## Success Criteria

A successful test run should:
1. ✅ Complete all automated test steps without errors
2. ✅ Generate all expected screenshots
3. ✅ Display correct error messages for each scenario
4. ✅ Successfully authenticate and navigate to home screen
5. ✅ Trigger full sync on first login (visible in logs)
6. ✅ Work offline for subsequent logins (manual verification)

## Notes for Test Maintenance

1. **Network Control**: Maestro doesn't support network toggling. Manual tests are required for offline scenarios.

2. **Test Data**: The default admin user (admin/admin) is auto-created. Additional test users must be created manually.

3. **Screenshots**: All screenshots are saved with prefix `android_online_first_` or `ios_online_first_` for easy identification.

4. **Timing**: First login has extended timeout (15s) to allow for full sync. Adjust if needed based on network speed.

5. **Element IDs**: Android tests use resource IDs (`com.medistock:id/editUsername`). iOS tests use accessibility labels (`"Username"`). Update if UI changes.

6. **Error Messages**: Tests use regex patterns to match error messages in multiple languages. Update patterns if message format changes.
