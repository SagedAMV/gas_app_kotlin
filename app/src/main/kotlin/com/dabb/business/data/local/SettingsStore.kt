package com.dabb.business.data.local

import android.content.Context
import java.security.MessageDigest

/**
 * إعدادات التطبيق عبر SharedPreferences:
 * - السعر الافتراضي للأسطوانة (قروش)
 * - رمز القفل PIN (يُخزَّن هاش SHA-256 لا بصريته — أدنى معقول لحماية محلية)
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("dabb_prefs", Context.MODE_PRIVATE)

    var defaultPricePiasters: Long
        get() = prefs.getLong(KEY_PRICE, 25000L) // افتراضي 25,000 ريال
        set(value) = prefs.edit().putLong(KEY_PRICE, value).apply()

    val isPinSet: Boolean get() = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, sha256(pin)).apply()
    }

    fun clearPin() = prefs.edit().remove(KEY_PIN_HASH).apply()

    fun checkPin(pin: String): Boolean =
        prefs.getString(KEY_PIN_HASH, null) == sha256(pin)

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_PRICE = "default_price_piasters"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
