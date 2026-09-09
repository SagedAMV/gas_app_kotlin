package com.dabb.business.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dabb.business.ui.components.sharedAppViewModel
import com.dabb.business.data.local.SettingsStore
import com.dabb.business.ui.animation.MotionPreferences
import com.dabb.business.ui.animation.Motion
import com.dabb.business.ui.animation.MiniSpinner
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.motionDuration
import com.dabb.business.ui.animation.shakeEffect
import com.dabb.business.ui.components.AppHeader
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.dabb.business.ui.theme.DangerRed
import com.dabb.business.ui.theme.SuccessGreen
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.util.Money
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val viewModel: AppViewModel = sharedAppViewModel()
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(ctx) }

    var priceText by remember { mutableStateOf(Money.format(viewModel.defaultPricePiasters)) }
    var savedMsg by remember { mutableStateOf(false) }
    var priceErr by remember { mutableStateOf<String?>(null) }
    var pinSet by remember { mutableStateOf(store.isPinSet) }
    var pinDialog by remember { mutableStateOf(false) }
    var busyMsg by remember { mutableStateOf<String?>(null) }

    // اختيار ملف للتصدير
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            busyMsg = "جارٍ حفظ النسخة…"
            val err = viewModel.exportDatabase(uri)
            busyMsg = if (err == null) "تم حفظ النسخة الاحتياطية بنجاح" else "فشل الحفظ: $err"
        }
    }

    // اختيار ملف للاستيراد
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            busyMsg = "جارٍ الاستعادة…"
            val err = viewModel.importDatabase(uri)
            busyMsg = if (err == null) "تمت الاستعادة — سيُعاد فتح التطبيق" else "فشلت الاستعادة: $err"
            if (err == null) {
                // إعادة إنشاء العملية لتُحمَّل القاعدة المستوردة
                (ctx as? android.app.Activity)?.recreate()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(title = "الإعدادات", subtitle = "الأسعار، الأمان، النسخ الاحتياطي",
            icon = Icons.Filled.PriceChange)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            StaggeredReveal(0) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("السعر الافتراضي للأسطوانة", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = priceText,
                            // إصلاح الفحص H3: حد 11 خانة — بلا حد كان يُدخل Long.MAX
                            onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                            label = { Text("السعر (ريال)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                        )
                        // §5.2: التحول السحري — زر الحفظ يتحول إلى ✓ مرة واحدة ثم يعود
                        Button(onClick = {
                            val p = Money.poundsToPiasters(priceText)
                            // إصلاح الفحص M1: السعر الفارغ/الصفر لم يعد يُبلع صامتاً
                            if (p > 0) { viewModel.setDefaultPrice(p); savedMsg = true; priceErr = null }
                            else priceErr = "أدخل سعراً أكبر من صفر"
                        }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            AnimatedContent(
                                targetState = savedMsg,
                                transitionSpec = {
                                    (fadeIn(tween(motionDuration(150))) + scaleIn(
                                        initialScale = 0.7f,
                                        animationSpec = Motion.overshootSpring()
                                    )) togetherWith fadeOut(tween(motionDuration(150)))
                                },
                                label = "saveBtn"
                            ) { saved ->
                                if (saved) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("تم الحفظ")
                                } else Text("حفظ السعر الافتراضي")
                            }
                        }
                        if (savedMsg) Text("تم الحفظ", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                        if (priceErr != null) Text(priceErr!!, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            StaggeredReveal(1) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("قفل التطبيق (PIN)", style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f))
                        }
                        Text(if (pinSet) "القفل مفعّل — يُطلب الكود عند فتح التطبيق."
                        else "القفل غير مفعّل.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (pinSet) {
                            OutlinedButton(onClick = { store.clearPin(); pinSet = false },
                                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.LockOpen, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("إزالة القفل")
                            }
                        } else {
                            Button(onClick = { pinDialog = true },
                                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Lock, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تعيين كود (4 أرقام)")
                            }
                        }
                    }
                }
            }

            StaggeredReveal(2) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تقليل الحركة", style = MaterialTheme.typography.titleSmall)
                        Text("يوقف الحركات المتكررة (الموجات، النبض، الكشف المتدرج المتواصل) ويجعل كل الانتقالات فورية. مناسب للأجهزة البطيئة ولمن يزعجهم التكرار.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PriceChange, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("وضع تقليل الحركة", modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = MotionPreferences.reducedMotion,
                                onCheckedChange = { on ->
                                    MotionPreferences.reducedMotion = on
                                    store.reducedMotion = on
                                }
                            )
                        }
                    }
                }
            }

            StaggeredReveal(3) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("النسخ الاحتياطي والاستعادة", style = MaterialTheme.typography.titleSmall)
                        Text("تُحفَظ بياناتك في ملف قاعدة بيانات على هاتفك. انسخه دورياً لأن فقدان الهاتف = فقدان كل الديون.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                exportLauncher.launch("dabb_backup_$stamp.sqlite")
                            }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("نسخ احتياطي")
                            }
                            OutlinedButton(onClick = {
                                importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                            }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("استعادة")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
    }

    // §5.5: غطاء نسخ احتياطي — اسكرين أخضر يمنع اللمس أثناء العملية
    busyMsg?.let { msg ->
        val working = msg.startsWith("جارٍ")
        val ok = msg.startsWith("تم")
        // إخفاء تلقائي للرسائل النهائية كي لا يعلق الغطاء على الواجهة
        LaunchedEffect(msg) {
            if (!working) { kotlinx.coroutines.delay(1400); busyMsg = null }
        }
        val iconScale = remember { Animatable(0.3f) }
        LaunchedEffect(ok) {
            if (ok) iconScale.animateTo(1f, Motion.overshootSpring())
        }
        var errShake by remember { mutableIntStateOf(0) }
        LaunchedEffect(msg) { if (!working && !ok) errShake++ }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xE6000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (working) MiniSpinner(size = 44.dp, color = Color.White)
                else if (ok) Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = SuccessGreen,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
                )
                else Icon(
                    Icons.Filled.Warning, null, tint = DangerRed,
                    modifier = Modifier.size(56.dp).shakeEffect(errShake)
                )
                Text(msg, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (pinDialog) {
        PinCreateDialog(
            onDismiss = { pinDialog = false },
            onConfirm = { pin ->
                store.setPin(pin); pinSet = true; pinDialog = false
            }
        )
    }
}

@Composable
private fun PinCreateDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("تعيين كود الدخول") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الكود الجديد", style = MaterialTheme.typography.labelMedium)
                PinPadField(pin, { v -> pin = v })
                Text("تأكيد الكود", style = MaterialTheme.typography.labelMedium)
                PinPadField(pin2, { v -> pin2 = v })
                if (err != null) Text(err!!, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length != 4 -> err = "الكود يجب أن يكون 4 أرقام"
                    pin != pin2 -> err = "الكودان غير متطابقين"
                    else -> onConfirm(pin)
                }
            }) { Text("تعيين", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
