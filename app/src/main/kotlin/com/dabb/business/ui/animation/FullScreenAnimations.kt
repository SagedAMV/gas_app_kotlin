package com.dabb.business.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dabb.business.ui.theme.SuccessGreen
import com.dabb.business.ui.theme.WarmAmber
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * شاشة نجاح العملية — مخصصة حسب نوع العملية (دليل إعادة التصميم §5.3):
 * كل عملية مالية لها بصمة حركية دالة على معناها:
 *  - SALE_CASH: الأسطوانة تخرج نحو الزبون + موجة تركوازية تتمدد
 *  - SALE_CREDIT: نفس الأسطوانة + شارة «بالآجل» كهرمانية تنزلق
 *  - PAYMENT_COLLECTED: عملة تسقط وترتد داخل «جيب»
 *  - STATION_INTAKE: شاحنة تدخل وتُفرغ أسطوانات تتراكم
 *  - STATION_PAYMENT: عملة تتجه نحو شعار المحطة
 *  - FIRST_SALE_TODAY: احتفال وحيد بالكونفيتي (أول بيع في اليوم فقط)
 * لا كونفيتي عشوائي لغيره — الاحتفال الاستثنائي يحتفظ بقيمته (§5.3).
 */
enum class SuccessKind { SALE_CASH, SALE_CREDIT, PAYMENT_COLLECTED, STATION_INTAKE, STATION_PAYMENT, FIRST_SALE_TODAY, GENERIC }

@Composable
fun FullScreenSuccess(
    visible: Boolean,
    message: String = "تمت العملية بنجاح",
    subMessage: String = "",
    kind: SuccessKind = SuccessKind.GENERIC,
    amount: String = "",
    onDismiss: () -> Unit = {}
) {
    val accent = when (kind) {
        SuccessKind.SALE_CASH, SuccessKind.PAYMENT_COLLECTED -> SuccessGreen
        SuccessKind.SALE_CREDIT, SuccessKind.STATION_PAYMENT -> WarmAmber
        SuccessKind.STATION_INTAKE, SuccessKind.FIRST_SALE_TODAY -> Color(0xFF2A6B5E)
        SuccessKind.GENERIC -> MaterialTheme.colorScheme.primary
    }
    val haptic = LocalHapticFeedback.current

    // إغلاق تلقائي 1500ms (1400–1800 حسب الدليل) أو بلمسة
    LaunchedEffect(visible) {
        if (visible) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(motionDuration(180))) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(motionDuration(320))),
        exit = fadeOut(tween(motionDuration(240))) +
            scaleOut(targetScale = 0.96f, animationSpec = tween(motionDuration(240)))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.82f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // موجة تتمدد من المركز بلون لهجة العملية (§5.3)
            WaveBurst(accent)
            if (kind == SuccessKind.FIRST_SALE_TODAY) {
                ConfettiBurst(
                    colors = listOf(WarmAmber, SuccessGreen, Color(0xFF7FD9C6), Color.White),
                    count = 8 // 40% من الكثافة القديمة — ألوان العلامة فقط
                )
            }
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KindVisual(kind, amount, accent)
                Spacer(Modifier.padding(top = 18.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (subMessage.isNotBlank()) {
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        text = subMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/** موجة دائرية تتمدد من المركز وتخفت — توقيت الوصول للعملية (§5.3). */
@Composable
private fun WaveBurst(accent: Color) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(motionDuration(750), easing = Motion.EaseOutCubic))
    }
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.72f * t.value
        val fade = (1f - t.value).coerceIn(0f, 1f)
        drawCircle(
            color = accent.copy(alpha = 0.30f * fade),
            radius = radius,
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = accent.copy(alpha = 0.18f * fade),
            radius = radius * 0.68f,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/** المحتوى المركزي المتحرك حسب نوع العملية. */
@Composable
private fun KindVisual(kind: SuccessKind, amount: String, accent: Color) {
    when (kind) {
        SuccessKind.SALE_CASH -> {
            // الأسطوانة تتحرك من المنتصف نحو الأسفل-يمين (تخرج للزبون — RTL)
            val t = remember { Animatable(0f) }
            LaunchedEffect(Unit) { t.animateTo(1f, tween(motionDuration(900), easing = Motion.EaseOutCubic)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.LocalGasStation, null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            translationX = t.value * 74f
                            translationY = t.value * 48f
                            rotationZ = t.value * 14f
                            alpha = 1f - t.value * 0.30f
                        }
                )
                if (amount.isNotBlank()) {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        "+$amount",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 0.8f + 0.2f * t.value
                            scaleY = 0.8f + 0.2f * t.value
                        }
                    )
                }
            }
        }
        SuccessKind.SALE_CREDIT -> {
            // نفس الأسطوانة + شارة «بالآجل» تنزلق من الأسفل (§5.3)
            val t = remember { Animatable(0f) }
            LaunchedEffect(Unit) { t.animateTo(1f, tween(motionDuration(800), easing = Motion.EaseOutCubic)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.LocalGasStation, null,
                    tint = Color.White,
                    modifier = Modifier.size(58.dp).graphicsLayer {
                        translationY = -t.value * 26f
                    }
                )
                Spacer(Modifier.padding(top = 10.dp))
                Text(
                    "بالآجل" + if (amount.isNotBlank()) " · $amount" else "",
                    color = Color(0xFF1E1400),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .graphicsLayer { translationY = (1f - t.value) * 90f }
                        .background(WarmAmber, CircleShape)
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
        }
        SuccessKind.PAYMENT_COLLECTED -> {
            // عملة تسقط من الأعلى وترتد داخل «الجيب» (spring)
            val y = remember { Animatable(-280f) }
            LaunchedEffect(Unit) {
                y.animateTo(
                    0f,
                    spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Payments, null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp).graphicsLayer { translationY = y.value }
                )
                if (amount.isNotBlank()) {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        "+$amount",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        SuccessKind.STATION_INTAKE -> {
            // شاحنة تدخل من اليسار وتتوقف + أسطوانات تتراكم (fade+scale متتابع)
            val x = remember { Animatable(-320f) }
            LaunchedEffect(Unit) {
                x.animateTo(0f, tween(motionDuration(560), easing = Motion.EaseOutQuint))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.LocalShipping, null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp).graphicsLayer { translationX = x.value }
                )
                Spacer(Modifier.padding(top = 10.dp))
                Row {
                    repeat(3) { i ->
                        val s = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(320 + i * 130L)
                            s.animateTo(1f, tween(motionDuration(260), easing = Motion.EaseOutCubic))
                        }
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(16.dp)
                                .graphicsLayer {
                                    scaleX = s.value
                                    scaleY = s.value
                                    alpha = s.value
                                }
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }
        SuccessKind.STATION_PAYMENT -> {
            // عملة تتجه نحو شعار المحطة (تدفق خارج — RTL: نحو اليمين)
            val t = remember { Animatable(0f) }
            LaunchedEffect(Unit) { t.animateTo(1f, tween(motionDuration(850), easing = Motion.EaseOutCubic)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Payments, null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp).graphicsLayer {
                        translationX = t.value * 86f
                        alpha = 1f - t.value * 0.4f
                    }
                )
                Icon(
                    Icons.Filled.LocalGasStation, null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp).graphicsLayer {
                        alpha = 0.35f + 0.65f * t.value
                    }
                )
            }
        }
        SuccessKind.FIRST_SALE_TODAY, SuccessKind.GENERIC -> {
            val s = remember { Animatable(0.4f) }
            LaunchedEffect(Unit) {
                s.animateTo(
                    1f,
                    spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)
                )
            }
            Icon(
                Icons.Filled.CheckCircle, null,
                tint = Color.White,
                modifier = Modifier.size(64.dp).graphicsLayer {
                    scaleX = s.value
                    scaleY = s.value
                }
            )
        }
    }
}

