package com.dabb.business.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.PaymentEntity
import com.dabb.business.model.SaleEntity
import com.dabb.business.model.SaleStatus
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.ui.viewmodel.CustomerDetail
import com.dabb.business.util.Money
import kotlinx.coroutines.launch

@Composable
fun CustomerDetailScreen(customerId: String, onBack: () -> Unit) {
    val viewModel: AppViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<CustomerDetail?>(null) }
    var showPay by remember { mutableStateOf(false) }
    var confirmCancelSale by remember { mutableStateOf<SaleEntity?>(null) }
    var confirmReversePayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var editCustomer by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    // إصلاح الفحص M1: خطأ العملية لم يعد يضيع — كان يُتجاهل بـ { _ -> }
    var opError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(customerId, refreshKey) {
        detail = viewModel.getCustomerDetail(customerId)
        if (refreshKey > 0) opError = null
    }

    val d = detail
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(
            title = d?.customer?.name ?: "الزبون",
            subtitle = "التفاصيل والتحصيل",
            icon = Icons.Filled.Payments,
            trailing = {
                Row {
                    IconButton(onClick = { editCustomer = true }) {
                        Icon(Icons.Filled.Edit, "تعديل", tint = Color.White)
                    }
                    TextButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "رجوع", tint = Color.White)
                        Text("رجوع", color = Color.White)
                    }
                }
            }
        )

        if (d == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // إصلاح الفحص M1: رسالة رفض العملية (مثل "توجد تحصيلات لاحقة")
            // كانت تُبلع صامتاً فيُظن المستخدم أن الإلغاء تم.
            opError?.let { msg ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { opError = null }) { Text("إغلاق") }
                }
            }
            StaggeredReveal(0) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        // إصلاح الخطأ 5: ‏> 0L — الدين = 1 ريال دينٌ فعلاً
                        containerColor = if (d.balance > 0L) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("الرصيد المتبقي", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedMoney(Money.piastersToPounds(d.balance),
                                style = MaterialTheme.typography.displaySmall,
                                color = if (d.balance > 0L) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary)
                            Text(" ريال", style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniStat("إجمالي المشتريات", d.totalBought, Modifier.weight(1f))
                            MiniStat("إجمالي المدفوع", d.totalPaid, Modifier.weight(1f))
                        }
                    }
                }
            }

            StaggeredReveal(1) {
                Button(
                    onClick = { showPay = true }, enabled = d.balance > 0L,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.Payments, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تحصيل دفعة من الزبون", style = MaterialTheme.typography.labelLarge)
                }
            }

            StaggeredReveal(2) {
                Text("المبيعات (${d.sales.size})", style = MaterialTheme.typography.titleMedium)
            }
            StaggeredReveal(3) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    if (d.sales.isEmpty()) EmptyHint("لا توجد مبيعات")
                    else d.sales.forEachIndexed { i, sale ->
                        SaleHistoryRow(sale) { confirmCancelSale = sale }
                        if (i < d.sales.lastIndex) DividerSoft()
                    }
                }
            }

            StaggeredReveal(4) {
                Text("دفعات التحصيل (${d.payments.size})", style = MaterialTheme.typography.titleMedium)
            }
            StaggeredReveal(5) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    if (d.payments.isEmpty()) EmptyHint("لم يدفع أي دفعة بعد")
                    else d.payments.forEachIndexed { i, p ->
                        PaymentRow(p) { confirmReversePayment = p }
                        if (i < d.payments.lastIndex) DividerSoft()
                    }
                }
            }

            StaggeredReveal(6) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp)); Text("حذف هذا الزبون")
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showPay && d != null) {
        PaymentDialog(
            title = "تحصيل دفعة", maxPiasters = d.balance, allowExceedMax = true,
            onDismiss = { showPay = false },
            onConfirm = { amountPiasters, note ->
                scope.launch {
                    viewModel.recordCustomerPayment(customerId, amountPiasters, note) { e ->
                        if (e == null) { showPay = false; refreshKey++ }
                    }
                }
            }
        )
    }

    if (editCustomer && d != null) {
        EditCustomerDialog(
            initialName = d.customer?.name ?: "",
            initialPhone = d.customer?.phone ?: "",
            onDismiss = { editCustomer = false },
                onSave = { name, phone ->
                    scope.launch {
                        // إصلاح الفحص M1: فشل التعديل (كان P0) لم يعد صامتاً
                        viewModel.updateCustomer(customerId, name, phone) { e ->
                            if (e == null) { editCustomer = false; refreshKey++ }
                            else opError = e
                        }
                    }
                }
        )
    }

    confirmCancelSale?.let { sale ->
        AlertDialog(
            onDismissRequest = { confirmCancelSale = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("إلغاء هذا البيع؟") },
            text = { Text("ستُرجَع ${sale.unitsSold} أسطوانة إلى المخزون وتُخصم قيمة البيع (${Money.format(sale.totalAmount)} ج) من دين/مدفوعات الزبون.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.cancelSale(sale.id) { err ->
                                confirmCancelSale = null
                                if (err != null) opError = err
                                refreshKey++
                            }
                        }
                    }) { Text("نعم، إلغاء البيع", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { confirmCancelSale = null }) { Text("تراجع") } }
        )
    }

    confirmReversePayment?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmReversePayment = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("عكس هذه الدفعة؟") },
            text = { Text("ستُعاد قيمة ${Money.format(p.amount)} ج كدَين على الزبون (لحالة تسجيل دفعة خاطئة).") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.reverseCustomerPayment(p.id) { err ->
                                confirmReversePayment = null
                                if (err != null) opError = err
                                refreshKey++
                            }
                        }
                    }) { Text("نعم، عكس الدفعة", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { confirmReversePayment = null }) { Text("تراجع") } }
        )
    }

    confirmDelete.let {
        if (confirmDelete) AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("حذف الزبون؟") },
            text = { Text("يُحذف الزبون نهائياً. لا يمكن الحذف إذا كانت له أي عملية بيع أو دفعة (تُلغى عملياته أولاً).") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.deleteCustomer(customerId) { e ->
                                confirmDelete = false
                                if (e == null) onBack()
                                else opError = e
                            }
                        }
                    }) { Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("تراجع") } }
        )
    }
}

@Composable
private fun MiniStat(label: String, valuePiasters: Long, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)).padding(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        AnimatedMoney(Money.piastersToPounds(valuePiasters),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SaleHistoryRow(sale: SaleEntity, onCancel: () -> Unit) {
    val paid = sale.status == SaleStatus.PAID
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(if (paid) Icons.Filled.CheckCircle else Icons.Filled.Schedule, null,
            tint = if (paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("${sale.unitsSold} أسطوانة · ${Money.format(sale.totalAmount)} ريال",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${if (paid) "سدد" else "بالأجل"} · ${timeAgoLocal(sale.saleDate)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Delete, "إلغاء البيع", tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PaymentRow(p: PaymentEntity, onReverse: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("دفعة تحصيل", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(timeAgoLocal(p.paymentDate), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("+${Money.format(p.amount)} ريال", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onReverse) {
            Icon(Icons.Filled.Delete, "عكس الدفعة", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EditCustomerDialog(initialName: String, initialPhone: String,
                               onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("تعديل بيانات الزبون") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("التلفون") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { err = "الاسم لا يمكن أن يكون فارغاً"; return@TextButton }
                onSave(name, phone)
            }) { Text("حفظ", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

internal fun timeAgoLocal(millis: Long): String = timeAgo(millis)
