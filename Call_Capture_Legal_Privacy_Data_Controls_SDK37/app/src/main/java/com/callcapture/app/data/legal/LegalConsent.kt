package com.callcapture.app.data.legal

import android.content.Context

/** Minimal local receipt. Terms acceptance never grants Android access or feature consent. */
object LegalConsent {
    const val VERSION = "2026-11-03.1"
    private fun prefs(context: Context) = context.getSharedPreferences("legal_consent", Context.MODE_PRIVATE)
    fun accepted(context: Context) = prefs(context).getString("terms_version", null) == VERSION
    fun acceptedAt(context: Context) = prefs(context).getLong("terms_time", 0L)
    fun accept(context: Context) {
        prefs(context).edit().putString("terms_version", VERSION)
            .putLong("terms_time", System.currentTimeMillis()).apply()
    }
    fun withdraw(context: Context) { prefs(context).edit().clear().apply() }
    fun recordFeature(context: Context, name: String) {
        prefs(context).edit().putString("feature:$name:version", VERSION)
            .putLong("feature:$name:time", System.currentTimeMillis()).apply()
    }
}
