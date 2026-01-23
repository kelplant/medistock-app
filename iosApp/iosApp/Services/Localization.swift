import Foundation
import shared

/// Swift wrapper for the shared LocalizationManager.
///
/// Usage:
/// ```swift
/// // Get a localized string
/// Text(Localized.save)  // "Save" in current locale
///
/// // Change language
/// Localized.setLanguage(.french)
///
/// // Format with parameters
/// let welcome = Localized.format(
///     Localized.welcomeBack,
///     "name", "John"
/// )
/// ```
enum Localized {
    /// Get the current strings object for accessing all localized strings
    static var strings: Strings {
        return LocalizationManager.shared.strings
    }

    // MARK: - Locale Management

    /// Set the current language
    static func setLanguage(_ locale: AppLanguage) {
        LocalizationManager.shared.setLocale(locale: locale.supportedLocale)
        // Persist preference
        UserDefaults.standard.set(locale.code, forKey: "app_language")
    }

    /// Set language by code (e.g., "en", "fr", "de", "es")
    @discardableResult
    static func setLanguageByCode(_ code: String) -> Bool {
        let result = LocalizationManager.shared.setLocaleByCode(code: code)
        if result {
            UserDefaults.standard.set(code, forKey: "app_language")
        }
        return result
    }

    /// Get the current language code
    static var currentLanguageCode: String {
        return LocalizationManager.shared.getCurrentLocaleCode()
    }

    /// Get the current language display name (in native language)
    static var currentLanguageDisplayName: String {
        return LocalizationManager.shared.getCurrentLocaleDisplayName()
    }

    /// Get all available languages
    static var availableLanguages: [AppLanguage] {
        return AppLanguage.allCases
    }

    /// Load saved language preference or use system language
    static func loadSavedLanguage() {
        if let savedCode = UserDefaults.standard.string(forKey: "app_language") {
            _ = setLanguageByCode(savedCode)
        } else {
            // Try to use system language (compatible with iOS 15+)
            let preferredLanguages = Locale.preferredLanguages
            let systemLanguage = preferredLanguages.first?.prefix(2).lowercased() ?? "en"
            _ = setLanguageByCode(String(systemLanguage))
        }
    }

    // MARK: - String Formatting

    /// Format a string with named parameters
    /// Example: format(strings.welcomeBack, "name", "John")
    static func format(_ template: String, _ params: Any...) -> String {
        var pairs: [KotlinPair<NSString, AnyObject>] = []
        var i = 0
        while i < params.count - 1 {
            if let key = params[i] as? String {
                let value = params[i + 1]
                pairs.append(KotlinPair(first: key as NSString, second: value as AnyObject))
            }
            i += 2
        }
        // Note: Direct format call with vararg isn't easily accessible from Swift
        // We'll do it manually
        var result = template
        i = 0
        while i < params.count - 1 {
            if let key = params[i] as? String {
                let value = params[i + 1]
                result = result.replacingOccurrences(of: "{\(key)}", with: "\(value)")
            }
            i += 2
        }
        return result
    }

    // MARK: - Quick Access to Common Strings

    static var appName: String { strings.appName }
    static var ok: String { strings.ok }
    static var cancel: String { strings.cancel }
    static var save: String { strings.save }
    static var deleteText: String { strings.delete_ }
    static var edit: String { strings.edit }
    static var add: String { strings.add }
    static var search: String { strings.search }
    static var loading: String { strings.loading }
    static var error: String { strings.error }
    static var success: String { strings.success }
    static var warning: String { strings.warning }
    static var confirm: String { strings.confirm }
    static var yes: String { strings.yes }
    static var no: String { strings.no }
    static var close: String { strings.close }
    static var back: String { strings.back }
    static var next: String { strings.next }
    static var retry: String { strings.retry }
    static var noData: String { strings.noData }
    static var required: String { strings.required }

    // Auth
    static var loginTitle: String { strings.loginTitle }
    static var username: String { strings.username }
    static var password: String { strings.password }
    static var login: String { strings.login }
    static var logout: String { strings.logout }
    static var logoutConfirm: String { strings.logoutConfirm }
    static var changePassword: String { strings.changePassword }
    static var loginError: String { strings.loginError }
    static var loginErrorInvalidCredentials: String { strings.loginErrorInvalidCredentials }
    static var welcomeBack: String { strings.welcomeBack }

    // Profile
    static var profile: String { strings.profile }
    static var myProfile: String { strings.myProfile }
    static var information: String { strings.information }
    static var currentPassword: String { strings.currentPassword }
    static var newPassword: String { strings.newPassword }
    static var confirmPassword: String { strings.confirmPassword }
    static var passwordsDoNotMatch: String { strings.passwordsDoNotMatch }
    static var passwordChangedSuccessfully: String { strings.passwordChangedSuccessfully }
    static var userNotFound: String { strings.userNotFound }
    static var incorrectPassword: String { strings.incorrectPassword }

    // Sync Status
    static var synced: String { strings.synced }
    static var pendingChanges: String { strings.pendingChanges }
    static var conflictsToResolve: String { strings.conflictsToResolve }
    static var online: String { strings.online }
    static var offline: String { strings.offline }
    static var realtimeConnected: String { strings.realtimeConnected }
    static var realtimeDisconnected: String { strings.realtimeDisconnected }
    static var lastError: String { strings.lastError }
    static var offlineMode: String { strings.offlineMode }
    static var syncing: String { strings.syncing }
    static var syncNow: String { strings.syncNow }

    // Settings
    static var settings: String { strings.settings }
    static var language: String { strings.language }
    static var selectLanguage: String { strings.selectLanguage }

    // Users
    static var fullName: String { strings.fullName }
    static var role: String { strings.role }
    static var admin: String { strings.admin }
    static var user: String { strings.user }
}

/// Available app languages
enum AppLanguage: String, CaseIterable, Identifiable {
    case english = "en"
    case french = "fr"
    case german = "de"
    case spanish = "es"
    case italian = "it"
    case russian = "ru"
    case bemba = "bem"
    case nyanja = "ny"

    var id: String { rawValue }
    var code: String { rawValue }

    var displayName: String {
        switch self {
        case .english: return "English"
        case .french: return "Français"
        case .german: return "Deutsch"
        case .spanish: return "Español"
        case .italian: return "Italiano"
        case .russian: return "Русский"
        case .bemba: return "Ichibemba"
        case .nyanja: return "Chinyanja"
        }
    }

    var supportedLocale: SupportedLocale {
        switch self {
        case .english: return .english
        case .french: return .french
        case .german: return .german
        case .spanish: return .spanish
        case .italian: return .italian
        case .russian: return .russian
        case .bemba: return .bemba
        case .nyanja: return .nyanja
        }
    }

    static func from(code: String) -> AppLanguage {
        return AppLanguage(rawValue: code) ?? .english
    }
}
