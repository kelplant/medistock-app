# Language Switching Test - iOS

## Overview

This test validates the language switching functionality in the MediStock iOS app, ensuring that users can change the app's display language and that the preference persists across app restarts.

## Test File

`/Users/xarroues/Projects/medistock-app/.maestro/ios/12_language_switching.yaml`

## What This Test Covers

### 1. Navigation
- Opens the profile menu by tapping the profile badge in the toolbar
- Navigates to Settings → Language
- Opens the language picker

### 2. Language Options
- Verifies all 8 supported languages are displayed:
  - English
  - Français (French)
  - Deutsch (German)
  - Español (Spanish)
  - Italiano (Italian)
  - Русский (Russian)
  - Ichibemba (Bemba)
  - Chinyanja (Nyanja)

### 3. Language Switching
- Switches to French and verifies UI text changes (e.g., "Mon profil", "Paramètres")
- Switches to German and verifies UI text changes (e.g., "Mein Profil", "Einstellungen")
- Switches to Spanish and verifies UI text changes (e.g., "Mi perfil", "Ajustes")
- Switches back to English and verifies restoration

### 4. Persistence
- Changes language to French
- Restarts the app
- Verifies that French is still active on the login screen
- Logs in and confirms the home screen is still in French
- Resets back to English for cleanup

## Prerequisites

### iOS Simulator Setup
1. Start an iOS simulator:
   ```bash
   open -a Simulator
   # Or from Xcode: Xcode → Open Developer Tool → Simulator
   ```

2. Build and install the app:
   ```bash
   cd /Users/xarroues/Projects/medistock-app/iosApp
   xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
     -sdk iphonesimulator \
     -destination 'platform=iOS Simulator,name=iPhone 15' \
     build
   ```

### Test Account
The test uses the default admin credentials:
- Username: `admin`
- Password: `admin`

## Running the Test

### Single Test Execution
```bash
cd /Users/xarroues/Projects/medistock-app
maestro test .maestro/ios/12_language_switching.yaml
```

### With Screenshots
Screenshots are automatically saved at key steps:
- `language_test_start_english` - Initial state
- `language_picker_opened` - Language picker view
- `language_switched_to_french` - After switching to French
- `language_switched_to_german` - After switching to German
- `language_switched_to_spanish` - After switching to Spanish
- `language_switched_back_to_english` - After returning to English
- `language_persisted_french_login_screen` - Login screen after restart
- `language_persisted_french_profile` - Profile after restart
- `language_test_cleanup_complete` - Final state

### Interactive Debugging
If the test fails, use Maestro Studio to debug:
```bash
cd /Users/xarroues/Projects/medistock-app
maestro studio
```

Then open the test file in the studio to step through and inspect elements.

## Test Duration

Approximate runtime: **2-3 minutes**
- Login: 10s
- Navigation: 5s
- Language switches (4x): 30s
- Persistence test (app restart): 60s
- Cleanup: 10s

## Expected Behavior

### Successful Test Run
- All language names are visible in the picker
- UI text updates immediately when language changes
- Correct translations appear for each language
- Language preference persists after app restart
- App can be reset to English successfully

### Common Failure Scenarios

1. **Profile badge not found**
   - Ensure you're logged in
   - Check accessibility label: "Profile - ..."

2. **Language picker doesn't open**
   - Verify Settings section is visible
   - May need to scroll down to find "Language" option

3. **UI doesn't update after language change**
   - Check if `Localized.setLanguage()` is being called
   - Verify localization files exist for the language

4. **Language doesn't persist**
   - Check UserDefaults key "app_language"
   - Verify `Localized.loadSavedLanguage()` is called on app start

## Key Text Mappings

When verifying language changes, these are the key translations:

| English | French | German | Spanish |
|---------|--------|--------|---------|
| My Profile | Mon profil | Mein Profil | Mi perfil |
| Information | Informations | Informationen | Información |
| Settings | Paramètres | Einstellungen | Ajustes |
| Language | Langue | Sprache | Idioma |
| Close | Fermer | Schließen | Cerrar |
| Operations | Opérations | Operationen | Operaciones |
| Login | Connexion | Anmeldung | Iniciar sesión |

## Architecture Notes

### Code References
- **ProfileViews.swift** (lines 437-474): `LanguagePickerView` implementation
- **Localization.swift** (lines 1-186): Language management service
- **AppLanguage enum** (lines 143-185): Supported languages definition

### How It Works
1. Language selection updates `LocalizationManager.shared.setLocale()`
2. Preference is saved to UserDefaults with key "app_language"
3. On app launch, `Localized.loadSavedLanguage()` restores preference
4. All UI strings use `Localized.strings.*` for translation
5. UI automatically refreshes when language changes

## Maintenance

### Adding New Languages
If a new language is added to the app:
1. Update the `AppLanguage` enum in `Localization.swift`
2. Add verification in Test 2 (lines 104-118)
3. Consider adding a language-specific switching test

### Updating Test Expectations
If UI text changes, update assertions:
- French text: lines 132-137
- German text: lines 159-164
- Spanish text: lines 186-191
- English text: lines 213-218

## Troubleshooting

### Test Hangs on Language Switch
- Increase timeout on `extendedWaitUntil` (currently 5000ms)
- Check if animation completes properly

### Wrong Text Detected
- Use `maestro studio` to inspect actual text values
- Update regex patterns if needed (e.g., `".*Profil.*"`)

### App Crashes on Restart
- Check logs: `xcrun simctl spawn booted log stream --predicate 'process == "iosApp"'`
- Verify database migrations are stable

## Related Documentation

- [Maestro Documentation](https://maestro.mobile.dev/)
- [MediStock E2E Tests README](../README.md)
- [Phase 11 E2E Analysis](../PHASE11_E2E_ANALYSIS.md)
