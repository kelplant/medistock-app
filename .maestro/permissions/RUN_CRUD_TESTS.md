# Quick Start - CRUD Permission Tests

## Test Summary

10 new CRUD permission tests have been created (5 Android + 5 iOS) to validate granular Create, Read, Update, Delete permissions on the Products module.

## Test Users

All passwords: `Test123!`

| Username | Permissions | Expected UI |
|----------|-------------|-------------|
| `test_products_view` | View only | No Add/Edit/Delete buttons |
| `test_products_create` | View + Create | Add button visible, no Edit/Delete |
| `test_products_edit` | View + Edit | Edit button visible, no Add/Delete |
| `test_products_delete` | View + Delete | Delete button visible, no Add/Edit |
| `test_products_only` | Full CRUD | All buttons visible |

## Quick Run Commands

### Android (5 tests)

```bash
# Prerequisites
emulator -avd Pixel_6_API_34
./gradlew :app:installDebug

# Run all CRUD tests
maestro test .maestro/permissions/android/crud/

# Run specific test
maestro test .maestro/permissions/android/crud/01_products_view_only.yaml
maestro test .maestro/permissions/android/crud/02_products_create_only.yaml
maestro test .maestro/permissions/android/crud/03_products_edit_only.yaml
maestro test .maestro/permissions/android/crud/04_products_delete_only.yaml
maestro test .maestro/permissions/android/crud/05_products_full_crud.yaml
```

### iOS (5 tests)

```bash
# Prerequisites
open -a Simulator
cd iosApp && xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build

# Run all CRUD tests
maestro -p ios test .maestro/permissions/ios/crud/

# Run specific test
maestro -p ios test .maestro/permissions/ios/crud/01_products_view_only.yaml
maestro -p ios test .maestro/permissions/ios/crud/02_products_create_only.yaml
maestro -p ios test .maestro/permissions/ios/crud/03_products_edit_only.yaml
maestro -p ios test .maestro/permissions/ios/crud/04_products_delete_only.yaml
maestro -p ios test .maestro/permissions/ios/crud/05_products_full_crud.yaml
```

### Both Platforms (10 tests)

```bash
# Run all CRUD permission tests
maestro test .maestro/permissions/android/crud/
maestro -p ios test .maestro/permissions/ios/crud/
```

## What Gets Tested

Each test follows this flow:

1. Login with specific CRUD test user
2. Navigate to Products list (Administration > Manage Products > Products)
3. Verify Add/FAB button visibility based on `canCreate` permission
4. Open product detail view
5. Verify Edit button visibility based on `canEdit` permission
6. Verify Delete button visibility based on `canDelete` permission
7. Take screenshot as evidence

## Expected Results

| Test | List Screen | Detail Screen |
|------|-------------|---------------|
| 01_view_only | No Add button | No Edit/Delete buttons |
| 02_create_only | Add button visible | No Edit/Delete buttons |
| 03_edit_only | No Add button | Edit button visible, no Delete |
| 04_delete_only | No Add button | Delete button visible, no Edit |
| 05_full_crud | Add button visible | Edit and Delete buttons visible |

## Test File Locations

**Android:**
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/android/crud/01_products_view_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/android/crud/02_products_create_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/android/crud/03_products_edit_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/android/crud/04_products_delete_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/android/crud/05_products_full_crud.yaml`

**iOS:**
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/ios/crud/01_products_view_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/ios/crud/02_products_create_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/ios/crud/03_products_edit_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/ios/crud/04_products_delete_only.yaml`
- `/Users/xarroues/Projects/medistock-app/.maestro/permissions/ios/crud/05_products_full_crud.yaml`

## Documentation Updated

The following documentation files have been updated to include these CRUD permission tests:

1. `/Users/xarroues/Projects/medistock-app/.maestro/permissions/README.md`
   - Added CRUD test section
   - Updated test structure diagram
   - Added CRUD test users table
   - Updated running commands to include CRUD tests

2. `/Users/xarroues/Projects/medistock-app/documentation/TESTING.md`
   - Updated total test count (52 → 62 tests)
   - Added CRUD permission tests section
   - Added CRUD test user table
   - Added CRUD test execution commands

3. `/Users/xarroues/Projects/medistock-app/.maestro/permissions/crud/README.md` (NEW)
   - Detailed CRUD test documentation
   - Test flow patterns
   - UI elements validated
   - Troubleshooting guide

## Troubleshooting

**Product list is empty:**
The tests assume at least one product exists. Debug builds seed sample data automatically.

**Buttons not detected:**
- Android: Verify FAB has id matching `.*fab.*`
- iOS: Verify toolbar "+" button is visible
- Detail screen: Check "Edit" and "Delete" text buttons

**Test user not found:**
Ensure you're running a debug build. CRUD test users are seeded in debug mode only.

## Next Steps

Consider extending CRUD permission tests to other modules:
- Sites (test_sites_view, test_sites_create, etc.)
- Categories (test_categories_view, test_categories_create, etc.)
- Customers (test_customers_view, test_customers_create, etc.)
- Packaging Types (test_packaging_view, test_packaging_create, etc.)
