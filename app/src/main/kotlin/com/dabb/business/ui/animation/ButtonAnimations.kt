package com.dabb.business.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dabb.business.ui.theme.TealDeep
import com.dabb.business.ui.theme.WarmAmber
import kotlinx.coroutines.delay

/**
 * عائلة الأزرار الحركية — دليل إعادة التصميم §7.1:
 *  - GlowingButton: إجراء رئيسي إيجابي + توهّج حافة متحرك بطيء + تحويل لمؤشر تحميل
 *  - DangerButton: حذف/إلغاء — تأكيد ثانٍ بقلب نص الزر بدل حوار منفصل
 *  - GhostButton: إجراء ثانوي — بلا scale (يبقي التركيز على الزر الأساسي)
 *  - IconPulseButton: أيقونات متكررة (+/-، بحث) — نبضة 120ms + لمس لمسي
 */

/** الزر الرئيسي (CTA) — غوص TapHero + توهّج حافة دوّار بطيء (6s) + تحميل داخلي. */
@Composable
fun GlowingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    loading: Boolean = false,
    glow: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = Motion.tapHeroSpring(),
        label = "press"
    )

    // توهّج حافة متحرك — حلقة بطيئة جداً، تُوقف عند تقليل الحركة (§3.7)
    val loops = motionLoopsAllowed()
    val glowPhase = if (loops && glow) {
        val t = rememberInfiniteTransition(label = "glow")
        t.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "glowAngle"
        ).value
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
    ) {
        // طبقة التوهّج الدوّارة خلف الحواف فقط
        if (glow) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = glowPhase }
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                TealDeep.copy(alpha = 0.0f),
                                WarmAmber.copy(alpha = 0.30f),
                                Color.Transparent,
                                TealDeep.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
                    .padding(2.dp)
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(TealDeep, Color(0xFF2A6B5E), Color(0xFF357A6B))
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = !loading
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = {
                    (fadeIn(tween(motionDuration(150))) + scaleIn(
                        initialScale = 0.85f,
                        animationSpec = tween(motionDuration(150))
                    )) togetherWith fadeOut(tween(motionDuration(120)))
                },
                label = "btnState"
            ) { busy ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        MiniSpinner(size = 20.dp, color = Color.White)
                    } else {
                        if (leadingIcon != null) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        content()
                    }
                }
            }
        }
    }
}

/** زر الخطر — تأكيد ثانٍ بقلب النص (AnimatedContent) بدل حوار منفصل. */
@Composable
fun DangerButton(
    label: String,
    confirmLabel: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    autoResetMs: Long = 2500
) {
    var armed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    // تسليح مؤقت: يعود تلقائياً إن لم يُؤكَّد
    LaunchedEffect(armed) {
        if (armed) {
            delay(autoResetMs)
            armed = false
        }
    }
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (armed) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
        animationSpec = tween(motionDuration(220)),
        label = "dangerBg"
    )
    val fg by androidx.compose.animation.animateColorAsState(
        targetValue = if (armed) Color.White else MaterialTheme.colorScheme.error,
        animationSpec = tween(motionDuration(220)),
        label = "dangerFg"
    )
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(armed) {
        if (armed) shake.animateTo(0f, Motion.errorShake())
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer { translationX = shake.value }
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable {
                if (armed) { armed = false; onConfirmed() }
                else { armed = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        AnimatedContent(
            targetState = armed,
            transitionSpec = {
                (fadeIn(tween(motionDuration(160))) + scaleIn(initialScale = 0.9f,
                    animationSpec = tween(motionDuration(160)))) togetherWith
                    fadeOut(tween(motionDuration(120)))
            },
            label = "dangerText"
        ) { isArmed ->
            Text(
                if (isArmed) confirmLabel else label,
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** زر شبحي ثانوي — خلفية شفافة تظهر عند الضغط فقط، بلا scale ملحوظ. */
@Composable
fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        else Color.Transparent,
        animationSpec = tween(motionDuration(Motion.MICRO)),
        label = "ghostBg"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** زر أيقونة نابض — نبضة 120ms عند كل ضغطة + Haptic tick (إحساس ميكانيكي). */
@Composable
fun IconPulseButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Int = 22
) {
    var pressCount by remember { mutableStateOf(0) }
    val pulse = remember { androidx.compose.animation.core.Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressCount) {
        if (pressCount > 0) {
            pulse.snapTo(1f)
            pulse.animateTo(1.15f, tween(55))
            pulse.animateTo(1f, tween(65))
        }
    }
    Box(
        modifier = modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .clip(CircleShape)
            .clickable {
                pressCount++
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize.dp))
    }
}
