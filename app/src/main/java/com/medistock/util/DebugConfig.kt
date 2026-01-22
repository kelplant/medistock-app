package com.medistock.util

import android.util.Log

/**
 * Debug configuration for the MediStock Android app.
 * Controls debug logging and other development features.
 */
object DebugConfig {
    /**
     * Enable or disable debug logging throughout the application.
     * Set to true for debug builds, false for release.
     * Note: Uses reflection to check BuildConfig.DEBUG to avoid import issues.
     */
    val isDebugEnabled: Boolean by lazy {
        try {
            val buildConfigClass = Class.forName("com.medistock.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            // Default to false if we can't determine
            false
        }
    }

    /**
     * Log a debug message if debug mode is enabled.
     * @param tag The tag for the log message
     * @param message The message to log
     */
    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d(tag, message)
        }
    }

    /**
     * Log an info message if debug mode is enabled.
     * @param tag The tag for the log message
     * @param message The message to log
     */
    fun i(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.i(tag, message)
        }
    }

    /**
     * Log a warning message if debug mode is enabled.
     * @param tag The tag for the log message
     * @param message The message to log
     */
    fun w(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.w(tag, message)
        }
    }

    /**
     * Log an error message (always logged, even in production).
     * @param tag The tag for the log message
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
