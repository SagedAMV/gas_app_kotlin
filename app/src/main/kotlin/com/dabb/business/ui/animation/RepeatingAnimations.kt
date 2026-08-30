package com.dabb.business.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * نبض متكرر — مؤشر حالة (مثلاً: بالآجل / متوفر) يتنفس بلون ذهبي
 * لا يستهلك البطارية — فقط عنصر واجهة صغير
 * من المعرض: #16 (نبض + توهج)
 */
@Composable
fun BreathingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = Color.Unspecified
) {
    val c1 = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .alpha(alpha)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        c1.copy(alpha = 0.85f),
                        c1.copy(alpha = 0.3f)
                    )
                ),
                shape = CircleShape
            )
    )
}

/**
 * حلقة تحميل دوّارة خفيفة — لعناصر صغيرة داخل الشاشة
 * من المعرض: #57 (حلقة دوّارة)
 */
@Composable
fun MiniSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Unspecified
) {
    val c1 = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = angle }
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        c1.copy(alpha = 0.0f),
                        c1.copy(alpha = 0.15f),
                        c1.copy(alpha = 0.9f)
                    )
                ),
                shape = CircleShape
            )
    )
}
