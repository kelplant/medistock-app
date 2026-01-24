# MediStock Permission Visibility Tests

This directory contains comprehensive E2E tests for validating the granular permission system in MediStock. These tests verify that users can only see and access the modules they have permission for.

## Test Structure

```
permissions/
├── android/
│   ├── 00_setup_test_users.yaml   # REQUIRED: Run before permission tests
│   ├── 99_cleanup_test_users.yaml # REQUIRED: Run after permission tests
│   ├── visibility/          # 13 tests - UI visibility validation
│   │   ├── 01_no_permission.yaml
│   │   ├── 02_sites_only.yaml
│   │   ├── 03_products_only.yaml
│   │   ├── 04_categories_only.yaml
│   │   ├── 05_customers_only.yaml
│   │   ├── 06_packaging_only.yaml
│   │   ├── 07_stock_only.yaml
│   │   ├── 08_purchases_only.yaml
│   │   ├── 09_sales_only.yaml
│   │   ├── 10_transfers_only.yaml
│   │   ├── 11_inventory_only.yaml
│   │   ├── 12_users_only.yaml
│   │   └── 13_audit_only.yaml
│   ├── crud/                # 5 tests - CRUD permission validation
│   │   ├── 01_products_view_only.yaml
│   │   ├── 02_products_create_only.yaml
│   │   ├── 03_products_edit_only.yaml
│   │   ├── 04_products_delete_only.yaml
│   │   └── 05_products_full_crud.yaml
│   └── combination/         # Future: combination permission tests
└── ios/
    ├── 00_setup_test_users.yaml   # REQUIRED: Run before permission tests
    ├── 99_cleanup_test_users.yaml # REQUIRED: Run after permission tests
    ├── visibility/          # 13 tests - UI visibility validation
    │   ├── 01_no_permission.yaml
    │   ├── 02_sites_only.yaml
    │   ├── 03_products_only.yaml
    │   ├── 04_categories_only.yaml
    │   ├── 05_customers_only.yaml
    │   ├── 06_packaging_only.yaml
    │   ├── 07_stock_only.yaml
    │   ├── 08_purchases_only.yaml
    │   ├── 09_sales_only.yaml
    │   ├── 10_transfers_only.yaml
    │   ├── 11_inventory_only.yaml
    │   ├── 12_users_only.yaml
    │   └── 13_audit_only.yaml
    ├── crud/                # 5 tests - CRUD permission validation
    │   ├── 01_products_view_only.yaml
    │   ├── 02_products_create_only.yaml
    │   ├── 03_products_edit_only.yaml
    │   ├── 04_products_delete_only.yaml
    │   └── 05_products_full_crud.yaml
    └── combination/         # Future: combination permission tests
```

## Permission System Overview

MediStock has 12 module-level permissions organized into two categories:

### Operations (Home Screen)
- **STOCK** - View stock levels and quantities
- **PURCHASES** - Create purchase orders and add stock
- **SALES** - Create sales and process FIFO
- **TRANSFERS** - Transfer products between sites
- **INVENTORY** - Perform inventory counts and adjustments

### Administration (Admin Menu)
- **SITES** - Manage site locations
- **PRODUCTS** - Manage product catalog
- **CATEGORIES** - Manage product categories
- **PACKAGING_TYPES** - Manage packaging types
- **CUSTOMERS** - Manage customer database
- **USERS** - Manage user accounts and permissions
- **AUDIT** - View audit history and logs

## Test Users

All test users are automatically seeded in debug builds with the password: `Test123!`

### Visibility Test Users (Module Access)