private data class ConfettiParticle(
    val angleRad: Float,
    val distance: Float,
    val size: Float,
    val color: Color,
    val spin: Float,
    val drift: Float
)

/** حبيبات ملوّنة من ألوان العلامة فقط — كثافة مخفَّضة (أول بيع في اليوم فقط). */
@Composable
private fun ConfettiBurst(colors: List<Color>, count: Int) {
    val particles = remember(colors, count) {
        val rnd = kotlin.random.Random(7)
        List(count) {
            ConfettiParticle(
                angleRad = (rnd.nextInt(360) * (Math.PI / 180.0)).toFloat(),
                distance = 70f + rnd.nextInt(100),
                size = 6f + rnd.nextInt(7),
                color = colors[rnd.nextInt(colors.size)],
                spin = 180f + rnd.nextInt(360),
                drift = (rnd.nextInt(70) - 35).toFloat()
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.snapTo(0f)
        t.animateTo(1f, tween(1300, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        particles.forEach { p ->
            val dist = p.distance * t.value * 1.9f
            val drop = 240f * t.value * t.value
            val x = (cos(p.angleRad) * dist + p.drift * t.value).roundToInt()
            val y = (sin(p.angleRad) * dist + drop - 70f).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(x, y) }
                    .size(p.size.dp)
                    .alpha(1f - 0.35f * t.value)
                    .graphicsLayer { rotationZ = p.spin * t.value }
                    .background(p.color, CircleShape)
            )
        }
    }
}
