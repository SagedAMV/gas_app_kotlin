package com.dabb.business.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dabb.business.data.local.SettingsStore
import com.dabb.business.ui.animation.Motion
import com.dabb.business.ui.animation.motionDuration
import com.dabb.business.ui.animation.motionLoopsAllowed
import com.dabb.business.ui.animation.pressScale
import com.dabb.business.ui.animation.shakeEffect
import com.dabb.business.ui.theme.DeepTeal
import com.dabb.business.ui.theme.TealDeep
import com.dabb.business.ui.theme.WarmAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * بوابة قفل PIN — إعادة تصميم كاملة وفق دليل إعادة التصميم §5.1:
 *  - خلفية حيّة: تدرّج عمودي + موجتا سائل تتحركان بسرعتين (إحساس خزان غاز)
 *  - دخول الشعار بـ scaleIn(0.7→1) مع overshoot بعد 80ms
 *  - نقاط PIN: تكبير 1→1.25→1 (140ms) + تلوين متدرج عند التعبئة
 *  - أزرار زجاجية بضغط 0.9 + لمس لمسي
 *  - الخطأ: اهتزاز + وميض أحمر خافت في الخلفية
 *  - القفل التدرجي: عدّاد دائري يفرغ حول أيقونة القفل
 *  - النجاح: النقاط → ✓ ، القفل يفتح، الشاشة تنكمش نحو المركز (0.92) ثم تنكشف
 */
@Composable
fun PinGate(onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SettingsStore(ctx) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var needsPin by remember { mutableStateOf(store.isPinSet) }
    // مراحل النجاح: Locked → Unlocking (أيقونات تتحول) → Dismissing (انكماش الشاشة)
    var unlocking by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(needsPin, unlocking, dismissed) {
        if (!needsPin && !unlocking && !dismissed) onUnlocked()
    }

    if (!needsPin && !unlocking && !dismissed) return

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var errorFlash by remember { mutableIntStateOf(0) }
    // العيب 2: التهيئة من المتجر — القفل يصمد عبر إعادة تشغيل التطبيق
    // (كانت تبدأ من صفر فتُقبل أول محاولة بعد كل إقلاع جديد)
    var lockRemainingMs by remember { mutableStateOf(store.pinLockRemainingMs()) }

    LaunchedEffect(lockRemainingMs) {
        while (lockRemainingMs > 0L) {
            // آخر نبضة دقيقة: لا ننتظر 500ms كاملة إذا بقي أقل منها
            delay(minOf(500L, lockRemainingMs))
            lockRemainingMs = store.pinLockRemainingMs()
        }
    }

    fun press(d: String) {
        if (pin.length >= 4 || lockRemainingMs > 0L) return
        pin += d
        error = false
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (pin.length == 4) {
            if (store.checkPin(pin)) {
                store.resetPinFailures()
                // تسلسل النجاح (§5.1.7): تحوّلات ← انكماش ← كشف التطبيق
                unlocking = true
                needsPin = false
                scope.launch {
                    delay(340)
                    dismissed = true
                    delay(260)
                    onUnlocked()
                }
            } else {
                error = true; shakeKey++; errorFlash++; pin = ""
                lockRemainingMs = store.registerPinFailure()
            }
        }
    }

    // الشاشة كلها تنكمش نحو المركز قبل الكشف — إحساس «تفتح باب»
    AnimatedVisibility(
        visible = !dismissed,
        enter = fadeIn(tween(motionDuration(200))),
        exit = scaleOut(targetScale = 0.92f, animationSpec = tween(motionDuration(260))) +
            fadeOut(tween(motionDuration(260)))
    ) {
        Surface(
            color = TealDeep,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // ① الخلفية الحية: تدرّج + موجات سائل خلف الشعار
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(TealDeep, DeepTeal, WarmAmber.copy(alpha = 0.18f))
                            )
                        )
                )
                LiquidWaves()
                // ② وميض أحمر خافت عند الخطأ (طبقة فوق التدرّج — §5.1.5)
                val flash = remember { Animatable(0f) }
                LaunchedEffect(errorFlash) {
                    if (errorFlash > 0) {
                        flash.snapTo(0.22f)
                        flash.animateTo(0f, tween(motionDuration(150)))
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFFC0392B).copy(alpha = flash.value))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ③ الشعار: دائرة زجاجية تدخل بـ overshoot بعد 80ms
                    var logoIn by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(80); logoIn = true }
                    AnimatedVisibility(
                        visible = logoIn,
                        enter = scaleIn(
                            initialScale = 0.7f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                        ) + fadeIn(tween(motionDuration(240)))
                    ) {
                        // إصلاح فحص 2026-09-14: مقام الكسر = مدة الدرجة الحالية
                        // (كان 60ث ثابتة — راجع تعليق SettingsStore.pinLockTotalMs)
                        LockBadge(unlocking, lockRemainingMs, store.pinLockTotalMs())
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("دبب البترول", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        when {
                            lockRemainingMs > 0L -> "محاولات خاطئة متكررة — أعد المحاولة بعد ${(lockRemainingMs / 1000 + 1).toInt()} ثوانٍ"
                            error -> "الكود غير صحيح"
                            else -> "أدخل رمز الدخول"
                        },
                        color = if (error || lockRemainingMs > 0L) Color(0xFFFFD2CF) else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(28.dp))

                    // ④ نقاط PIN — تكبير وتلوين متحركان، ثم ✓ عند النجاح
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        repeat(4) { i ->
                            PinDot(
                                filled = unlocking || i < pin.length,
                                showCheck = unlocking
                            )
                        }
                    }
                    Spacer(Modifier.height(36.dp))

                    // ⑤ لوحة أرقام زجاجية — بلا لوحة أثناء القفل التدرجي
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫").chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                row.forEach { d ->
                                    if (d.isEmpty()) Spacer(Modifier.size(72.dp))
                                    else GlassKey(
                                        label = d,
                                        enabled = lockRemainingMs <= 0L && !unlocking,
                                        onClick = {
                                            if (d == "⌫") pin = pin.dropLast(1) else press(d)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** موجات السائل — طبقتان بطورين وسرعتين مختلفتين (إحساس عمق الخزان). */
@Composable
private fun LiquidWaves() {
    // العيب 17: الحلقة تُنشأ داخل الفرع — كانت تُنشأ دائماً وتعمل 60
    // إطاراً/ثانية حتى مع «تقليل الحركة» (تجميد القيمة لا يوقف الحلقة)
    val waves: List<Pair<Float, Float>> = if (motionLoopsAllowed()) {
        val t = rememberInfiniteTransition(label = "pinWaves")
        val p1 by t.animateFloat(
            initialValue = 0f, targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(Motion.LIQUID, easing = LinearEasing)),
            label = "w1"
        )
        val p2 by t.animateFloat(
            initialValue = 0f, targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween((Motion.LIQUID * 0.62f).toInt(), easing = LinearEasing)),
            label = "w2"
        )
        listOf(p1 to 0.05f, p2 to 0.08f)
    } else listOf(0.6f to 0.05f, 1.9f to 0.08f)
    Canvas(Modifier.fillMaxSize()) {
        waves.forEach { (phase, alpha) ->
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, size.height)
            var x = 0f
            val baseY = size.height * 0.72f
            while (x <= size.width) {
                val y = baseY + 14.dp.toPx() * sin((x / size.width) * 2f * PI.toFloat() * 1.8f + phase)
                path.lineTo(x, y)
                x += 8f
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, Color.White.copy(alpha = alpha))
        }
    }
}