| Username | Permission | Expected Visibility |
|----------|-----------|---------------------|
| test_no_permission | None | No operations or admin modules visible |
| test_sites_only | SITES | Only "Site Management" in Admin menu |
| test_products_only | PRODUCTS | Only "Manage Products" in Admin menu |
| test_categories_only | CATEGORIES | Only "Manage Products" in Admin menu |
| test_customers_only | CUSTOMERS | Only "Manage Customers" in Admin menu |
| test_packaging_only | PACKAGING_TYPES | Only "Packaging Types" in Admin menu |
| test_stock_only | STOCK | Only "View Stock" on Home screen |
| test_purchases_only | PURCHASES | Only "Purchase Products" on Home screen |
| test_sales_only | SALES | Only "Sell Products" on Home screen |
| test_transfers_only | TRANSFERS | Only "Transfer Products" on Home screen |
| test_inventory_only | INVENTORY | Only "Inventory Stock" on Home screen |
| test_users_only | USERS | Only "User Management" in Admin menu |
| test_audit_only | AUDIT | Only "Audit History" in Admin menu |

### CRUD Permission Test Users (Granular Actions)

| Username | CRUD Permissions | Expected Behavior |
|----------|------------------|-------------------|
| test_products_view | canView only | Can see product list and details, no Add/Edit/Delete buttons |
| test_products_create | canView + canCreate | Can see products and Add button, no Edit/Delete |
| test_products_edit | canView + canEdit | Can see products and Edit button, no Add/Delete |
| test_products_delete | canView + canDelete | Can see products and Delete button, no Add/Edit |
| test_products_only | Full CRUD | Can see all buttons: Add, Edit, Delete |

## Test Strategy

Each test follows this pattern:

1. **Launch and Login**: Clear app state and login with specific test user
2. **Verify Home Screen**: Check which operations buttons are visible/hidden
3. **Navigate to Admin** (if applicable): Check which admin modules are visible/hidden
4. **Screenshot**: Capture evidence of permission enforcement
5. **Validation**: Use assertVisible and assertNotVisible to verify correct UI state

### Example Test Flow (test_sites_only)

```yaml
# Login with test_sites_only user
- tapOn: Username
- inputText: "test_sites_only"
- tapOn: Password
- inputText: "Test123!"
- tapOn: Login

# Verify NO operations visible on Home
- assertNotVisible: "Purchase Products"
- assertNotVisible: "Sell Products"
- assertNotVisible: "Transfer Products"
- assertNotVisible: "View Stock"
- assertNotVisible: "Inventory Stock"

# Verify Administration IS visible
- assertVisible: "Administration"

# Navigate to Admin
- tapOn: "Administration"

# Verify ONLY Sites is visible
- assertVisible: "Site Management"

# Verify other admin modules are NOT visible
- assertNotVisible: "Manage Products"
- assertNotVisible: "Packaging Types"
- assertNotVisible: "Manage Customers"
- assertNotVisible: "User Management"
- assertNotVisible: "Audit History"
```

## Running the Tests

### Setup and Cleanup (REQUIRED)

Permission tests require test users to be seeded before running and cleaned up after.

```bash
# Android: Seed test users BEFORE running permission tests
maestro test .maestro/permissions/android/00_setup_test_users.yaml

# ... run your permission tests ...

# Android: Cleanup test users AFTER running permission tests
maestro test .maestro/permissions/android/99_cleanup_test_users.yaml
```

### Android Permission Tests

```bash
# Complete test run with setup and cleanup
maestro test .maestro/permissions/android/00_setup_test_users.yaml && \
maestro test .maestro/permissions/android/visibility/ && \
maestro test .maestro/permissions/android/crud/ && \
maestro test .maestro/permissions/android/99_cleanup_test_users.yaml

# All Android visibility tests (13 tests)
maestro test .maestro/permissions/android/visibility/

# All Android CRUD permission tests (5 tests)
maestro test .maestro/permissions/android/crud/

# All Android permission tests (18 tests)
maestro test .maestro/permissions/android/

# Individual visibility test
maestro test .maestro/permissions/android/visibility/01_no_permission.yaml
maestro test .maestro/permissions/android/visibility/07_stock_only.yaml

# Individual CRUD permission test
maestro test .maestro/permissions/android/crud/01_products_view_only.yaml
maestro test .maestro/permissions/android/crud/05_products_full_crud.yaml
```

### iOS Permission Tests

