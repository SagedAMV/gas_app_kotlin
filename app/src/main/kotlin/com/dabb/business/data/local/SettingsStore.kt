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
        // إصلاح الخطأ 13: ‏commit() كتابة متزامنة — الـ PIN لا يُفقد عند انقطاع
        // التيار فور الضغط (apply غير متزامن). الكتابة صغيرة فالأثر على الأداء مهمل.
        prefs.edit().putString(KEY_PIN_HASH, salt + ":" + sha256(pin + salt)).commit()
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_FAILS).remove(KEY_PIN_LAST_FAIL).commit()
    }

    /**
     * إصلاح الفحص M8: قفل تدرجي ضد التخمين على جهاز مفقود —
     * 4 أرقام = 10,000 احتمال، وبلا حد للمحاولات يصبح التخمين ممكناً.
     * بعد MAX_ATTEMPTS محاولة خاطئة متتالية: قفل LOCK_MS.
     */
    fun pinLockRemainingMs(now: Long = System.currentTimeMillis()): Long {
        val fails = prefs.getInt(KEY_PIN_FAILS, 0)
        if (fails < MAX_ATTEMPTS) return 0L
        val elapsed = now - prefs.getLong(KEY_PIN_LAST_FAIL, 0L)
        return if (elapsed < LOCK_MS) LOCK_MS - elapsed else 0L
    }

    /** يسجّل محاولة فاشلة ويُرجع المدة المتبقية للقفل إن تراكمت المحاولات. */
    fun registerPinFailure(now: Long = System.currentTimeMillis()): Long {
        val fails = prefs.getInt(KEY_PIN_FAILS, 0) + 1
        prefs.edit()
            .putInt(KEY_PIN_FAILS, fails)
            .putLong(KEY_PIN_LAST_FAIL, now)
            .commit()
        return pinLockRemainingMs(now)
    }

    fun resetPinFailures() {
        prefs.edit().remove(KEY_PIN_FAILS).remove(KEY_PIN_LAST_FAIL).commit()
    }

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
        private const val KEY_PIN_FAILS = "pin_fails"
        private const val KEY_PIN_LAST_FAIL = "pin_last_fail"
        private const val MAX_ATTEMPTS = 5
        private const val LOCK_MS = 60_000L
    }
}
