package com.dabb.business.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * مكوّنات حركية قابلة لإعادة الاستخدام — مطوَّرة وفق دليل إعادة التصميم §7:
 *  - StaggeredReveal(speed=) : سرعتان (Fast للبحث الحي، Normal لتحميل الشاشة)
 *  - AnimatedProgressBar : تدرّج لوني ديناميكي (أحمر→كهرماني→أخضر)
 *  - Modifier.animatedFocusBorder() : إطار تركيز يتحول لوناً وسماكة
 *  - Modifier.entryTilt() : دوران Y خفيف عند أول ظهور (صفوف السجلات)
 *  - Modifier.errorFlash() : حد أحمر يومض ثم يتلاشى خلال ثانيتين
 *  - StatTile / ExpandableDetailCard : توحيد البلاطات والبطاقات القابلة للتوسيع
 */

/** سرعة الظهور المتتابع — §7.3. */
enum class StaggerSpeed(val delayMs: Int) {
    Fast(Motion.STAGGER_FAST),      // بحث/فلترة حية — فورية الإحساس
    Normal(Motion.STAGGER_NORMAL)   // أول تحميل شاشة
}

/** رقم يعدّ تصاعدياً عند تغيّره. */
@Composable
fun AnimatedNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMillis: Int = Motion.COUNTER
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

/** مبلغ يعدّ تصاعدياً بالريال — بلا كسور (%.0f). */
@Composable
fun AnimatedMoney(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMillis: Int = Motion.COUNTER
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

/** ظهور متتابع — كل عنصر يدخل بعد الذي قبله. */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    speed: StaggerSpeed = StaggerSpeed.Normal,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(
        visible = shown,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = Motion.CARD_ENTER,
                delayMillis = index * speed.delayMs
            )
        ) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(
                durationMillis = Motion.CARD_ENTER,
                delayMillis = index * speed.delayMs
            )
        )
    ) {
        content()
    }
}

/**
 * اهتزاز أفقي عند حدوث خطأ — منحنى محسّن من Motion.errorShake().
 * استخدمه: `Modifier.shakeEffect(shakeCounter)` وزد العدّاد عند كل خطأ.
 */
@Composable
fun Modifier.shakeEffect(trigger: Int): Modifier {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            offset.animateTo(targetValue = 0f, animationSpec = Motion.errorShake())
        }
    }
    return this.graphicsLayer { translationX = offset.value }
}

/** ضغطة ناعمة — الزر يغوص قليلاً عند اللمس. */
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

/**
 * شريط تقدم يُملأ بسلاسة.
 * @param dynamicColor true = التدرّج الديناميكي أحمر→كهرماني→أخضر حسب النسبة (§7.4)
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFF2A6B5E).copy(alpha = 0.10f),
    height: Dp = 10.dp,
    dynamicColor: Boolean = false
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(motionDuration(900), easing = Motion.EaseOutCubic),
        label = "progressBar"
    )
    val amber = Color(0xFFE5A843)
    val red = Color(0xFFC0392B)
    val green = Color(0xFF2E7D5B)
    val effectiveColor = if (dynamicColor) {
        when {
            animated >= 1f -> green
            animated >= 0.55f -> androidx.compose.ui.graphics.lerp(amber, green, (animated - 0.55f) / 0.45f)
            animated >= 0.25f -> androidx.compose.ui.graphics.lerp(red, amber, (animated - 0.25f) / 0.30f)
            else -> red
        }
    } else color

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
                .background(effectiveColor)
        )
    }
}

/** إحصائية: رقم يعدّ تصاعدياً + مؤشر نبض اختياري بشدة متغيرة. */
@Composable
fun AnimatedStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF2A6B5E),
    pulse: Boolean = false,
    pulseIntensity: Float = 1f
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (pulse) {
                BreathingIndicator(size = 10.dp, intensity = pulseIntensity)
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

/**
 * بلاطة إحصائية موحّدة (§7.2): دخول CardEnter + عدّاد داخلي + نبض اختياري.
 */
@Composable
fun StatTile(
    icon: ImageVector,
    tint: Color,
    value: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    pulse: Boolean = false,
    pulseIntensity: Float = 1f
) {
    Card(
        modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                value()
                if (pulse) {
                    Modifier.size(5.dp)
                    BreathingIndicator(size = 7.dp, color = tint, intensity = pulseIntensity)
                }
            }
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * بطاقة قابلة للتوسيع — تفاصيل القراءة بلا حوار منفصل (§7.2):
 * النقر على الترويسة يوسّع التفاصيل بـ expandVertically(spring).
 */
@Composable
fun ExpandableDetailCard(
    header: @Composable () -> Unit,
    details: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = !expanded }
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) { header() }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = shrinkVertically(tween(motionDuration(Motion.SHEET_OUT)))
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { details() }
        }
    }
}

/**
 * إطار تركيز متحرك (§7.7): اللون والسماكة يتحولان بحركة عند اكتساب التركيز.
 * يوضع على الحاوية الأم للحقل (onFocusChanged يرصد تركيز الأبناء).
 */
@Composable
fun Modifier.animatedFocusBorder(
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = MaterialTheme.colorScheme.primary
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) color else Color.Transparent,
        animationSpec = tween(motionDuration(220)),
        label = "focusColor"
    )
    val width by animateDpAsState(
        targetValue = if (focused) 1.6.dp else 0.dp,
        animationSpec = tween(motionDuration(220)),
        label = "focusWidth"
    )
    return this
        .onFocusChanged { focused = it.hasFocus }
        .clip(shape)
        .then(
            if (width > 0.5.dp) Modifier.border(width, borderColor, shape)
            else Modifier
        )
}

/** دوران Y خفيف عند أول ظهور (صفوف السجلات — §6.4: 10° ثم استقرار). */
@Composable
fun Modifier.entryTilt(degrees: Float = 10f): Modifier {
    val rot = remember { Animatable(degrees) }
    LaunchedEffect(Unit) {
        rot.animateTo(0f, tween(motionDuration(320), easing = Motion.EaseOutQuint))
    }
    return this.graphicsLayer {
        rotationY = rot.value
        cameraDistance = 10 * density
    }
}

/**
 * وميض حد أحمر موضعي عند الخطأ (§6.2.7): يومض ثم يتلاشى خلال ثانيتين —
 * يبقى أثر مرئي يلتقطه المستخدم لاحقاً دون بقاء دائم.
 */
@Composable
fun Modifier.errorFlash(trigger: Int): Modifier {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            alpha.snapTo(0.85f)
            alpha.animateTo(0f, tween(2000, easing = FastOutSlowInEasing))
        }
    }
    return this.border(
        1.5.dp,
        Color(0xFFC0392B).copy(alpha = alpha.value.coerceIn(0f, 1f)),
        RoundedCornerShape(14.dp)
    )
}

/** يحسب كسراً آمناً (0..1) من عددين. */
internal fun safeFraction(numerator: Int, denominator: Int): Float =
    if (denominator <= 0) 0f else (numerator.toFloat() / denominator.toFloat()).coerceIn(0f, 1f)

/** يحسب كسراً آمناً (0..1) من رقمين عشريين. */
internal fun safeFraction(numerator: Double, denominator: Double): Float =
    if (denominator <= 0.0) 0f else (numerator / denominator).toFloat().coerceIn(0f, 1f)
