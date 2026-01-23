# CRUD Permission Tests - Products Module

This directory contains E2E tests for validating granular CRUD (Create, Read, Update, Delete) permissions within the Products module of MediStock.

## Overview

Unlike visibility tests that verify whether users can see entire modules, CRUD permission tests validate whether users can perform specific actions (add, edit, delete) within a module they have access to.

## Test Users

All test users have the password: `Test123!`

| Username | canView | canCreate | canEdit | canDelete | Description |
|----------|---------|-----------|---------|-----------|-------------|
| `test_products_view` | ✓ | ✗ | ✗ | ✗ | Read-only access to products |
| `test_products_create` | ✓ | ✓ | ✗ | ✗ | Can view and create products |
| `test_products_edit` | ✓ | ✗ | ✓ | ✗ | Can view and edit products |
| `test_products_delete` | ✓ | ✗ | ✗ | ✓ | Can view and delete products |
| `test_products_only` | ✓ | ✓ | ✓ | ✓ | Full CRUD access |

## Test Files

### Android Tests (`.maestro/permissions/android/crud/`)

1. **01_products_view_only.yaml** - View permission only
   - Verifies product list is visible
   - Verifies FAB (Add button) is NOT visible
   - Verifies Edit button is NOT visible in detail view
   - Verifies Delete button is NOT visible in detail view

2. **02_products_create_only.yaml** - View + Create permissions
   - Verifies product list is visible
   - Verifies FAB (Add button) IS visible
   - Verifies Edit button is NOT visible in detail view
   - Verifies Delete button is NOT visible in detail view

3. **03_products_edit_only.yaml** - View + Edit permissions
   - Verifies product list is visible
   - Verifies FAB (Add button) is NOT visible
   - Verifies Edit button IS visible in detail view
   - Verifies Delete button is NOT visible in detail view

4. **04_products_delete_only.yaml** - View + Delete permissions
   - Verifies product list is visible
   - Verifies FAB (Add button) is NOT visible
   - Verifies Edit button is NOT visible in detail view
   - Verifies Delete button IS visible in detail view

5. **05_products_full_crud.yaml** - Full CRUD permissions
   - Verifies product list is visible
   - Verifies FAB (Add button) IS visible
   - Verifies Edit button IS visible in detail view
   - Verifies Delete button IS visible in detail view

### iOS Tests (`.maestro/permissions/ios/crud/`)

Same test coverage as Android but adapted for iOS UI patterns:
- Uses toolbar "+" button instead of FAB
- Uses iOS-specific navigation patterns
- Uses iOS text selectors and identifiers

## Running the Tests

### Prerequisites

**Android:**
```bash
# Start Android emulator
emulator -avd Pixel_6_API_34

# Install debug build
./gradlew :app:installDebug
```

**iOS:**
```bash
# Start iOS simulator
open -a Simulator

# Build and install app
cd iosApp
xcodebuild -workspace iosApp.xcworkspace \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  build
```

### Execute Tests

```bash
# Android - All CRUD permission tests (5 tests)
maestro test .maestro/permissions/android/crud/

# Android - Individual test
maestro test .maestro/permissions/android/crud/01_products_view_only.yaml

# iOS - All CRUD permission tests (5 tests)
maestro -p ios test .maestro/permissions/ios/crud/

# iOS - Individual test
maestro -p ios test .maestro/permissions/ios/crud/03_products_edit_only.yaml

# Both platforms (10 tests total)
maestro test .maestro/permissions/android/crud/
maestro -p ios test .maestro/permissions/ios/crud/
```

## Test Flow Pattern

Each test follows this pattern:

```yaml
# 1. Launch and Login
- launchApp:
    clearState: true
- tapOn: Username
- inputText: "test_products_<permission>"
- tapOn: Password
- inputText: "Test123!"
- tapOn: Login

# 2. Navigate to Products
- tapOn: "Administration"
- tapOn: "Manage Products"
- tapOn: "Products"

# 3. Verify List UI (Add button)
- assertVisible/assertNotVisible: FAB or "+"

# 4. Open Product Detail
- tapOn:
    index: 1  # First product in list

# 5. Verify Detail UI (Edit/Delete buttons)
- assertVisible/assertNotVisible: "Edit"
- assertVisible/assertNotVisible: "Delete"

# 6. Screenshot
- takeScreenshot: "platform_permission_detail"
```

## UI Elements Validated

### Android
- **List Screen:**
  - FAB (Floating Action Button) with id matching `.*fab.*`
  - RecyclerView/ListView for product list

- **Detail Screen:**
  - Edit menu item or button (text: "Edit" or id: `.*menu_edit.*`)
  - Delete menu item or button (text: "Delete" or id: `.*menu_delete.*`)

### iOS
- **List Screen:**
  - Toolbar "+" button for adding products
  - SwiftUI List for product display

- **Detail Screen:**
  - Edit button in toolbar or as text
  - Delete button in toolbar or as text

## Expected Results

| Test | Add Button | Edit Button | Delete Button | Screenshot |
|------|------------|-------------|---------------|------------|
| view_only | ✗ | ✗ | ✗ | Shows read-only view |
| create_only | ✓ | ✗ | ✗ | Shows Add button only |
| edit_only | ✗ | ✓ | ✗ | Shows Edit button only |
| delete_only | ✗ | ✗ | ✓ | Shows Delete button only |
| full_crud | ✓ | ✓ | ✓ | Shows all action buttons |

## Troubleshooting

### Test user not found
Ensure you're running a debug build with seeded test users. CRUD permission users are created by the TestDataSeeder in debug mode.

### Product list is empty
The tests assume at least one product exists in the database. The app seeds sample data in debug builds.

### Buttons not detected
- For Android: Check that resource IDs match the patterns in tests (`.*fab.*`, `.*menu_edit.*`)
- For iOS: Verify button text is in English ("Edit", "Delete", "+")

### Navigation fails
Ensure you're starting with a clean app state (`clearState: true`). The tests navigate from the login screen through the full menu hierarchy.

## Future Enhancements

1. **Extend to Other Modules**: Create CRUD permission tests for Sites, Categories, Customers, etc.
2. **Action Execution**: Test that users can actually execute the permitted actions (not just see buttons)
3. **Negative Testing**: Attempt to perform unauthorized actions and verify proper error handling
4. **Combined Permissions**: Test users with multiple CRUD permissions (e.g., canCreate + canEdit)

## Related Documentation

- Main permission tests: `../.maestro/permissions/README.md`
- Testing guide: `/documentation/TESTING.md`
- Permission system: `/shared/src/commonMain/kotlin/com/medistock/shared/models/Permission.kt`
