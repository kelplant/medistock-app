package com.medistock.shared.config

/**
 * Debug configuration for the MediStock SDK.
 * Controls debug logging and other development features.
 */
object DebugConfig {
    /**
     * Enable or disable debug logging throughout the application.
     * Set to false for production builds.
     */
    var isDebugEnabled: Boolean = false
        private set

    /**
     * Enable debug mode.
     * Should only be called during app initialization in debug builds.
     */
    fun enableDebug() {
        isDebugEnabled = true
    }

    /**
     * Disable debug mode.
     * Should be called for production builds.
     */
    fun disableDebug() {
        isDebugEnabled = false
    }

    /**
     * Log a debug message if debug mode is enabled.
     */
    inline fun log(tag: String, message: () -> String) {
        if (isDebugEnabled) {
            println("[$tag] ${message()}")
        }
    }

    /**
     * Log a debug message if debug mode is enabled.
     */
    fun log(tag: String, message: String) {
        if (isDebugEnabled) {
            println("[$tag] $message")
        }
    }
}
