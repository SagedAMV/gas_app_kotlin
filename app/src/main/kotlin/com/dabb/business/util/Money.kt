package com.dabb.business.util

import java.util.Locale
import kotlin.math.abs

/**
 * كل المبالغ تُخزَّن كعدد صحيح (Long) بوحدة الريال اليمني.
 * الريال لا يُستخدم معه كسر (فلس) عملياً في السوق، لذلك لا كسور عشرية.
 * (أسماء الدوال محفوظة من مرحلة سابقة لتفادي تغيير واسع؛ الوحدة الآن = ريال واحد صحيح.)
 */
object Money {
    /** سقف المبالغ: مليار ريال — كافٍ لأي محل تجاري ويمنع تشبع
     *  Math.round عند Long.MAX وoverflow في `units * price` (إصلاح الفحص H3). */
    const val MAX_AMOUNT = 1_000_000_000L

    /** سقف كمية أسطوانات في عملية واحدة — يمنع List() الضخمة (OOM). */
    const val MAX_UNITS = 10_000

    /** إدخال نصي بالريال مثل "25000" → 25000. أي إدخال غير صالح = 0.
     *  القيمة تُحبَس ضمن [0, MAX_AMOUNT] — بلا سقف كان يُنتج Long.MAX ← فساد مالي. */
    fun poundsToPiasters(rialsText: String): Long {
        val v = rialsText.trim().toDoubleOrNull() ?: return 0L
        return Math.round(v).coerceIn(0L, MAX_AMOUNT)
    }

    fun poundsToPiasters(rials: Double): Long = Math.round(rials).coerceIn(0L, MAX_AMOUNT)

    /** للعدّادات المتحركة (Double). */
    fun piastersToPounds(rials: Long): Double = rials.toDouble()

    /** عرض بشري بفواصل الآلاف: 25000 → "25,000". */
    fun format(rials: Long): String {
        val sign = if (rials < 0) "-" else ""
        return sign + String.format(Locale.US, "%,d", abs(rials))
    }
}
