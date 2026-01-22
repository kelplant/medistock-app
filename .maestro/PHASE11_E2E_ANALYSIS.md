# Phase 11 E2E Testing Analysis - Referential Integrity & Soft Delete

## Executive Summary

This document analyzes the impact of Phase 11 (Referential Integrity and Soft Delete) on the existing Maestro E2E test suite and provides recommendations for test updates.

**Date**: 2026-01-23
**Status**: Phase 11 - Schema & Service Implemented, UI Not Yet Implemented
**Test Impact**: LOW - Existing tests will continue to work

---

## Phase 11 Implementation Status

### What Was Implemented ✅

1. **Database Schema Changes** (in `shared/src/commonMain/sqldelight/com/medistock/shared/db/Medistock.sq`):
   - Added `is_active INTEGER NOT NULL DEFAULT 1` column to:
     - `sites`
     - `categories`
     - `packaging_types`
     - `products`
     - `customers`
     - `users`

2. **ReferentialIntegrityService** (in `shared/src/commonMain/kotlin/com/medistock/shared/domain/validation/`):
   - `checkDeletion()` - Determines if entity can be deleted or must be deactivated
   - `getUsageDetails()` - Returns where entity is referenced
   - `deactivate()` - Soft delete (sets is_active = 0)
   - `activate()` - Reactivates entity (sets is_active = 1)
   - Checks usage across all related tables:
     - **Sites**: products, batches, movements, transfers, inventories, customers, sales
     - **Products**: batches, movements, transfers, inventory_items, sale_items
     - **Categories**: products
     - **Packaging Types**: products
     - **Customers**: sales
     - **Users**: permissions, audit_log

3. **Deactivation Queries** (in `Medistock.sq`):
   - `deactivateSite()`, `activateSite()`
   - `deactivateProduct()`, `activateProduct()`
   - `deactivateCategory()`, `activateCategory()`
   - `deactivateCustomer()`, `activateCustomer()`
   - `deactivateUser()` (activate not yet implemented)
   - Packaging type queries

### What Is NOT Yet Implemented ⏳

1. **UI Changes**: No screens have been updated to:
   - Show deactivation dialogs when deletion is blocked
   - Display inactive entities with visual indicators
   - Provide toggle to show/hide inactive entities in admin screens
   - Prevent selection of inactive entities in dropdowns (except current selection)
   - Show warnings when editing entities with inactive references

2. **Repository Integration**: Repositories still use direct delete operations, not checking ReferentialIntegrityService first

3. **ViewModel/Logic Layer**: No business logic yet calls the ReferentialIntegrityService before delete operations

---

## Impact on Existing Maestro E2E Tests

### Current Test Suite Overview

| Test File | Platform | Operations Tested | Entities Affected by Phase 11 |
|-----------|----------|-------------------|-------------------------------|
| `02_sites_crud.yaml` | Android/iOS | Create, Edit, Delete | Sites (is_active) |
| `03_products_crud.yaml` | Android/iOS | Create, Edit, Delete | Products (is_active) |
| `04_categories_crud.yaml` | Android/iOS | Create, Edit, Delete | Categories (is_active) |
| `05_customers_crud.yaml` | Android/iOS | Create, Edit, Delete | Customers (is_active) |
| `06_packaging_types_crud.yaml` | Android/iOS | Create, Edit, Delete | Packaging Types (is_active) |
| `07_users_crud.yaml` | Android/iOS | Create, Edit, Delete | Users (is_active) |

### Impact Analysis: SHOULD EXISTING TESTS WORK? ✅ YES

**Verdict**: All existing E2E tests should continue to work without modifications because:

1. **Schema Changes Are Backward Compatible**:
   - `is_active` has a `DEFAULT 1` constraint
   - All new entities are created as active by default
   - Existing CRUD operations don't need to explicitly set `is_active`

