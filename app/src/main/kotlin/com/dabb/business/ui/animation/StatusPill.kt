package com.dabb.business.ui.animation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * شارة حالة موحّدة (متوفر/مباع، نقدي/آجل، مسدّد/دَين) — §7.5.
 * اللون يتحرك تدريجياً فقط عند تغيّر الحالة الفعلي
 * (animateColorAsState لا تعيد التشغيل عند نفس القيمة).
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    val animated by animateColorAsState(
        targetValue = color,
        animationSpec = tween(motionDuration(320)),
        label = "pillColor"
    )
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        color = if (filled) Color.White else animated,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(
                if (filled) animated else animated.copy(alpha = 0.13f),
                shape
            )
            .border(1.dp, animated.copy(alpha = if (filled) 0f else 0.35f), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
