# Phase 11 E2E Testing Checklist

## Current Status (2026-01-23)

### Implementation Status
- [x] Database schema updated with `is_active` column
- [x] `ReferentialIntegrityService` implemented in shared module
- [x] Soft delete queries added to `Medistock.sq`
- [ ] UI implementation (dialogs, filters, indicators)
- [ ] Repository integration with ReferentialIntegrityService
- [ ] ViewModel/business logic integration

### Test Status
- [x] Existing 22 E2E tests verified compatible with Phase 11
- [x] Analysis document created (`PHASE11_E2E_ANALYSIS.md`)
- [x] Template test created (`templates/deactivation_template.yaml`)
- [x] Documentation updated (README.md, TESTING.md)
- [ ] New deactivation tests (waiting for UI implementation)

---

## Pre-Implementation Checklist

### Before Running Existing Tests
- [ ] Ensure database migration has run (adds `is_active` column)
- [ ] Verify default value for `is_active` is 1 (active)
- [ ] Confirm existing app behavior unchanged

### Run Existing Tests
```bash
# Android
maestro test .maestro/android/

# iOS
maestro -p ios test .maestro/ios/
```

**Expected Result**: All 22 tests pass (0 failures)

---

## Post-UI-Implementation Checklist

### When Phase 11 UI is Ready

#### 1. Verify Existing Tests Still Pass
- [ ] Run all 22 existing E2E tests
- [ ] Document any failures or regressions
- [ ] Update tests if UI changed (e.g., delete button moved)

#### 2. Gather UI Specifications
- [ ] Document dialog text for deactivation
- [ ] Identify testIds for inactive indicators
- [ ] Locate "Show Inactive" toggle (if exists)
- [ ] Document dropdown behavior for inactive entities
- [ ] Identify reactivation UI flow

#### 3. Create New Deactivation Tests

##### Android Tests
- [ ] `android/12_deactivation_sites.yaml`
  - [ ] Site with products
  - [ ] Site with purchases
  - [ ] Site with sales
  - [ ] Verify dialog appears
  - [ ] Confirm deactivation
  - [ ] Verify inactive indicator

- [ ] `android/13_deactivation_products.yaml`
  - [ ] Product with purchases
  - [ ] Product with sales
  - [ ] Verify dialog and deactivation

- [ ] `android/14_deactivation_categories.yaml`
  - [ ] Category with products
  - [ ] Verify dialog and deactivation

- [ ] `android/15_deactivation_customers.yaml`
  - [ ] Customer with sales
  - [ ] Verify dialog and deactivation

- [ ] `android/16_inactive_filters.yaml`
  - [ ] Toggle show/hide inactive in Site Management
  - [ ] Toggle show/hide inactive in Product Management
  - [ ] Toggle show/hide inactive in Category Management
  - [ ] Toggle show/hide inactive in Customer Management

- [ ] `android/17_reactivation.yaml`
  - [ ] Reactivate site
  - [ ] Reactivate product
  - [ ] Reactivate category
  - [ ] Reactivate customer

- [ ] `android/18_clean_deletion.yaml`
  - [ ] Delete unused site (hard delete)
  - [ ] Delete unused product (hard delete)
  - [ ] Verify no deactivation dialog appears

##### iOS Tests
- [ ] `ios/12_deactivation_sites.yaml`
- [ ] `ios/13_deactivation_products.yaml`
- [ ] `ios/14_deactivation_categories.yaml`
- [ ] `ios/15_deactivation_customers.yaml`
- [ ] `ios/16_inactive_filters.yaml`
- [ ] `ios/17_reactivation.yaml`
- [ ] `ios/18_clean_deletion.yaml`

#### 4. Test Edge Cases
- [ ] Try to use inactive site in new product creation (should be hidden)
- [ ] Try to use inactive product in new purchase (should be hidden)
- [ ] Try to use inactive category in new product (should be hidden)
- [ ] Try to use inactive customer in new sale (should be hidden)
- [ ] Edit product with inactive category (should show warning)
- [ ] View purchase history with inactive product (should show with indicator)
- [ ] View sale history with inactive customer (should show with indicator)

#### 5. Create Shared Flows
- [ ] `shared/create_used_site.yaml` - Creates site with products/purchases
- [ ] `shared/create_used_product.yaml` - Creates product with purchases/sales
- [ ] `shared/create_used_category.yaml` - Creates category with products
- [ ] `shared/create_used_customer.yaml` - Creates customer with sales
- [ ] `shared/verify_deactivation_dialog.yaml` - Reusable deactivation checks
- [ ] `shared/toggle_inactive_filter.yaml` - Reusable filter toggle

#### 6. Update Documentation
- [ ] Update `.maestro/README.md` with new test descriptions
- [ ] Update `documentation/TESTING.md` with Phase 11 section
- [ ] Update test count (from 22 to ~40 tests)
- [ ] Add screenshots of deactivation dialogs
- [ ] Document testIds for new UI elements

