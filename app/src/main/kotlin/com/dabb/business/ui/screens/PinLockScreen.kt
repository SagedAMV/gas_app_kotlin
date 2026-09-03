package com.dabb.business.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dabb.business.data.local.SettingsStore

/**
 * حاجز قفل بكود PIN — يغطي التطبيق عند الإقلاع إن وُجد كود.
 * كود بسيط محلي (الكود مُخزَّن كهاش SHA-256).
 */
@Composable
fun PinGate(onUnlocked: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { SettingsStore(ctx) }
    var needsPin by remember { mutableStateOf(store.isPinSet) }

    // إن لم يُضبط كود، نتجاوز البوابة في أثر جانبي (لا نعدّل حالة الأب أثناء التركيب)
    LaunchedEffect(needsPin) { if (!needsPin) onUnlocked() }

    if (!needsPin) return

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var shakeKey by remember { mutableIntStateOf(0) }
    // إصلاح الفحص M8: قفل تدرجي — 5 محاولات خاطئة ← 60 ثانية بلا إدخال
    var lockRemainingMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lockRemainingMs) {
        while (lockRemainingMs > 0L) {
            kotlinx.coroutines.delay(500)
            lockRemainingMs = store.pinLockRemainingMs()
        }
    }

    fun press(d: String) {
        if (pin.length >= 4 || lockRemainingMs > 0L) return
        pin += d
        error = false
        if (pin.length == 4) {
            if (store.checkPin(pin)) {
                store.resetPinFailures()
                needsPin = false; onUnlocked()
            } else {
                error = true; shakeKey++; pin = ""
                lockRemainingMs = store.registerPinFailure()
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.primary) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(30.dp))
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
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape)
                            .background(if (i < pin.length) Color.White else Color.White.copy(alpha = 0.25f))
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫").chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { d ->
                            if (d.isEmpty()) { Spacer(Modifier.size(72.dp)) }
                            else Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable {
                                        if (d == "⌫") pin = pin.dropLast(1) else press(d)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(d, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