/** شارة القفل الزجاجية + عدّاد دائري يفرغ أثناء القفل التدرجي (§5.1.6). */
@Composable
private fun LockBadge(unlocking: Boolean, lockRemainingMs: Long, lockTotalMs: Long) {
    Box(contentAlignment = Alignment.Center) {
        // حلقة العدّاد التنازلي — تفرغ بانتظام على مدى درجة القفل كاملة
        if (lockRemainingMs > 0L) {
            val fraction = if (lockTotalMs > 0L)
                (lockRemainingMs.toFloat() / lockTotalMs.toFloat()).coerceIn(0f, 1f)
            else 0f
            Canvas(Modifier.size(84.dp)) {
                val stroke = 3.5.dp.toPx()
                val inset = stroke / 2
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = WarmAmber,
                    startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            // القفل يدور 15° ثم «يفتح»: Lock → LockOpen عبر AnimatedContent
            androidx.compose.animation.AnimatedContent(
                targetState = unlocking,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.6f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(tween(motionDuration(200)))) togetherWith fadeOut(tween(motionDuration(150)))
                },
                label = "lockIcon"
            ) { open ->
                Icon(
                    if (open) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer { rotationZ = if (open) 15f else 0f }
                )
            }
        }
    }
}

/** نقطة PIN — تكبر 1→1.25→1 (140ms) وتتلون، وتتحول ✓ عند النجاح. */
@Composable
private fun PinDot(filled: Boolean, showCheck: Boolean) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(filled) {
        if (filled) {
            scale.snapTo(1f)
            scale.animateTo(1.25f, tween(motionDuration(70)))
            scale.animateTo(1f, tween(motionDuration(70)))
        }
    }
    val color by animateColorAsState(
        targetValue = if (filled || showCheck) Color.White else Color.White.copy(alpha = 0.25f),
        animationSpec = tween(motionDuration(140)),
        label = "dotColor"
    )
    Box(
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (showCheck) {
            Icon(
                Icons.Filled.Check, null,
                tint = TealDeep,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

/** زر رقمي زجاجي — ضغط 0.9 + لمس لمسي خفيف. */
@Composable
private fun GlassKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(72.dp)
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.12f else 0.05f))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * حقل إدخال PIN موحّد — يُستخدم في إعدادات تعيين الكود (§6.7):
 * نفس نقاط PinGate ولوحتها الزجاجية بدل TextField عادي.
 */
@Composable
fun PinPadField(
    pin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 4
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(maxLength) { i ->
                PinDot(filled = i < pin.length, showCheck = false)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫").chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d ->
                        if (d.isEmpty()) Spacer(Modifier.size(52.dp))
                        else Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (d == "⌫") onPinChange(pin.dropLast(1))
                                    else if (pin.length < maxLength) onPinChange(pin + d)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(d, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
