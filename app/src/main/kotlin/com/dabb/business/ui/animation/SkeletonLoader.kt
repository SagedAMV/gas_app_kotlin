package com.dabb.business.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * هياكل تحميل بلمعان متحرك (Shimmer) — تحل محل الظهور الفارغ المفاجئ
 * أثناء أول جلب من قاعدة البيانات — دليل إعادة التصميم §7.3.
 */
@Composable
private fun shimmerBrush(base: Color, highlight: Color): Brush {
    val loops = motionLoopsAllowed()
    val transition = rememberInfiniteTransition(label = "shimmer")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing)),
        label = "shimmerT"
    )
    val x = if (loops) t else 0.35f
    val w = 900f
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(w * (x * 2f - 1f), 0f),
        end = Offset(w * x * 2f, 80f)
    )
}

/** صف هيكلي واحد: دائرة + سطران (شكل صف قائمة نموذجي). */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier, rowHeight: Dp = 46.dp) {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val highlight = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(shimmerBrush(base, highlight)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth(0.62f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush(base, highlight)))
            Box(Modifier.fillMaxWidth(0.4f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(shimmerBrush(base, highlight)))
        }
    }
}

/** بطاقة هيكلية كاملة (لأول تحميل شاشة قبل وصول البيانات). */
@Composable
fun SkeletonCard(rows: Int = 3, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp)
    ) {
        repeat(rows) { SkeletonRow() }
    }
}
