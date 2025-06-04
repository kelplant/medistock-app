package com.medistock.util

import android.content.Context

object PrefsHelper {
    private const val PREF_NAME = "medistock_prefs"
    private const val KEY_ACTIVE_SITE_ID = "active_site_id"

    fun saveActiveSiteId(context: Context, siteId: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_ACTIVE_SITE_ID, siteId)
            .apply()
    }

    fun getActiveSiteId(context: Context): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_ACTIVE_SITE_ID, -1L)
    }

    fun clearActiveSite(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE_SITE_ID)
            .apply()
    }
}