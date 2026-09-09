package com.dabb.business.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * نظام الرموز الحركية الموحّد (Motion Tokens) — دليل إعادة التصميم §4.
 * يحل محل التوسّع العشوائي في AnimationSpecs ويضيف:
 *  - تدرّج المدة حسب حجم العنصر (ميكرو ≤150ms، متوسط 150-350ms، كبير 400-900ms)
 *  - منحنيات مخصصة (EaseOutQuint/EaseInQuint/EaseOutCubic) عبر CubicBezierEasing
 *  - دعم «تقليل الحركة»: motionDuration() تُرجع 0 عند التفعيل،
 *    وmotionLoopsAllowed() توقف كل الحلقات اللانهائية.
 */
object Motion {
    // ══ المنحنيات المخصصة ══
    val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseInQuint = CubicBezierEasing(0.64f, 0f, 0.78f, 0f)
    val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

    // ══ المدد (ms) — §4 ══
    const val MICRO = 100            // تفاعل فوري (أيقونة صغيرة)
    const val TAP_IN = 150           // ضغط زر عام (دخول)
    const val TAP_OUT = 220          // ضغط زر عام (عودة)
    const val TAP_HERO = 180         // الزر الرئيسي CTA — غوص أعمق
    const val CARD_ENTER = 420       // دخول بطاقة عند أول رسم
    const val CARD_ENTER_STAGGER = 70 // تأخير كل بطاقة عن سابقتها
    const val SHEET_IN = 320         // ورقة سفلية (دخول)
    const val SHEET_OUT = 220        // ورقة سفلية (خروج)
    const val TAB_SWITCH = 220       // تبديل تبويب سفلي — خفيف عمداً
    const val SCREEN_PUSH_IN = 380   // فتح شاشة فرعية (تعمّق)
    const val SCREEN_PUSH_OUT = 300
    const val FULL_SCREEN_IN = 550   // تراكب كامل (نجاح/PIN)
    const val FULL_SCREEN_OUT = 260
    const val CHART = 900            // رسم بياني (إلى 1200 عند الحاجة)
    const val COUNTER = 650          // عدّاد أرقام/مبالغ
    const val ERROR_SHAKE = 420      // اهتزاز خطأ
    const val PULSE = 1100           // نبض حالة (دَين/متوفر) — حلقة
    const val LIQUID = 2600          // موجة سائل الغاز — حلقة خطية
    const val STAGGER_FAST = 40      // قوائم البحث/الفلترة الحية
    const val STAGGER_NORMAL = 80    // أول تحميل شاشة

    // ══ الينابيع ══
    fun tapSpring(): SpringSpec<Float> =
        spring(dampingRatio = 0.55f, stiffness = 400f)

    fun tapHeroSpring(): SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    fun overshootSpring(): SpringSpec<Float> =
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)

    /** اهتزاز الخطأ — keyframes بمنحنى محسّن (§4: Motion.Error). */
    fun errorShake(): KeyframesSpec<Float> = keyframes {
        durationMillis = ERROR_SHAKE
        -16f at 30
        14f at 90
        -10f at 150
        7f at 210
        -4f at 270
        2f at 330
        0f at 415
    }
}

/**
 * تفضيل «تقليل الحركة» كحالة Compose تفاعلية — تُهيَّأ من SettingsStore
 * في AppTheme (الخطوة 28) وتُحدَّث فوراً من شاشة الإعدادات (الخطوة 29).
 */
object MotionPreferences {
    var reducedMotion by mutableStateOf(false)
}

/**
 * مدة محترمة لتقليل الحركة: 0 = snap فوري عند التفعيل.
 * ليست @Composable عمداً: تُقرأ حالتها لحظياً في أي سياق — داخل composition
 * (تُشترك في إعادة التركيب كأي قراءة حالة) وداخل LaunchedEffect (تلتقط
 * القيمة الحالية عند انطلاق الحركة — وهو سلوك §3.7 المطلوب: التبديل يسري
 * على الحركات الجديدة).
 */
fun motionDuration(ms: Int): Int = if (MotionPreferences.reducedMotion) 0 else ms

/** هل الحلقات اللانهائية مسموحة؟ (تُوقف كلياً عند تقليل الحركة — §3.7). ليست @Composable — نفس منطق motionDuration. */
fun motionLoopsAllowed(): Boolean = !MotionPreferences.reducedMotion

/** منحنى قياسي مع احترام تقليل الحركة. */
@Composable
fun motionEasing(default: androidx.compose.animation.core.Easing = FastOutSlowInEasing) =
    default
