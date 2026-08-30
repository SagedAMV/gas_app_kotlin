package com.dabb.business.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.SaleEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.AnimatedNumber
import com.dabb.business.ui.animation.FullScreenSuccess
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.shakeEffect
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.theme.TealDeep
import com.dabb.business.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة الصرف — التصميم الاحترافي الجديد:
 * - خطوات مرقّمة (الزبون ← التفاصيل ← السداد) بدل كتلة عشوائية
 * - ملخص ثابت أسفل الشاشة: الإجمالي + زر التأكيد (لا يختفي مع الكيبورد)
 * - تمرير + إمساك لوحة المفاتيح (كان الزر يختفي خلف الكيبورد)
 * الأنميشنات: #97 عدّادات، #63 ظهور متتابع، #32 اهتزاز الخطأ، #96/#100 نجاح كامل
 */
@Composable
fun DispenseScreen() {
    var customerQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var units by remember { mutableStateOf(1) }
    var pricePerUnit by remember { mutableStateOf(25.0) }
    var status by remember { mutableStateOf("CREDIT") } // PAID أو CREDIT
    var notes by remember { mutableStateOf("") }
    var shakeKey by remember { mutableIntStateOf(0) }
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val viewModel: AppViewModel = viewModel()

    // بحث فوري مع تأخير بسيط (Debounce)
    LaunchedEffect(customerQuery) {
        if (customerQuery.isBlank()) {
            results = emptyList()
            searching = false
        } else {
            searching = true
            delay(250)
            results = viewModel.searchCustomers(customerQuery.trim())
            searching = false
        }
    }

    // إخفاء النجاح تلقائياً ثم تصفير النموذج
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1600)
            showSuccess = false
            customerQuery = ""
            results = emptyList()
            selectedCustomer = null
            units = 1
            notes = ""
        }
    }

    val total = units * pricePerUnit

    Column(
        modifier = Modifier
            .fillMaxSize()
            .shakeEffect(shakeKey)
    ) {
        AppHeader(
            title = "صرف أسطوانة",
            subtitle = "تسجيل بيع جديد لزبون",
            icon = Icons.Filled.Person
        )

        // المحتوى — قابل للتمرير ويحترم لوحة المفاتيح
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== الخطوة ١: الزبون =====
            StepHeader(number = "١", title = "الزبون")

            OutlinedTextField(
                value = customerQuery,
                onValueChange = { customerQuery = it; selectedCustomer = null },
                label = { Text("بحث بالاسم أو التلفون") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // نتائج البحث — ظهور متتابع (المعرض #63)
            if (results.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        results.take(4).forEachIndexed { index, c ->
                            StaggeredReveal(index = index) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCustomer = c; customerQuery = c.name }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(name = c.name)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        c.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "دينه: ${c.currentBalance().toInt()} ج",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // زر إنشاء زبون جديد
            if (customerQuery.trim().length >= 2 && results.isEmpty() && !searching) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    OutlinedButton(
                        onClick = {
                            val newCustomer = CustomerEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                name = customerQuery.trim(),
                                phone = "",
                                createdAt = System.currentTimeMillis()
                            )
                            selectedCustomer = newCustomer
                            customerQuery = newCustomer.name
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء زبون جديد: «${customerQuery.trim()}»", maxLines = 1)
                    }
                }
            }

            // بطاقة الزبون المختار — تتوسّع (المعرض #02)
            AnimatedVisibility(
                visible = selectedCustomer != null,
                enter = expandVertically(animationSpec = tween(350)) + fadeIn(tween(350)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(tween(250))
            ) {
                selectedCustomer?.let { c ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(name = c.name)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    c.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "الدين الحالي: ${c.currentBalance().toInt()} ج · المدفوع: ${c.totalPaid.toInt()} ج",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // ===== الخطوة ٢: التفاصيل =====
            StepHeader(number = "٢", title = "التفاصيل")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("عدد الأسطوانات", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (units > 1) units-- }) {
                            Icon(Icons.Filled.Remove, contentDescription = "نقص", tint = MaterialTheme.colorScheme.primary)
                        }
                        AnimatedNumber(
                            value = units,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            durationMillis = 250
                        )
                        IconButton(onClick = { units++ }) {
                            Icon(Icons.Filled.Add, contentDescription = "زيادة", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("سعر الوحدة (جنيه)", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = if (pricePerUnit == pricePerUnit.toInt().toDouble()) pricePerUnit.toInt().toString() else pricePerUnit.toString(),
                        onValueChange = { pricePerUnit = it.toDoubleOrNull() ?: 0.0 },
                        singleLine = true,
                        suffix = { Text("ج", style = MaterialTheme.typography.labelMedium) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ===== الخطوة ٣: السداد =====
            StepHeader(number = "٣", title = "حالة السداد")

            // مفتاح مقسّم (Segmented) — بأيقونات بدل الإيموجي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                SegOption(
                    label = "سدد الآن",
                    selected = status == "PAID",
                    accent = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.CheckCircle,
                    onClick = { status = "PAID" },
                    modifier = Modifier.weight(1f)
                )
                SegOption(
                    label = "بالأجل",
                    selected = status == "CREDIT",
                    accent = MaterialTheme.colorScheme.error,
                    icon = Icons.Filled.Schedule,
                    onClick = { status = "CREDIT" },
                    modifier = Modifier.weight(1f)
                )
            }

            // ملاحظات
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات (اختياري)") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // رسالة خطأ — اهتزاز (المعرض #32)
            AnimatedVisibility(visible = showError, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "اختر زبوناً وأدخل سعراً صحيحاً قبل التأكيد",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // ===== لوحة الإجمالي الثابتة أسفل الشاشة =====
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "الإجمالي",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedMoney(
                            value = total,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            durationMillis = 300
                        )
                        Text(
                            " جنيه",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Button(
                    onClick = {
                        if (selectedCustomer == null || units <= 0 || pricePerUnit <= 0) {
                            shakeKey++
                            showError = true
                        } else {
                            showError = false
                            val c = selectedCustomer!!
                            val amountPaid = if (status == "PAID") total else 0.0
                            val updatedCustomer = c.copy(
                                totalDebt = c.totalDebt + total,
                                totalPaid = c.totalPaid + amountPaid
                            )
                            viewModel.dispenseAndRecord(
                                cylinderIds = listOf("DEMO"), // في التطبيق الكامل: اختيار من المتوفر
                                customer = updatedCustomer,
                                sale = SaleEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    customerId = c.id,
                                    customerName = c.name,
                                    cylinderIdsJson = listOf("DEMO").joinToString(","),
                                    unitsSold = units,
                                    pricePerUnit = pricePerUnit,
                                    totalAmount = units * pricePerUnit,
                                    amountPaid = amountPaid,
                                    status = status,
                                    saleDate = System.currentTimeMillis(),
                                    notes = notes
                                )
                            )
                            showSuccess = true
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == "PAID") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد البيع", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // شاشة النجاح الكاملة — المعرض #96/#100
    FullScreenSuccess(
        visible = showSuccess,
        message = "تم تسجيل البيع",
        subMessage = "تم تحديث المخزون ودين الزبون",
        onDismiss = { showSuccess = false }
    )
}

/** رأس خطوة مرقّمة */
@Composable
private fun StepHeader(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
    }
}

/** صورة رمزية بالحرف الأول */
@Composable
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
    }
}

/** خيار داخل المفتاح المقسّم — يتحول لونه بسلاسة */
@Composable
private fun SegOption(
    label: String,
    selected: Boolean,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else Color.Transparent,
        animationSpec = tween(220),
        label = "segBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "segFg"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