#### 7. Run Full Test Suite
```bash
# Run all tests (Android + iOS)
./scripts/run_tests_all.sh

# Or individually
maestro test .maestro/android/
maestro -p ios test .maestro/ios/
```

**Expected Result**: All tests pass (~40 tests total after Phase 11)

---

## Test Scenarios Matrix

| Entity Type | Clean Delete | Deactivate (Used) | Reactivate | Filter Toggle | Dropdown Exclusion |
|-------------|--------------|-------------------|------------|---------------|-------------------|
| Site        | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed |
| Product     | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed |
| Category    | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed |
| Customer    | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed | ✅ Test needed |
| Packaging Type | ⚠️ Optional | ⚠️ Optional | ⚠️ Optional | ⚠️ Optional | ⚠️ Optional |
| User        | ⚠️ Optional | ⚠️ Optional | ⚠️ Optional | ⚠️ Optional | ⚠️ Optional |

---

## Risk Assessment

| Risk | Likelihood | Impact | Status | Mitigation |
|------|------------|--------|--------|------------|
| Existing tests break | Low | Medium | ✅ Verified | Tests work with current schema |
| UI changes break tests | Medium | High | ⏳ Monitor | Run tests after each UI PR |
| Async deactivation causes flakiness | Low | Medium | ⏳ Watch | Use proper wait conditions |
| Inconsistent Android/iOS behavior | Low | High | ⏳ Test | Test both platforms in parallel |
| Missing edge cases | Medium | High | ⏳ Plan | Follow test scenario matrix |

---

## Questions for Dev Team (Before Creating Tests)

### UI Design Questions
1. What text appears in the deactivation dialog?
   - Title: "Cannot Delete [Entity]"? "Delete Not Allowed"?
   - Message: "This [entity] is used in X places"?
   - Buttons: "Deactivate" + "Cancel"? Other labels?

2. How are inactive entities displayed in lists?
   - Badge with "Inactive" text?
   - Grey/faded text color?
   - Strikethrough text?
   - Icon indicator?

3. Where is the "Show Inactive" toggle located?
   - In toolbar?
   - In overflow menu?
   - As a filter button?
   - As a FAB menu item?

4. Do dropdowns show inactive entities at all?
   - Completely hidden?
   - Shown but disabled?
   - Exception: current selection shows even if inactive?

### Technical Questions
5. What are the testIds for:
   - Deactivation dialog?
   - Dialog buttons?
   - Inactive indicators?
   - "Show Inactive" toggle?
   - Reactivate button?

6. Does the dialog show detailed usage information?
   - "Used in 3 products, 2 sales, 1 transfer"?
   - Or just "This entity cannot be deleted"?

7. What happens when editing an entity with inactive references?
   - Warning message shown?
   - Reference shown with "Inactive" label?
   - User can keep inactive reference?

8. Are there any animations/transitions?
   - When dialog appears?
   - When entity becomes inactive?
   - Need to add wait conditions?

---

## Success Criteria

### Phase 11 E2E Testing Complete When:
- [x] All 22 existing tests pass
- [ ] ~14-18 new deactivation tests created (7-9 per platform)
- [ ] All new tests pass on both Android and iOS
- [ ] Edge cases covered (dropdowns, history, warnings)
- [ ] Documentation updated
- [ ] Shared flows created for reusability
- [ ] CI/CD integration verified

### Test Coverage Goals:
- 100% coverage of deactivation flows (all entity types)
- 100% coverage of clean deletion flows (unused entities)
- 100% coverage of reactivation flows
- 100% coverage of inactive filter toggles
- 100% coverage of dropdown exclusion behavior

---

## Timeline Estimate

Assuming UI is ready:

| Task | Duration | Owner |
|------|----------|-------|
| Gather UI specifications | 1 day | Dev Team |
| Create Android deactivation tests | 2 days | QA |
| Create iOS deactivation tests | 2 days | QA |
| Create shared flows | 1 day | QA |
| Test edge cases | 2 days | QA |
| Update documentation | 1 day | QA |
| Run full regression | 0.5 day | QA |
| **TOTAL** | **9.5 days** | |

---

## Resources

- **Analysis Document**: `.maestro/PHASE11_E2E_ANALYSIS.md`
- **Test Template**: `.maestro/templates/deactivation_template.yaml`
- **Maestro Docs**: https://maestro.mobile.dev/
- **ReferentialIntegrityService**: `shared/src/commonMain/kotlin/com/medistock/shared/domain/validation/ReferentialIntegrityService.kt`
- **Service Tests**: `shared/src/commonTest/kotlin/com/medistock/shared/ReferentialIntegrityServiceTest.kt`

---

**Last Updated**: 2026-01-23
**Checklist Version**: 1.0
**Status**: Ready for UI implementation
