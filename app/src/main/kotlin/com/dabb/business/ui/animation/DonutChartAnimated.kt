package com.dabb.business.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * رسم دونات متحرك — دليل إعادة التصميم §6.5/§7.4:
 *  - القوس يُرسم بزيادة sweepAngle تدريجياً (CHART، EaseOutCubic)
 *  - تغيير الفترة = تحريك بيني بين القيمتين (لا إعادة رسم من الصفر)
 *  - نسبة المنتصف تُحدَّث من نفس مصدر القوس (تزامن حسي مضمون)
 */
@Composable
fun DonutChartAnimated(
    paid: Double,
    credit: Double,
    modifier: Modifier = Modifier,
    paidColor: Color = MaterialTheme.colorScheme.primary,
    creditColor: Color = MaterialTheme.colorScheme.error,
    strokeWidth: Dp = 11.dp
) {
    val total = paid + credit
    val paidFraction = if (total <= 0.0) 0f else (paid / total).toFloat().coerceIn(0f, 1f)

    // Animatable: يبدأ من 0 (رسم تدريجي عند أول ظهور) ويتحرّك بين القيم عند التغيير
    val anim = remember { Animatable(0f) }
    LaunchedEffect(paidFraction) {
        anim.animateTo(
            targetValue = paidFraction,
            animationSpec = tween(motionDuration(Motion.CHART), easing = Motion.EaseOutCubic)
        )
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = creditColor.copy(alpha = 0.18f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (anim.value > 0.005f) {
                drawArc(
                    color = paidColor,
                    startAngle = -90f, sweepAngle = 360f * anim.value, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // النص يقرأ نفس مصدر القوس (anim) — تزامن تام §6.5
            Text("${(anim.value * 100).toInt()}٪", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Text("محصَّل", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