2. **Current Tests Delete Clean Entities**:
   - All CRUD tests create entities at the start
   - Tests delete these same entities before they're used anywhere
   - Since entities are not referenced, they can still be hard deleted
   - No referential integrity violations occur

3. **ReferentialIntegrityService Not Yet Integrated**:
   - UI still uses old delete operations
   - Service exists but isn't called by ViewModels/Views yet
   - Tests interact with the same UI that existed before Phase 11

4. **No UI Changes Yet**:
   - No deactivation dialogs to test
   - No inactive entity filters to test
   - No visual indicators for inactive entities

### Example: Why `02_sites_crud.yaml` Still Works

```yaml
# Test creates a site
- inputText: "Site E2E Test"
- tapOn: "Save"

# Test deletes the same site (not referenced anywhere)
- longPressOn: "Site E2E Test"
- tapOn: "Delete"
- tapOn: "Confirm"
```

This works because:
- "Site E2E Test" is newly created
- It's not referenced by products, purchases, sales, etc.
- ReferentialIntegrityService would return `DeletionCheck.CanDelete`
- Even though the service exists, the UI doesn't call it yet
- Hard delete proceeds as before

---

## What WILL Need Testing When Phase 11 UI Is Implemented

### 1. Deactivation Flow Tests (NEW - Not Yet Needed)

When the UI implements deactivation, we'll need tests like:

```yaml
# FUTURE: .maestro/shared/deactivation_flow.yaml
appId: com.medistock
---
# Scenario: Try to delete a used site

# Create a site
- tapOn: "Site Management"
- tapOn: "Add"
- inputText: "Site With Products"
- tapOn: "Save"

# Create a product using this site
- back
- tapOn: "Manage Products"
- tapOn: "Add"
- inputText: "Product at Site"
- tapOn: "Site Selector"
- tapOn: "Site With Products"
- tapOn: "Save"

# Try to delete the site (should show deactivation dialog)
- back
- tapOn: "Site Management"
- longPressOn: "Site With Products"
- tapOn: "Delete"

# Expected: Dialog saying site is used, offer deactivation
- assertVisible: "This site is used in"
- assertVisible: "products"
- assertVisible: "Deactivate instead"

# Deactivate
- tapOn: "Deactivate"

# Verify site is now marked inactive (with visual indicator)
- assertVisible:
    text: ".*Site With Products.*"
    index: 0
- assertVisible:
    text: ".*Inactive.*"
    optional: true

# Verify site doesn't appear in product creation dropdown
- tapOn: "Manage Products"
- tapOn: "Add"
- tapOn: "Site Selector"
- assertNotVisible: "Site With Products"
```

### 2. Inactive Entity Filter Tests (NEW - Not Yet Needed)

```yaml
# FUTURE: Test show/hide inactive toggle in admin screens
- tapOn: "Site Management"

# Toggle to show inactive entities
- tapOn:
    text: ".*Show Inactive.*"
    optional: true
- tapOn:
    id: ".*show_inactive_toggle.*"
    optional: true

# Verify inactive site is visible
- assertVisible: "Site With Products"
- assertVisible: "Inactive"

# Toggle to hide inactive
- tapOn:
    text: ".*Hide Inactive.*"
    optional: true

# Verify inactive site is hidden
- assertNotVisible: "Site With Products"
```

### 3. Reactivation Tests (NEW - Not Yet Needed)

```yaml
# FUTURE: Test reactivating a deactivated entity
- tapOn: "Site Management"
- tapOn: "Show Inactive"

# Select inactive site
- longPressOn: "Site With Products"
- tapOn: "Reactivate"

# Verify site is active again
- assertVisible: "Site With Products"
- assertNotVisible: "Inactive"

# Verify site appears in dropdowns again
- tapOn: "Manage Products"
- tapOn: "Add"
- tapOn: "Site Selector"
- assertVisible: "Site With Products"
```

---

## Recommendations

### 1. DO NOT Modify Existing Tests Now ✅

**Reasoning**:
- Existing tests work with current implementation
- Phase 11 UI not yet implemented
- Modifying tests now would be premature
- Tests document current behavior correctly

