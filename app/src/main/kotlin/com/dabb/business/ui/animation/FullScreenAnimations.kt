package com.dabb.business.ui.animation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * شاشة كاملة للنجاح — تأثير عميق ببطء متوازن
 * يظهر أيقونة/نص يتوسع من الصفر ويضيء ببطء
 */
@Composable
fun FullScreenSuccess(
    message: String = "تمت العملية بنجاح",
    onDismiss: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = AnimationSpecs.FULL_SCREEN) +
                    scaleIn(
                        initialScale = 0.3f,
                        animationSpec = AnimationSpecs.FULL_SCREEN
                    ),
            exit = fadeOut(animationSpec = AnimationSpecs.FULL_SCREEN) +
                    scaleOut(
                        targetScale = 0.3f,
                        animationSpec = AnimationSpecs.FULL_SCREEN
                    )
        ) {
            Box(modifier = Modifier.padding(32.dp)) {
                // دائرة متوهجة خلف النص
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.5f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                // النص الرئيسي
                Text(
                    text = message,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
