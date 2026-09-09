package com.dabb.business.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** شدة رسالة الخطأ — تحدد لون الشريط (§7.8). */
enum class ErrorSeverity {
    /** تحذير كهرماني — حالة قابلة للتصحيح دون خطر. */
    WARNING,

    /** خطأ أحمر — فشل عملية فعلية. */
    ERROR
}

/**
 * شريط خطأ عام موحّد — دليل إعادة التصميم §7.8:
 * يدخل من الأعلى، يعرض شريط تقدم زمني رفيعاً يتقلّص خلال 5 ثوانٍ
 * (الاختفاء متوقَّع لا مفاجئ)، ثم يختفي تلقائياً أو بلمسة «إغلاق».
 */
@Composable
fun ErrorSnackbarHost(
    error: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    severity: ErrorSeverity = ErrorSeverity.ERROR,
    durationMs: Int = 5000
) {
    AnimatedVisibility(
        visible = error != null,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(motionDuration(Motion.SHEET_IN), easing = Motion.EaseOutQuint)
        ) + fadeIn(tween(motionDuration(200))),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(motionDuration(Motion.SHEET_OUT))
        ) + fadeOut(tween(motionDuration(150)))
    ) {
        // شريط التقدم الزمني — يتقلّص خلال مدة العرض
        val progress = remember { Animatable(1f) }
        LaunchedEffect(error) {
            if (error != null) {
                progress.snapTo(1f)
                val step = 50
                var elapsed = 0
                while (elapsed < durationMs) {
                    delay(step.toLong())
                    elapsed += step
                    progress.snapTo(1f - elapsed.toFloat() / durationMs)
                }
                onDismiss()
            }
        }
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (severity == ErrorSeverity.WARNING)
                        MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Warning, null,
                    tint = if (severity == ErrorSeverity.WARNING)
                        MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.height(18.dp)
                )
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    error ?: "",
                    color = if (severity == ErrorSeverity.WARNING)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
            // خط التقدم الرفيع المتقلّص
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        (if (severity == ErrorSeverity.WARNING)
                            MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.error).copy(alpha = 0.15f)
                    )
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(
                            if (severity == ErrorSeverity.WARNING)
                                MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.error
                        )
                )
            }
        }
    }
}
