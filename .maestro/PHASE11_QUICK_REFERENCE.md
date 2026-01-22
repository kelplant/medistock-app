# Phase 11 E2E Testing - Quick Reference

**Last Updated**: 2026-01-23

---

## TL;DR (Too Long; Didn't Read)

### Question: Do existing Maestro tests need updates for Phase 11?
**Answer**: NO ❌

### Question: Should we add new tests for deactivation?
**Answer**: YES ✅ But wait for UI implementation first

### Question: Will existing tests break?
**Answer**: NO ❌ All 22 tests should pass

---

## Quick Status

| Component | Status | Action Needed |
|-----------|--------|---------------|
| Database Schema | ✅ Implemented | None |
| ReferentialIntegrityService | ✅ Implemented | None |
| UI (dialogs, filters) | ⏳ Not implemented | Wait for implementation |
| Existing E2E Tests (22) | ✅ Working | None - don't modify |
| New E2E Tests (~16-18) | ⏳ Templates ready | Create after UI done |

---

## What to Do Now

### Step 1: Verify No Regressions
```bash
# Run existing tests
maestro test .maestro/android/
maestro -p ios test .maestro/ios/

# Expected: All 22 tests pass
```

### Step 2: Read Documentation
- **Start here**: `.maestro/PHASE11_SUMMARY.md`
- **Detailed analysis**: `.maestro/PHASE11_E2E_ANALYSIS.md`
- **Implementation checklist**: `.maestro/PHASE11_TEST_CHECKLIST.md`
- **Test template**: `.maestro/templates/deactivation_template.yaml`

### Step 3: Wait for UI Implementation
- Don't create new tests yet
- Monitor for Phase 11 UI PRs
- Get UI specs from dev team when ready

---

## When UI Is Ready

### Information Needed from Dev Team
1. Dialog text and button labels
2. testIds for new UI elements
3. Visual design for inactive indicators
4. Location of "Show Inactive" toggle
5. Dropdown behavior specification

### Tests to Create
```
.maestro/android/
├── 12_deactivation_sites.yaml          # NEW
├── 13_deactivation_products.yaml       # NEW
├── 14_deactivation_categories.yaml     # NEW
├── 15_deactivation_customers.yaml      # NEW
├── 16_inactive_filters.yaml            # NEW
├── 17_reactivation.yaml                # NEW
└── 18_clean_deletion.yaml              # NEW

.maestro/ios/
└── [same 7 tests for iOS]              # NEW
```

### Estimated Effort
- **Specification gathering**: 1 day
- **Test creation**: 4 days (both platforms)
- **Shared flows**: 1 day
- **Edge case testing**: 2 days
- **Documentation**: 1 day
- **Total**: ~9 days

---

## Test Scenarios at a Glance

### High Priority Scenarios

| Scenario | Description | Expected Behavior |
|----------|-------------|-------------------|
| **Deactivate Used Site** | Site has products, try to delete | Dialog appears, deactivate instead |
| **Deactivate Used Product** | Product has sales, try to delete | Dialog appears, deactivate instead |
| **Delete Clean Entity** | Entity not used, try to delete | Hard delete succeeds, no dialog |
| **Dropdown Exclusion** | Select site in product form | Inactive sites not shown |
| **History Display** | View sale with inactive product | Product shown with "Inactive" label |

### Medium Priority Scenarios

| Scenario | Description | Expected Behavior |
|----------|-------------|-------------------|
| **Show Inactive Toggle** | Toggle in Site Management | Inactive sites appear/disappear |
| **Reactivate Entity** | Reactivate inactive site | Site becomes active, appears in dropdowns |
| **Edit Warning** | Edit product with inactive category | Warning shown, can keep or change |

---

## Why Existing Tests Still Work

```
Existing Test Flow:
1. Create Site "Test Site"     → is_active = 1 (default)
2. Edit Site to "Modified"      → is_active still 1
3. Delete Site                  → Site not used anywhere
                                → ReferentialIntegrityService would return CanDelete
                                → But UI doesn't call service yet
                                → Hard delete proceeds as before
✅ Test passes
```

**Key Points**:
- Tests create clean entities (not used elsewhere)
- `is_active` defaults to 1 (active)
- UI unchanged (no new dialogs)
- Delete operations work exactly as before

---

## Phase 11 Implementation Overview