**Action**: NONE - Keep existing tests as-is

### 2. Monitor Test Results After UI Implementation ⏳

When Phase 11 UI is implemented, verify that:

1. **Existing CRUD tests still pass**: They should, because they delete clean entities
2. **New deactivation UI doesn't break existing flows**: Dialogs should only appear when needed

### 3. Add New Test Suites When UI Is Ready 📋

Create these NEW test files (don't modify existing):

```
.maestro/
├── android/
│   ├── 12_deactivation_sites.yaml          # NEW - Test site deactivation
│   ├── 13_deactivation_products.yaml       # NEW - Test product deactivation
│   ├── 14_deactivation_categories.yaml     # NEW - Test category deactivation
│   ├── 15_deactivation_customers.yaml      # NEW - Test customer deactivation
│   ├── 16_inactive_filters.yaml            # NEW - Test show/hide inactive
│   └── 17_reactivation.yaml                # NEW - Test reactivation flow
└── ios/
    ├── 12_deactivation_sites.yaml
    ├── 13_deactivation_products.yaml
    ├── 14_deactivation_categories.yaml
    ├── 15_deactivation_customers.yaml
    ├── 16_inactive_filters.yaml
    └── 17_reactivation.yaml
```

### 4. Create Shared Flows for Reusability 📋

```
.maestro/shared/
├── create_used_site.yaml           # Creates a site with products/sales
├── create_used_product.yaml        # Creates a product with purchases/sales
├── verify_deactivation_dialog.yaml # Reusable deactivation dialog checks
└── toggle_inactive_filter.yaml     # Reusable filter toggle
```

### 5. Update Test Documentation 📋

When new tests are added, update:
- `.maestro/README.md` - Add Phase 11 test descriptions
- `documentation/TESTING.md` - Add deactivation/reactivation tests to the table
- Test count: Update from "22 tests" to include new tests

---

## Testing Strategy for Phase 11 UI

### Scenarios to Test

| Scenario | Description | Priority |
|----------|-------------|----------|
| **Deactivate Used Site** | Site with products, try delete, deactivate instead | 🔴 High |
| **Deactivate Used Product** | Product with purchases/sales, try delete, deactivate | 🔴 High |
| **Deactivate Used Category** | Category with products, try delete, deactivate | 🔴 High |
| **Deactivate Used Customer** | Customer with sales, try delete, deactivate | 🔴 High |
| **Delete Clean Entity** | Entity not referenced anywhere, hard delete works | 🔴 High |
| **Show/Hide Inactive Filter** | Toggle filter in admin screens | 🟡 Medium |
| **Dropdown Excludes Inactive** | Inactive entities not in selection dropdowns | 🔴 High |
| **History Shows Inactive** | Purchase/Sale history shows inactive entities | 🟡 Medium |
| **Reactivate Entity** | Reactivate a deactivated entity | 🟡 Medium |
| **Edit With Inactive Reference** | Edit product with inactive category, show warning | 🟢 Low |

### User Journeys to Test

#### Journey 1: Site Deactivation (High Priority)
1. Admin creates Site A
2. Admin creates Product X at Site A
3. Admin creates Purchase for Product X at Site A
4. Admin tries to delete Site A
5. System shows "Site is used in 1 product and 1 purchase"
6. System offers "Deactivate" button
7. Admin clicks Deactivate
8. Site A marked inactive, hidden from new product creation
9. Existing Product X still shows Site A (with "Inactive" label)
10. Admin can reactivate Site A later

#### Journey 2: Product Deactivation (High Priority)
1. Admin creates Product Y
2. Manager creates Purchase for Product Y
3. Cashier creates Sale with Product Y
4. Admin tries to delete Product Y
5. System shows "Product is used in 1 purchase and 1 sale"
6. Admin deactivates Product Y
7. Product Y no longer appears in Sale screen product selector
8. Purchase/Sale history still shows Product Y (with "Inactive" label)

#### Journey 3: Clean Delete (High Priority)
1. Admin creates Category Z
2. Admin immediately tries to delete Category Z (not used anywhere)
3. System confirms deletion (no deactivation needed)
4. Category Z is hard deleted from database

---

## Schema Verification Queries

To verify Phase 11 implementation in the database:

```sql
-- Check is_active column exists
PRAGMA table_info(sites);
PRAGMA table_info(products);
PRAGMA table_info(categories);
PRAGMA table_info(customers);

-- Check default values are applied
SELECT id, name, is_active FROM sites;
SELECT id, name, is_active FROM products;

-- Check usage queries exist
-- These are defined in Medistock.sq:
-- getSiteUsageDetails, getProductUsageDetails, etc.
```

---

## Test Execution Plan

### Phase A: Current State (NOW) ✅
- Run existing 22 E2E tests (11 Android + 11 iOS)
- Verify all CRUD tests pass
- Document that tests work with Phase 11 schema changes

**Command**:
```bash
# Android
maestro test .maestro/android/

# iOS
maestro -p ios test .maestro/ios/
```

**Expected Result**: All tests pass (0 failures)

### Phase B: After UI Implementation (FUTURE) ⏳
1. Run existing tests again - verify no regressions
2. Add new deactivation tests (12 new tests minimum)
3. Add inactive filter tests (2 tests)
4. Add reactivation tests (2 tests)
5. Update documentation

**Total tests after Phase B**: ~38-40 tests (19-20 per platform)

---

## Questions for Implementation Team

Before creating deactivation tests, clarify:

1. **Dialog Design**:
   - What text appears in deactivation dialog?
   - What are button labels? ("Deactivate", "Cancel", or something else?)
   - Does dialog show usage count and details?

2. **Visual Indicators**:
   - How are inactive entities marked in lists? (grey text, strikethrough, "Inactive" badge?)
   - What testId or accessibility label is used for inactive entities?

3. **Filter UI**:
   - Where is the "Show Inactive" toggle? (toolbar, menu, filter button?)
   - What is the testId for this toggle?
   - Is it a switch, checkbox, or button?

4. **Dropdown Behavior**:
   - Do dropdowns completely hide inactive entities?
   - OR do they show with "Inactive" label and disable selection?
   - Exception: When editing, does current selection show even if inactive?

5. **Reactivation UI**:
   - How do users reactivate? (long press menu, edit screen, separate action?)
   - Any restrictions on reactivation? (admin only?)

---

## Conclusion

### Summary

✅ **Existing E2E tests DO NOT need updates now**
✅ **Phase 11 schema changes are backward compatible**
✅ **Tests will continue to pass because they delete clean entities**
⏳ **New tests needed when Phase 11 UI is implemented**
📋 **Recommendation: Wait for UI implementation before creating deactivation tests**

### Action Items

| Priority | Action | Owner | Status |
|----------|--------|-------|--------|
| 🔴 High | Run current test suite to verify no regressions | QA | TODO |
| 🟡 Medium | Define UI specs for deactivation dialogs | UI/UX | Needed before tests |
| 🟡 Medium | Define testIds for inactive entity indicators | Dev | Needed before tests |
| 🟢 Low | Create deactivation test templates (ready to use when UI done) | QA | Can start now |
| 🟢 Low | Update `.maestro/README.md` with Phase 11 section | QA | Can do now |

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Existing tests break after UI changes | Low | Medium | Run tests after each UI PR |
| New deactivation tests miss edge cases | Medium | High | Use ReferentialIntegrityServiceTest.kt as reference |
| Inconsistent behavior between Android/iOS | Low | High | Test both platforms in parallel |
| Tests become flaky due to async deactivation | Low | Medium | Use proper wait conditions in Maestro |

---

**Last Updated**: 2026-01-23
**Document Version**: 1.0
**Author**: QA Team + Claude Sonnet 4.5
