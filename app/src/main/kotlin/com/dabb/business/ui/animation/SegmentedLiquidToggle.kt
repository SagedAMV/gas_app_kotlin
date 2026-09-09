package com.dabb.business.ui.animation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * مفتاح انزلاقي مزدوج بخلفية «بلوب» متحركة تنزلق بين الخيارات —
 * دليل إعادة التصميم §6.2.4/§7.1. يُستخدم في: نقدي/آجل، الكل/مدينون،
 * فترات التقارير.
 *
 * إزاحة offset(x) اتجاهية (RTL-aware): المؤشر ينزلق صوب الخيار المختار
 * بصرياً في كلا الاتجاهين دون تدخل يدوي.
 */
@Composable
fun SegmentedLiquidToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    height: Int = 46
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        val itemWidth = maxWidth / options.size.coerceAtLeast(1)
        // البلوب ينزلق بمنحنى EaseOutQuint (توقّف ناعم — §5.4 نفس روح الأوراق)
        val x by animateDpAsState(
            targetValue = itemWidth * selectedIndex.coerceIn(0, options.lastIndex.coerceAtLeast(0)),
            animationSpec = tween(motionDuration(Motion.TAB_SWITCH), easing = Motion.EaseOutQuint),
            label = "blobX"
        )
        Box(
            Modifier
                .width(itemWidth)
                .fillMaxHeight()
                .offset(x = x)
                .padding(4.dp)
                .clip(RoundedCornerShape(((height - 8) / 2).dp))
                .background(accent)
        )
        Row(Modifier.fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { if (!selected) onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
