package com.medistock.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.medistock.shared.i18n.L
import com.medistock.shared.i18n.Strings

/**
 * Base Activity that provides localization support.
 *
 * Subclasses should override [applyLocalizedStrings] to set localized text
 * on their UI elements. This method is called automatically in [onCreate]
 * after [setContentView].
 *
 * Usage:
 * ```kotlin
 * class MyActivity : LocalizedActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.activity_my)
 *         // applyLocalizedStrings() is called automatically
 *     }
 *
 *     override fun applyLocalizedStrings() {
 *         findViewById<TextView>(R.id.title)?.text = strings.myTitle
 *     }
 * }
 * ```
 */
abstract class LocalizedActivity : AppCompatActivity() {

    /**
     * Convenience accessor for localized strings.
     * Shorthand for [L.strings].
     */
    protected val strings: Strings
        get() = L.strings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        applyLocalizedStrings()
    }

    /**
     * Override this method to apply localized strings to UI elements.
     * This is called automatically after [onCreate] and [setContentView].
     *
     * Example:
     * ```kotlin
     * override fun applyLocalizedStrings() {
     *     supportActionBar?.title = strings.myScreenTitle
     *     findViewById<TextView>(R.id.label)?.text = strings.myLabel
     *     findViewById<Button>(R.id.button)?.text = strings.myButton
     * }
     * ```
     */
    protected open fun applyLocalizedStrings() {
        // Default implementation does nothing.
        // Subclasses override to set their localized strings.
    }
}
