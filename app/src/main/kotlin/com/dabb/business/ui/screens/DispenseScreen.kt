package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.SaleEntity
import com.dabb.business.ui.viewmodel.AppViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * واجهة الصرف — اختيار الزبون، عدد الأسطوانات، السعر، حالة السداد (سدد / بالأجل)
 * التصميم: بطاقات واضحة، أزرار ملونة للتفريق بين "سدد" و"بالأجل"
 * مهارة دفاعية: لا يمكن صرف أكثر من المتاح — التحقق في DAO
 */
@Composable
fun DispenseScreen(
    onBack: () -> Unit,
    onSaleRecorded: () -> Unit
) {
    var customerQuery by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var units by remember { mutableStateOf(1) }
    var pricePerUnit by remember { mutableStateOf(25.0) }
    var status by remember { mutableStateOf("CREDIT") } // PAID أو CREDIT
    var notes by remember { mutableStateOf("") }
    val viewModel: AppViewModel = viewModel()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🔄 صرف أسطوانة", style = MaterialTheme.typography.headlineSmall)

        // اختيار الزبون
        OutlinedTextField(
            value = customerQuery,
            onValueChange = { customerQuery = it },
            label = { Text("بحث عن زبون (اسم أو تلفون)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // عرض الزبون المختار
        selectedCustomer?.let { c ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("الزبون: ${c.name}", style = MaterialTheme.typography.titleMedium)
                    Text("الدين الحالي: ${c.currentBalance()} جنيه", color = MaterialTheme.colorScheme.error)
                    Text("المدفوع: ${c.totalPaid}")
                }
            }
        }

        // عدد الوحدات
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("عدد الأسطوانات:", modifier = Modifier.weight(1f))
            IconButton(onClick = { if (units > 1) units-- }) { Text("−") }
            Text("$units", style = MaterialTheme.typography.titleMedium)
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

        // حالة السداد — أزرار واضحة ومريحة للعين
        Text("حالة السداد:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = status == "PAID",
                onClick = { status = "PAID" },
                label = { Text("✅ سدد") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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

        // ملاحظات
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("ملاحظات (اختياري)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        // زر التأكيد — مع تحقق دفاعي
        val total = units * pricePerUnit
        Button(
            onClick = {
                // التحقق الدفاعي ثم الحفظ
                if (selectedCustomer != null && units > 0) {
                    viewModel.dispenseAndRecord(
                        cylinderIds = listOf("DEMO"), // في التطبيق الكامل: اختيار من قائمة المتوفر
                        customer = com.dabb.business.model.CustomerEntity(
                            id = selectedCustomer!!.id,
                            name = selectedCustomer!!.name,
                            phone = selectedCustomer!!.phone,
                            totalDebt = selectedCustomer!!.totalDebt + (units * pricePerUnit - 0), // تحديث الدين تقريباً
                            totalPaid = selectedCustomer!!.totalPaid
                        ),
                        sale = com.dabb.business.model.SaleEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            customerId = selectedCustomer!!.id,
                            customerName = selectedCustomer!!.name,
                            cylinderIdsJson = listOf("DEMO").joinToString(","),
                            unitsSold = units,
                            pricePerUnit = pricePerUnit,
                            totalAmount = units * pricePerUnit,
                            amountPaid = if (status == "PAID") units * pricePerUnit else 0.0,
                            status = status,
                            saleDate = System.currentTimeMillis(),
                            notes = notes
                        )
                    )
                    onSaleRecorded()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCustomer != null && units > 0 && pricePerUnit > 0
        ) {
            Text("تأكيد البيع — إجمالي: ${total} جنيه")
        }
    }
}