### What Was Added

**Database**:
```sql
-- Added to sites, products, categories, customers, packaging_types, users
is_active INTEGER NOT NULL DEFAULT 1
```

**Service** (`ReferentialIntegrityService.kt`):
```kotlin
// Check if entity can be deleted
fun checkDeletion(entityType, entityId) -> DeletionCheck

// Get usage details
fun getUsageDetails(entityType, entityId) -> UsageDetails

// Soft delete
fun deactivate(entityType, entityId, updatedBy) -> Result

// Reactivate
fun activate(entityType, entityId, updatedBy) -> Result
```

### What Was NOT Added (Yet)

- ❌ Deactivation dialogs in UI
- ❌ "Show Inactive" toggle in admin screens
- ❌ Visual indicators for inactive entities
- ❌ Dropdown filtering of inactive entities
- ❌ ViewModel integration with service
- ❌ Repository integration with service

---

## Reference Commands

### Run Tests
```bash
# All Android tests
maestro test .maestro/android/

# All iOS tests
maestro -p ios test .maestro/ios/

# Specific test
maestro test .maestro/android/02_sites_crud.yaml

# Both platforms
./scripts/run_tests_all.sh
```

### Debug Tests
```bash
# Interactive mode
maestro studio

# With verbose output
maestro test --verbose .maestro/android/
```

### Check Test Status
```bash
# View hierarchy (Android)
adb shell pm list packages | grep medistock

# View hierarchy (iOS)
xcrun simctl listapps booted | grep medistock
```

---

## Files Created for Phase 11

```
.maestro/
├── PHASE11_SUMMARY.md              # Executive summary (read this first)
├── PHASE11_E2E_ANALYSIS.md         # Comprehensive analysis (read for details)
├── PHASE11_TEST_CHECKLIST.md       # Implementation checklist (use when building tests)
├── PHASE11_QUICK_REFERENCE.md      # This file (quick lookup)
└── templates/
    └── deactivation_template.yaml  # Template for future tests
```

**Also Updated**:
- `.maestro/README.md` - Added Phase 11 section
- `documentation/TESTING.md` - Added section 3.11

---

## Common Questions

### Q: Do I need to modify existing CRUD tests?
**A**: No. They work as-is.

### Q: When should I create deactivation tests?
**A**: After the UI implements deactivation dialogs and filters.

### Q: How many new tests are needed?
**A**: Approximately 16-18 tests (8-9 per platform).

### Q: Will tests be flaky?
**A**: Unlikely. Use proper wait conditions in Maestro. Template includes examples.

### Q: What if tests fail after UI changes?
**A**: Run tests after each PR. Update selectors if UI elements moved.

### Q: Can I start writing tests now?
**A**: You can prepare using the template, but can't run/verify until UI is ready.

---

## Contact & Resources

### Documentation
- **Phase 11 Roadmap**: `documentation/roadmap.md` (section "Phase 11")
- **Service Implementation**: `shared/src/commonMain/kotlin/com/medistock/shared/domain/validation/ReferentialIntegrityService.kt`
- **Service Tests**: `shared/src/commonTest/kotlin/com/medistock/shared/ReferentialIntegrityServiceTest.kt`
- **Maestro Docs**: https://maestro.mobile.dev/

### Key Files to Monitor
- Watch for PRs that add deactivation dialogs
- Watch for PRs that integrate ReferentialIntegrityService into ViewModels
- Watch for PRs that add "Show Inactive" toggles

---

## Decision Tree

```
┌─────────────────────────────────┐
│ Is Phase 11 UI implemented?     │
└────────┬────────────────────────┘
         │
    ┌────┴────┐
    │   NO    │
    └────┬────┘
         │
         ├─► Don't modify existing tests
         ├─► Don't create new tests yet
         ├─► Run existing tests to verify baseline
         └─► Read documentation and prepare

    ┌────┴────┐
    │  YES    │
    └────┬────┘
         │
         ├─► Verify existing tests still pass
         ├─► Get UI specs from dev team
         ├─► Create new tests using template
         ├─► Test all scenarios (deactivate, filter, reactivate)
         ├─► Run full regression
         └─► Update documentation
```

---

**Status**: ✅ Ready to proceed once UI is implemented
**Last Verified**: 2026-01-23
**Next Review**: After Phase 11 UI implementation PR
