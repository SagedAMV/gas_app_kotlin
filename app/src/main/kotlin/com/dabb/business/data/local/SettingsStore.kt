package com.dabb.business.data.local

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * إعدادات التطبيق عبر SharedPreferences:
 * - السعر الافتراضي للأسطوانة (ريال)
 * - رمز القفل PIN — إصلاح المشكلة 6: يُخزَّن الآن «salt:hash» حيث salt عشوائي
 *   لكل جهاز، فلا تنفع جداول Rainbow ضد الـ 10,000 احتمال. الصيغة القديمة
 *   (بصمة بلا salt) تُقبل مرة واحدة ثم تُرقَّى تلقائياً للصيغة الجديدة.
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("dabb_prefs", Context.MODE_PRIVATE)

    var defaultPricePiasters: Long
        get() = prefs.getLong(KEY_PRICE, 25000L) // افتراضي 25,000 ريال
        set(value) = prefs.edit().putLong(KEY_PRICE, value).apply()

    val isPinSet: Boolean get() = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        val salt = randomSalt()
        prefs.edit().putString(KEY_PIN_HASH, salt + ":" + sha256(pin + salt)).apply()
    }

    fun clearPin() = prefs.edit().remove(KEY_PIN_HASH).apply()

    fun checkPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return if (":" in stored) {
            // الصيغة الجديدة: salt:hash
            val parts = stored.split(":", limit = 2)
            parts.size == 2 && sha256(pin + parts[0]).equals(parts[1], ignoreCase = true)
        } else {
            // صيغة قديمة (بلا salt) — تحقّق ثم ترقية تلقائية شفافة
            if (stored.equals(sha256(pin), ignoreCase = true)) {
                setPin(pin)
                true
            } else false
        }
    }

    private fun randomSalt(): String =
        ByteArray(8)
            .also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_PRICE = "default_price_piasters"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
