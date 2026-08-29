package com.dabb.business.ui.animation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * أنيميشن زر مميز — توهج ذهبي + ضغط ناعم + ظل متدرج
 * المهارة: ㊶ (Clean) + ㉛ (إبداعي)
 */
@Composable
fun GlowingButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // تأثير الضغط (Scale)
    val interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.96f else 1.0f)
            .animatedContentSize(animationSpec = AnimationSpecs.BUTTON_PRESS)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFE5A843).copy(alpha = 0.25f),
                        Color(0xFF2A6B5E).copy(alpha = 0.15f)
                    )
                ),
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}
