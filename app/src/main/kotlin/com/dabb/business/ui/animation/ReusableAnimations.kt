package com.dabb.business.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * مكوّنات أنميشن قابلة لإعادة الاستخدام — مأخوذة من معرض الـ 100 تصميم.
 *
 * المصدر في المعرض:
 * - AnimatedNumber / AnimatedMoney  ←  #97 (عدّاد أرقام يعدّ تصاعدياً)
 * - StaggeredReveal                 ←  #63 (ظهور متتابع لعناصر القائمة)
 * - shakeEffect                     ←  #32 (اهتزاز عند الخطأ)
 * - pressScale                      ←  #01 (ضغطة الزر — يغوص عند اللمس)
 * - AnimatedProgressBar             ←  #62 (شريط تقدم يُملأ بسلاسة)
 */

/** رقم يعدّ تصاعدياً عند تغيّره (من المعرض #97) */
@Composable
fun AnimatedNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMillis: Int = 700
) {
    val displayed by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "counter"
    )
    Text(
        text = displayed.toString(),
        modifier = modifier,
        style = style,
        color = color
    )
}

/** مبلغ يعدّ تصاعدياً بالجنيه (من المعرض #97) */
@Composable
fun AnimatedMoney(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMillis: Int = 700
) {
    val displayed by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "money"
    )
    Text(
        text = "%.0f".format(displayed),
        modifier = modifier,
        style = style,
        color = color
    )
}

/** ظهور متتابع — كل عنصر يدخل بعد الذي قبله (من المعرض #63) */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(
        visible = shown,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 450,
                delayMillis = index * AnimationSpecs.STAGGER_DELAY
            )
        ) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(
                durationMillis = 450,
                delayMillis = index * AnimationSpecs.STAGGER_DELAY
            )
        )
    ) {
        content()
    }
}

/**
 * اهتزاز أفقي عند حدوث خطأ (من المعرض #32).
 * استخدمه هكذا: `Modifier.shakeEffect(shakeCounter)` وزد العدّاد عند كل خطأ.
 */
@Composable
fun Modifier.shakeEffect(trigger: Int): Modifier {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            offset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 450
                    -18f at 30
                    16f at 90
                    -12f at 150
                    9f at 210
                    -5f at 270
                    3f at 330
                    0f at 420
                }
            )
        }
    }
    return this.graphicsLayer { translationX = offset.value }
}

/** ضغطة ناعمة — الزر يغوص قليلاً عند اللمس (من المعرض #01) */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** شريط تقدم يُملأ بسلاسة (من المعرض #62) */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFF2A6B5E).copy(alpha = 0.10f),
    height: Dp = 10.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "progressBar"
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .alpha(0.85f)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/** إحصائية: رقم يعدّ تصاعدياً + مؤشر نبض اختياري بجانبه */
@Composable
fun AnimatedStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF2A6B5E),
    pulse: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            if (pulse) {
                BreathingIndicator(size = 10.dp)
            }
            AnimatedNumber(
                value = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor
            )
        }
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** يحسب كسراً آمناً (0..1) من عددين */
internal fun safeFraction(numerator: Int, denominator: Int): Float =
    if (denominator <= 0) 0f else (numerator.toFloat() / denominator.toFloat()).coerceIn(0f, 1f)

/** يحسب كسراً آمناً (0..1) من رقمين عشريين */
internal fun safeFraction(numerator: Double, denominator: Double): Float =
    if (denominator <= 0.0) 0f else (numerator / denominator).toFloat().coerceIn(0f, 1f)