```bash
# Complete test run with setup and cleanup
maestro test -p ios .maestro/permissions/ios/00_setup_test_users.yaml && \
maestro test -p ios .maestro/permissions/ios/visibility/ && \
maestro test -p ios .maestro/permissions/ios/crud/ && \
maestro test -p ios .maestro/permissions/ios/99_cleanup_test_users.yaml

# All iOS visibility tests (13 tests)
maestro test -p ios .maestro/permissions/ios/visibility/

# All iOS CRUD permission tests (5 tests)
maestro test -p ios .maestro/permissions/ios/crud/

# All iOS permission tests (18 tests)
maestro test -p ios .maestro/permissions/ios/

# Individual visibility test
maestro test -p ios .maestro/permissions/ios/visibility/01_no_permission.yaml
maestro test -p ios .maestro/permissions/ios/visibility/07_stock_only.yaml

# Individual CRUD permission test
maestro test -p ios .maestro/permissions/ios/crud/01_products_view_only.yaml
maestro test -p ios .maestro/permissions/ios/crud/05_products_full_crud.yaml
```

### Both Platforms

```bash
# Run all permission tests (Android + iOS = 36 tests total)
maestro test .maestro/permissions/android/
maestro -p ios test .maestro/permissions/ios/

# Visibility only (26 tests)
maestro test .maestro/permissions/android/visibility/
maestro -p ios test .maestro/permissions/ios/visibility/

# CRUD permissions only (10 tests)
maestro test .maestro/permissions/android/crud/
maestro -p ios test .maestro/permissions/ios/crud/
```

## Prerequisites

### Android
1. Start an Android emulator
2. Install the debug build:
   ```bash
   ./gradlew :app:installDebug
   ```

### iOS
1. Start an iOS simulator (iPhone 15 or similar)
2. Build and install the app:
   ```bash
   cd iosApp
   xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build
   ```

## Expected Test Results

### Success Criteria

All tests should pass with the following validations:

1. **User with no permissions**: Empty Home screen (no operation buttons visible)
2. **User with single permission**: Only that module's button/menu item visible
3. **Admin permission users**: "Administration" menu visible, specific admin modules accessible
4. **Operation permission users**: Specific operation buttons visible on Home, no admin access

### Failure Indicators

Tests will fail if:
- A user can see modules they don't have permission for
- A user cannot see modules they do have permission for
- Navigation to unauthorized screens is possible
- Permission checks are not enforced at the UI level

## Test Coverage Matrix

| Test File | User | Module | Location | Verifies |
|-----------|------|--------|----------|----------|
| 01_no_permission | test_no_permission | None | Home | No modules visible |
| 02_sites_only | test_sites_only | SITES | Admin | Only Sites visible |
| 03_products_only | test_products_only | PRODUCTS | Admin | Only Products visible |
| 04_categories_only | test_categories_only | CATEGORIES | Admin | Only Categories visible |
| 05_customers_only | test_customers_only | CUSTOMERS | Admin | Only Customers visible |
| 06_packaging_only | test_packaging_only | PACKAGING_TYPES | Admin | Only Packaging visible |
| 07_stock_only | test_stock_only | STOCK | Home | Only Stock visible |
| 08_purchases_only | test_purchases_only | PURCHASES | Home | Only Purchases visible |
| 09_sales_only | test_sales_only | SALES | Home | Only Sales visible |
| 10_transfers_only | test_transfers_only | TRANSFERS | Home | Only Transfers visible |
| 11_inventory_only | test_inventory_only | INVENTORY | Home | Only Inventory visible |
| 12_users_only | test_users_only | USERS | Admin | Only Users visible |
| 13_audit_only | test_audit_only | AUDIT | Admin | Only Audit visible |

## UI Elements Validated

### Home Screen Operations
- "Purchase Products" button
- "Sell Products" button
- "Transfer Products" button
- "View Stock" button
- "Inventory Stock" button
- "Administration" button

### Admin Menu Items
- "Site Management"
- "Manage Products" (covers Products and Categories)
- "Packaging Types"
- "Manage Customers"
- "User Management"
- "Audit History"

## Platform Differences

