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
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.components.AppHeader
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
                        Button(onClick = {
                            val p = Money.poundsToPiasters(priceText)
                            // إصلاح الفحص M1: السعر الفارغ/الصفر لم يعد يُبلع صامتاً
                            if (p > 0) { viewModel.setDefaultPrice(p); savedMsg = true; priceErr = null }
                            else priceErr = "أدخل سعراً أكبر من صفر"
                        }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("حفظ السعر الافتراضي")
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
                        if (busyMsg != null) {
                            Text(busyMsg!!, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
                    label = { Text("الكود (4 أرقام)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin2,
                    onValueChange = { v -> pin2 = v.filter { it.isDigit() }.take(4) },
                    label = { Text("تأكيد الكود") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
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
