package com.dabb.business.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.SaleEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.AnimatedNumber
import com.dabb.business.ui.animation.FullScreenSuccess
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.shakeEffect
import com.dabb.business.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * واجهة الصرف — اختيار الزبون، عدد الأسطوانات، السعر، حالة السداد
 *
 * إصلاح جوهري: البحث عن الزبون كان معطّلاً (لا توجد قائمة نتائج ولا زر إنشاء)
 * — الآن يعمل البحث مع اقتراحات فورية وزر «زبون جديد».
 *
 * الأنميشنات المدمجة من المعرض:
 * - #63: نتائج البحث + بطاقة الزبون تظهران متتابعتين
 * - #02: بطاقة الزبون تتوسّع للأسفل عند الاختيار
 * - #97: العدّادات (عدد الأسطوانات + الإجمالي) تعدّ تصاعدياً
 * - #01: زر التأكيد يغوص عند اللمس
 * - #32: اهتزاز عند محاولة بيع ناقصة
 * - #96/#100: شاشة نجاح كاملة مع حبيبات ملوّنة بعد البيع
 */
@Composable
fun DispenseScreen(
    onBack: () -> Unit,
    onSaleRecorded: () -> Unit
) {
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

    // إخفاء النجاح تلقائياً ثم العودة
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1600)
            onSaleRecorded()
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .shakeEffect(shakeKey),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("→ رجوع") }
            Text("🔄 صرف أسطوانة", style = MaterialTheme.typography.headlineSmall)
        }

        // اختيار الزبون
        OutlinedTextField(
            value = customerQuery,
            onValueChange = { customerQuery = it; selectedCustomer = null },
            label = { Text("بحث عن زبون (اسم أو تلفون)") },
            trailingIcon = {
                if (searching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // نتائج البحث — ظهور متتابع (المعرض #63)
        if (results.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(4.dp)) {
                    results.take(4).forEachIndexed { index, c ->
                        StaggeredReveal(index = index) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCustomer = c; customerQuery = c.name }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤 ${c.name}", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "دينه: ${c.currentBalance().toInt()} ج",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // زر إنشاء زبون جديد إذا لم توجد نتائج
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("＋ إنشاء زبون جديد: «${customerQuery.trim()}»")
                }
            }
        }

        // عرض الزبون المختار — يتوسّع من الأعلى (المعرض #02)
        AnimatedVisibility(
            visible = selectedCustomer != null,
            enter = expandVertically(animationSpec = tween(350)) + fadeIn(tween(350)),
            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(tween(250))
        ) {
            selectedCustomer?.let { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("الزبون: ${c.name}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "الدين الحالي: ${c.currentBalance().toInt()} جنيه",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text("المدفوع: ${c.totalPaid.toInt()} جنيه")
                    }
                }
            }
        }

        // عدد الوحدات — عدّاد متحرك (المعرض #97)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("عدد الأسطوانات:", modifier = Modifier.weight(1f))
            IconButton(onClick = { if (units > 1) units-- }) { Text("−") }
            AnimatedNumber(
                value = units,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { units++ }) { Text("+") }
        }

        // السعر
        OutlinedTextField(
            value = pricePerUnit.toString(),
            onValueChange = { pricePerUnit = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("سعر الوحدة (جنيه)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // حالة السداد — شرائح تتضخم عند الاختيار (المعرض #84)
        Text("حالة السداد:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChipScale(selected = status == "PAID") {
                FilterChip(
                    selected = status == "PAID",
                    onClick = { status = "PAID" },
                    label = { Text("✅ سدد") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            ChipScale(selected = status == "CREDIT") {
                FilterChip(
                    selected = status == "CREDIT",
                    onClick = { status = "CREDIT" },
                    label = { Text("⏳ بالأجل") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error,
                        selectedLabelColor = MaterialTheme.colorScheme.onError
                    )
                )
            }
        }

        // ملاحظات
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("ملاحظات (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        // رسالة خطأ — تظهر مع اهتزاز (المعرض #32)
        AnimatedVisibility(visible = showError, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Text(
                "⚠️ اختر زبوناً وأدخل سعراً صحيحاً قبل التأكيد",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // زر التأكيد — يغوص عند اللمس (المعرض #01)
        val total = units * pricePerUnit
        Button(
            onClick = {
                if (selectedCustomer == null || units <= 0 || pricePerUnit <= 0) {
                    shakeKey++
                    showError = true
                } else {
                    showError = false
                    val c = selectedCustomer!!
                    val amountPaid = if (status == "PAID") total else 0.0
                    // تحديث دين/مدفوعات الزبون تراكمياً: النقدي لا يترك ديناً
                    val updatedCustomer = c.copy(
                        totalDebt = c.totalDebt + total,
                        totalPaid = c.totalPaid + amountPaid
                    )
                    viewModel.dispenseAndRecord(
                        cylinderIds = listOf("DEMO"), // في التطبيق الكامل: اختيار من قائمة المتوفر
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
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (status == "PAID") MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        ) {
            Text("تأكيد البيع — إجمالي: ")
            AnimatedMoney(
                value = total,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }

    // شاشة النجاح الكاملة — المعرض #96
    FullScreenSuccess(
        visible = showSuccess,
        message = "✅ تم تسجيل البيع",
        subMessage = "تم تحديث المخزون ودين الزبون",
        onDismiss = { showSuccess = false; onSaleRecorded() }
    )
}

/** شريحة تتضخم برفق عند اختيارها (المعرض #84 — أيقونة تتحول وتكبر) */
@Composable
private fun ChipScale(selected: Boolean, content: @Composable () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "chipScale"
    )
    Box(modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }) {
        content()
    }
}
