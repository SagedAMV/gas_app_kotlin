package com.dabb.business.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * مؤشرات متكررة — دليل إعادة التصميم §7.5:
 *  - BreathingIndicator: نبض بشدة متغيرة حسب البيانات (دَين مرتفع = أسرع)
 *  - MiniSpinner: وضعان — دوران لا نهائي، أو قوس تقدم يُفرَّغ/يمتلئ فعلياً
 *    (progress-aware — §5.5 للعمليات الطويلة كالنسخ الاحتياطي)
 */

/**
 * @param intensity شدة النبض (1 = معيارية 1100ms). أعلى = أسرع (دَين كبير)،
 * أقل = أهدأ. النطاق مقيد (450..2200ms) لتفادي الوميض المزعج (§6.5).
 */
@Composable
fun BreathingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = Color.Unspecified,
    intensity: Float = 1f
) {
    val c1 = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color

    if (!motionLoopsAllowed() || intensity <= 0f) {
        // تقليل حركة: مؤشر ثابت خافت — المعنى يبقى بلا حلقة
        Box(
            modifier = modifier
                .size(size)
                .alpha(0.75f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(c1.copy(alpha = 0.85f), c1.copy(alpha = 0.3f))
                    ),
                    shape = CircleShape
                )
        )
        return
    }

    val duration = (Motion.PULSE / intensity.coerceIn(0.25f, 3f)).toInt().coerceIn(450, 2200)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
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
 * حلقة تحميل خفيفة.
 * @param progress null = دوران لا نهائي (كما كان)؛ قيمة 0..1 = قوس تقدم
 * حتمي يمتلئ فعلياً — للعمليات ذات التقدم المعروف (§5.5).
 */
@Composable
fun MiniSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color.Unspecified,
    progress: Float? = null
) {
    val c1 = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color

    if (progress != null) {
        // وضع التقدم الحتمي — يُفرَّغ/يمتلئ فعلياً بدل دوران بلا نهاية
        Box(modifier = modifier.size(size)) {
            Canvas(Modifier.size(size)) {
                val stroke = 2.5.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = c1.copy(alpha = 0.18f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val p = progress.coerceIn(0f, 1f)
                if (p > 0.01f) {
                    drawArc(
                        color = c1,
                        startAngle = -90f, sweepAngle = 360f * p, useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(this.size.width - stroke, this.size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }
        return
    }

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
