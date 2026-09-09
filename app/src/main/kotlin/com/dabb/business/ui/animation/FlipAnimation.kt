package com.dabb.business.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * تقليب البطاقة — دوران ثلاثي الأبعاد حول المحور Y.
 * تطوير §8 خطوة 14: تعريض زر قلب صغير مستقل عن محتوى البطاقة —
 * الأيقونة تدور 180° بمعزل عن قلب الوجه (يُستخدم في تفاصيل الزبون §6.3).
 */
@Composable
fun FlippableCard(
    modifier: Modifier = Modifier,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    isFlipped: Boolean = false
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(motionDuration(420), easing = Motion.EaseOutQuint),
        label = "flip"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 8 * density
            }
    ) {
        if (rotationY <= 90f) {
            front()
        } else {
            // عند التقليب الكامل نعرض الوجه الخلفي مع تدوير مضاد لإصلاح الاتجاه
            Box(modifier = Modifier.graphicsLayer { this.rotationY = 180f }) {
                back()
            }
        }
    }
}

/**
 * زر قلب مستقل — يستدعي onFlip عند كل ضغطة وتدور أيقونته 180°
 * تراكمياً (كل ضغطة نصف دورة إضافية) — §8 خطوة 14.
 */
@Composable
fun FlipButton(
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    contentDescription: String = "قلب البطاقة"
) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                scope.launch {
                    rotation.animateTo(
                        rotation.value + 180f,
                        tween(motionDuration(400), easing = Motion.EaseOutQuint)
                    )
                }
                onFlip()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Refresh, contentDescription,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation.value }
        )
    }
}
