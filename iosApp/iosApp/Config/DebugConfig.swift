import Foundation

/// Debug configuration for the MediStock iOS app.
/// Controls debug logging and other development features.
final class DebugConfig {
    static let shared = DebugConfig()

    /// Enable or disable debug logging throughout the application.
    /// Set to false for production builds.
    private(set) var isDebugEnabled: Bool = false

    private init() {
        #if DEBUG
        isDebugEnabled = true
        #endif
    }

    /// Enable debug mode manually.
    func enableDebug() {
        isDebugEnabled = true
    }

    /// Disable debug mode manually.
    func disableDebug() {
        isDebugEnabled = false
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
