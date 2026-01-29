import Foundation

/// Debug configuration for the MediStock iOS app.
/// Controls debug logging and other development features.
final class DebugConfig {
    static let shared = DebugConfig()

    private static let debugModeKey = "debug_mode"

    /// Enable or disable debug logging throughout the application.
    /// Persisted in UserDefaults, can be toggled from admin settings.
    private(set) var isDebugEnabled: Bool = false

    private init() {
        if UserDefaults.standard.object(forKey: Self.debugModeKey) != nil {
            isDebugEnabled = UserDefaults.standard.bool(forKey: Self.debugModeKey)
        } else {
            #if DEBUG
            isDebugEnabled = true
            #endif
        }
    }

    /// Enable debug mode manually and persist the setting.
    func enableDebug() {
        isDebugEnabled = true
        UserDefaults.standard.set(true, forKey: Self.debugModeKey)
    }

    /// Disable debug mode manually and persist the setting.
    func disableDebug() {
        isDebugEnabled = false
        UserDefaults.standard.set(false, forKey: Self.debugModeKey)
    }

    /// Log a debug message if debug mode is enabled.
    /// - Parameters:
    ///   - tag: The tag/category for the log message
    ///   - message: The message to log (autoclosure for lazy evaluation)
    func log(_ tag: String, _ message: @autoclosure () -> String) {
        guard isDebugEnabled else { return }
        print("[\(tag)] \(message())")
    }

    /// Log a debug message if debug mode is enabled.
    /// - Parameters:
    ///   - tag: The tag/category for the log message
    ///   - message: The message to log
    func log(_ tag: String, message: String) {
        guard isDebugEnabled else { return }
        print("[\(tag)] \(message)")
    }
}

/// Global debug logging function for convenience
/// - Parameters:
///   - tag: The tag/category for the log message
///   - message: The message to log
func debugLog(_ tag: String, _ message: @autoclosure () -> String) {
    DebugConfig.shared.log(tag, message())
}