### Android
- Uses resource IDs for login: `com.medistock:id/editUsername`, `com.medistock:id/editPassword`, `com.medistock:id/btnLogin`
- Text-based selectors with regex for buttons: `.*Purchase Products.*`
- Optional assertions account for async UI rendering

### iOS
- Uses text selectors: `"Username"`, `"Password"`, `"Login"`
- Index-based selection for duplicate labels: `text: "Login", index: 1`
- Consistent navigation structure: "MediStock" title, "Operations" section header

## Troubleshooting

### Test user not found
Ensure you're running a debug build with seeded test users. The test users are automatically created when the app starts in debug mode.

### Elements not visible
Some operations/admin modules may be hidden based on site context or other conditions. The tests use `optional: true` on assertions to handle this gracefully.

### Login fails
Verify the password is correct: `Test123!` (capital T, ends with exclamation mark)

### Admin menu not visible
If a user has no admin permissions, the "Administration" button will not appear on the Home screen.

## CRUD Permission Tests

The CRUD permission tests validate granular Create, Read, Update, Delete permissions within the Products module. These tests verify that UI elements (buttons, actions) are properly shown or hidden based on specific CRUD permissions.

### Test Strategy

Each CRUD test follows this pattern:

1. **Launch and Login**: Clear app state and login with specific CRUD test user
2. **Navigate to Products**: Go through Administration > Manage Products > Products
3. **Verify List UI**: Check if FAB/Add button is visible based on canCreate permission
4. **Open Product Detail**: Tap on first product in list
5. **Verify Detail UI**: Check if Edit and Delete buttons are visible based on canEdit/canDelete
6. **Screenshot**: Capture evidence of permission enforcement

### CRUD Test Coverage

| Test File | User | Permissions | Add Button | Edit Button | Delete Button |
|-----------|------|-------------|------------|-------------|---------------|
| 01_products_view_only | test_products_view | canView only | NOT visible | NOT visible | NOT visible |
| 02_products_create_only | test_products_create | canView + canCreate | VISIBLE | NOT visible | NOT visible |
| 03_products_edit_only | test_products_edit | canView + canEdit | NOT visible | VISIBLE | NOT visible |
| 04_products_delete_only | test_products_delete | canView + canDelete | NOT visible | NOT visible | VISIBLE |
| 05_products_full_crud | test_products_only | Full CRUD | VISIBLE | VISIBLE | VISIBLE |

### Example CRUD Test Flow (test_products_edit)

```yaml
# Login with test_products_edit user
- tapOn: Username
- inputText: "test_products_edit"
- tapOn: Password
- inputText: "Test123!"
- tapOn: Login

# Navigate to Products
- tapOn: "Administration"
- tapOn: "Manage Products"
- tapOn: "Products"

# Verify Add button is NOT visible
- assertNotVisible: "+"
- assertNotVisible: "Add"

# Tap on first product
- tapOn:
    index: 1

# Verify Edit button IS visible
- assertVisible: "Edit"

# Verify Delete button is NOT visible
- assertNotVisible: "Delete"
```

## Future Enhancements

Potential additions to the permission test suite:

1. **CRUD Tests for Other Modules**: Extend CRUD permission tests to Sites, Categories, Customers, etc.
2. **Action Execution Tests**: Verify users can actually perform actions (not just see UI) for their permitted operations
3. **Multi-Permission Tests**: Test users with combinations of module and CRUD permissions
4. **Role-Based Tests**: Test predefined roles (Admin, Manager, User)
5. **Permission Denial Tests**: Verify proper error messages when accessing unauthorized features
6. **Audit Tests**: Verify permission changes are logged in audit trail

## Related Documentation

- Main test documentation: `/documentation/TESTING.md`
- Maestro test guide: `/.maestro/README.md`
- Permission system implementation: `/shared/src/commonMain/kotlin/com/medistock/shared/models/Permission.kt`
- Test user seeding: `/app/src/main/java/com/medistock/util/TestDataSeeder.kt` (Android)

## Contact

For questions about the permission system or tests, refer to the main project documentation or open an issue.
