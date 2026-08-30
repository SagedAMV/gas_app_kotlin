package com.dabb.business.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * شاشة كاملة للنجاح — تظهر فوق أي شاشة وتختفي بلمسة
 * من المعرض: #96 (تأثير نجاح كامل) + #100 (تكسير/حبيبات ملوّنة)
 */
@Composable
fun FullScreenSuccess(
    visible: Boolean,
    message: String = "تمت العملية بنجاح",
    subMessage: String = "",
    onDismiss: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.96f, animationSpec = tween(320)),
        exit = fadeOut(tween(240)) + scaleOut(targetScale = 0.96f, animationSpec = tween(240))
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
            ConfettiBurst()
            Box(
                modifier = Modifier.padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (subMessage.isNotBlank()) {
                    Text(
                        text = subMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 72.dp)
                    )
                }
            }
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

/** حبيبات ملوّنة تنفجر من المنتصف — من المعرض #100 */
@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFFE5A843), Color(0xFFFF7B54), Color(0xFF4ADE80),
        Color(0xFF60A5FA), Color(0xFFF472B6), Color(0xFFFFFFFF)
    )
    val particles = remember {
        val rnd = kotlin.random.Random(7)
        List(18) {
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
        t.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
