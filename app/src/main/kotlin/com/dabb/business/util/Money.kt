package com.dabb.business.util

import java.util.Locale
import kotlin.math.abs

/**
 * كل المبالغ تُخزَّن كعدد صحيح (Long) بوحدة الريال اليمني.
 * الريال لا يُستخدم معه كسر (فلس) عملياً في السوق، لذلك لا كسور عشرية.
 * (أسماء الدوال محفوظة من مرحلة سابقة لتفادي تغيير واسع؛ الوحدة الآن = ريال واحد صحيح.)
 */
object Money {
    /** إدخال نصي بالريال مثل "25000" → 25000. أي إدخال غير صالح = 0. */
    fun poundsToPiasters(rialsText: String): Long {
        val v = rialsText.trim().toDoubleOrNull() ?: return 0L
        return Math.round(v)
    }

    fun poundsToPiasters(rials: Double): Long = Math.round(rials)

    /** للعدّادات المتحركة (Double). */
    fun piastersToPounds(rials: Long): Double = rials.toDouble()

    /** عرض بشري بفواصل الآلاف: 25000 → "25,000". */
    fun format(rials: Long): String {
        val sign = if (rials < 0) "-" else ""
        return sign + String.format(Locale.US, "%,d", abs(rials))
    }
}
