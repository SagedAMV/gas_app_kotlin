package com.dabb.business.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * صف قابل للسحب يكشف إجراءً واحداً لا أكثر — دليل إعادة التصميم §6.3/§7.2.
 * سحب أفقي مطاطي: يتجاوز النصف = يبقى مفتوحاً، دون ذلك = يعود بـ spring.
 *
 * الاتجاه (تطبيق RTL دائماً): المحتوى ينزاح فيزيائياً لليسار فيكشف
 * زر الإجراء المثبَّت على الحافة اليمنى (بداية القراءة في RTL).
 */
@Composable
fun SwipeableActionRow(
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxRevealPx = with(density) { 78.dp.toPx() }
    val offsetX = remember { Animatable(0f) }

    val draggableState = rememberDraggableState { delta ->
        // مطاطية: المقاومة تزداد قرب الحد ثم ثبات بعده
        val next = (offsetX.value + delta).coerceIn(-maxRevealPx, 0f)
        scope.launch { offsetX.snapTo(next) }
    }

    Box(
        modifier
            .fillMaxWidth()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    scope.launch {
                        offsetX.animateTo(
                            targetValue = if (offsetX.value < -maxRevealPx / 2f) -maxRevealPx else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            )
    ) {
        // زر الإجراء — الحافة اليمنى فيزيائياً (بداية RTL)
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(78.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(actionColor.copy(alpha = 0.14f))
                .clickable {
                    scope.launch { offsetX.animateTo(0f) }
                    onAction()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(actionIcon, actionLabel, tint = actionColor, modifier = Modifier.size(19.dp))
                Text(actionLabel, color = actionColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }
        // المحتوى فوق الزر — ينزاح فيزيائياً لليسار
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX.value }
        ) {
            content()
        }
    }
}
