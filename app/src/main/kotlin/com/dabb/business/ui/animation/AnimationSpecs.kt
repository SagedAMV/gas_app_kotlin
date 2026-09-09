package com.dabb.business.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * مواصفات قديمة — أُبقيت للتوافق فقط.
 * @deprecated استخدم {@link Motion} + motionDuration() — دليل إعادة التصميم §8 خطوة 1.
 */
@Deprecated("استخدم Motion + motionDuration()", ReplaceWith("Motion"))
object AnimationSpecs {
    @Deprecated("استخدم Motion.MICRO/TAP", ReplaceWith("Motion"))
    val BUTTON_PRESS = tween<Float>(durationMillis = Motion.TAP_HERO, easing = FastOutSlowInEasing)

    @Deprecated("استخدم Motion.SCREEN_PUSH_IN", ReplaceWith("Motion"))
    val SCREEN_ENTER = tween<Float>(durationMillis = Motion.SCREEN_PUSH_IN, easing = FastOutSlowInEasing)

    @Deprecated("استخدم Motion.SCREEN_PUSH_OUT", ReplaceWith("Motion"))
    val SCREEN_EXIT = tween<Float>(durationMillis = Motion.SCREEN_PUSH_OUT, easing = FastOutSlowInEasing)

    @Deprecated("استخدم Motion.SCREEN_PUSH_IN", ReplaceWith("Motion"))
    val SCREEN_ENTER_OFFSET = tween<IntOffset>(durationMillis = Motion.SCREEN_PUSH_IN, easing = FastOutSlowInEasing)

    @Deprecated("استخدم Motion.SCREEN_PUSH_OUT", ReplaceWith("Motion"))
    val SCREEN_EXIT_OFFSET = tween<IntOffset>(durationMillis = Motion.SCREEN_PUSH_OUT, easing = FastOutSlowInEasing)

    @Deprecated("قيمة ثابتة — تُدار الآن من Motion", ReplaceWith("Motion"))
    val FLIP = tween<Float>(durationMillis = 800, easing = LinearOutSlowInEasing)

    @Deprecated("استخدم Motion.PULSE", ReplaceWith("Motion"))
    val REPEAT_PULSE = tween<Float>(durationMillis = Motion.PULSE, easing = FastOutSlowInEasing)

    @Deprecated("استخدم Motion.FULL_SCREEN_IN", ReplaceWith("Motion"))
    val FULL_SCREEN = tween<Float>(durationMillis = Motion.FULL_SCREEN_IN, easing = FastOutSlowInEasing)

    @Deprecated("استخدم Motion.STAGGER_NORMAL", ReplaceWith("Motion"))
    const val STAGGER_DELAY = Motion.STAGGER_NORMAL
}
