package com.dabb.business.ui.animation

import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * مواصفات السرعة المتوازنة — لا سريع ولا بطيء
 * المهارة: ㉛ (إبداعي) + ⑨ (أداء — تقليل التعقيد غير الضروري)
 */
object AnimationSpecs {
    // زر / تفاعل سريع برفق
    val BUTTON_PRESS = tween<Float>(durationMillis = 180,
        easing = androidx.compose.animation.core.FastOutSlowInEasing)

    // دخول / خروج الشاشة — متوازن ومريح
    val SCREEN_ENTER = tween<Float>(durationMillis = 650,
        easing = androidx.compose.animation.core.FastOutSlowInEasing)
    val SCREEN_EXIT = tween<Float>(durationMillis = 350,
        easing = androidx.compose.animation.core.FastOutSlowInEasing)

    // إزاحة الانزلاق — IntOffset (الموضع) بنفس المدة
    val SCREEN_ENTER_OFFSET = tween<IntOffset>(durationMillis = 650,
        easing = androidx.compose.animation.core.FastOutSlowInEasing)
    val SCREEN_EXIT_OFFSET = tween<IntOffset>(durationMillis = 350,
        easing = androidx.compose.animation.core.FastOutSlowInEasing)

    // تقليب البطاقة — بطيء قليلاً لإاحساس بالقوة
    val FLIP = tween<Float>(durationMillis = 800,
        easing = androidx.compose.animation.core.LinearOutSlowInEasing)

    // تكراري (نفس) — دورة هادئة
    val REPEAT_PULSE = tween<Float>(durationMillis = 1200,
        easing = androidx.compose.animation.core.FastOutSlowInEasing)

    // شاشة كاملة — تأثر عميق
    val FULL_SCREEN = tween<Float>(durationMillis = 900,
        easing = androidx.compose.animation.core.FastOutSlowInEasing)

    // تأخير بسيط للظهور التدريجي
    val STAGGER_DELAY = 120
}
