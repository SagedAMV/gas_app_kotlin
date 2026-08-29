package com.dabb.business.ui.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotateY
import androidx.compose.ui.graphics.graphicsLayer

/**
 * تقليب البطاقة — دوران ثلاثي الأبعاد حول المحور Y
 * السرعة متوازنة (800ms) — لا سريع ولا بطيء
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
        animationSpec = AnimationSpecs.FLIP,
        label = "flip"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotationY
                cameraDistance = 8 * density
            }
    ) {
        if (rotationY <= 90f) {
            front()
        } else {
            // عند التقليب الكامل نعرض الوجه الخلفي مع تدوير مضاد لإصلاح الاتجاه
            Box(modifier = Modifier.rotateY(180f)) {
                back()
            }
        }
    }
}
