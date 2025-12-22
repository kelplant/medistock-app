package com.medistock.util

import android.content.Context

object PrefsHelper {
    private const val PREF_NAME = "medistock_prefs"
    private const val KEY_ACTIVE_SITE_ID = "active_site_id"

    fun saveActiveSiteId(context: Context, siteId: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_SITE_ID, siteId)
            .apply()
    }

    fun getActiveSiteId(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_SITE_ID, null)
    }

    fun clearActiveSite(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_SITE_ID)
            .apply()
    }
}
