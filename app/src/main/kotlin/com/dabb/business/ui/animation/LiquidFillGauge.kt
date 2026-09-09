package com.dabb.business.ui.animation

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.sin

/** طور موجة متحرك بحلقة لا نهائية — أو ثابت عند تقليل الحركة. */
@Composable
private fun rememberWavePhase(loops: Boolean, durationMs: Int): Float {
    if (!loops) return 0.6f
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            tween(durationMs, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "wavePhase"
    )
    return phase
}

/**
 * مقياس سائل حي — أسطوانة غاز تمتلئ بموجتين متراكبتين تتحركان بسرعتين
 * مختلفتين (إحساس عمق السائل) — دليل إعادة التصميم §6.1/§6.4.
 *
 * @param fraction نسبة الامتلاء 0..1
 * @param reversed منطق معكوس (خزان الدَّين: يفرغ كلما سدّدت)
 * @param colorLow لون السائل عند النسبة المنخفضة
 * @param colorHigh لون السائل عند النسبة المرتفعة (تحذيري)
 */
@Composable
fun LiquidFillGauge(
    fraction: Float,
    modifier: Modifier = Modifier,
    reversed: Boolean = false,
    colorLow: Color = Color(0xFF2A6B5E),
    colorHigh: Color = Color(0xFFC0392B),
    bodyColor: Color = Color.White,
    label: String = "",
    labelColor: Color = Color.White
) {
    // النسبة تتحرك بسلاسة عبر CHART (900ms) عند أي تغيّر مخزون — §6.1
    val fill by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(motionDuration(Motion.CHART), easing = Motion.EaseOutCubic),
        label = "liquidFill"
    )
    // موجتان بطورين مختلفي السرعة — توقف كامل عند تقليل الحركة (§3.7)
    val loops = motionLoopsAllowed()
    val wave1 = rememberWavePhase(loops, Motion.LIQUID)
    val wave2 = rememberWavePhase(loops, (Motion.LIQUID * 1.6f).toInt())
    val liquidColor = lerp(colorLow, colorHigh, fill)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 64.dp, height = 104.dp)) {
            val bodyW = size.width
            val bodyH = size.height * 0.88f
            val left = 0f
            val top = size.height - bodyH
            val corner = 12.dp.toPx()

            // صمّام أعلى الأسطوانة
            drawRoundRect(
                color = bodyColor.copy(alpha = 0.55f),
                topLeft = Offset(size.width / 2 - 5.dp.toPx(), top - 9.dp.toPx()),
                size = Size(10.dp.toPx(), 11.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            // جسم الأسطوانة (إطار)
            val bodyPath = Path().apply {
                addRoundRect(RoundRect(Rect(Offset(left, top), Size(bodyW, bodyH)), CornerRadius(corner)))
            }
            drawPath(bodyPath, bodyColor.copy(alpha = 0.10f))
            drawPath(bodyPath, bodyColor.copy(alpha = 0.55f), style = Stroke(2.dp.toPx()))

            // السائل المتموّج — مقصوص داخل الجسم
            clipPath(bodyPath) {
                val fillTop = top + bodyH * (1f - fill)
                val amp1 = 2.6f.dp.toPx()
                val amp2 = 1.7f.dp.toPx()
                val waves = listOf(
                    Triple(amp1, wave1, 0.34f),
                    Triple(amp2, wave2, 0.22f)
                )
                // الموجة السفلية (العمق) أولاً ثم العليا
                waves.sortedByDescending { it.third }.forEach { (amp, phase, alpha) ->
                    val p = Path()
                    p.moveTo(left, top + bodyH)
                    p.lineTo(left, fillTop)
                    var x = 0f
                    while (x <= bodyW) {
                        val dir = if (reversed) -1f else 1f
                        val y = fillTop + amp * sin(dir * (x / bodyW) * 2f * PI.toFloat() * 1.6f + phase)
                        p.lineTo(left + x, y)
                        x += 5f
                    }
                    p.lineTo(left + bodyW, fillTop)
                    p.lineTo(left + bodyW, top + bodyH)
                    p.close()
                    drawPath(p, liquidColor.copy(alpha = alpha))
                }
                // جسم السائل الصلب تحت الموجات
                val solid = Path()
                solid.moveTo(left, fillTop + 3.dp.toPx())
                solid.lineTo(left + bodyW, fillTop + 3.dp.toPx())
                solid.lineTo(left + bodyW, top + bodyH)
                solid.lineTo(left, top + bodyH)
                solid.close()
                drawPath(solid, liquidColor.copy(alpha = 0.55f))
            }
        }
        if (label.isNotBlank()) {
            Text(
                label,
                color = labelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
