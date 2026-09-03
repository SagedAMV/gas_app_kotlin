package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.SaleEntity
import com.dabb.business.model.SaleStatus
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.util.Money
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * سجل المبيعات الكامل.
 * إصلاح المشكلة 12: بحث فوري بالاسم أو الملاحظات أو التاريخ.
 * إصلاح المشكلة 13: تحميل على صفحات (100 سجل) بدل الجدول كاملاً — «تحميل المزيد» عند الحاجة.
 */
@Composable
fun SalesHistoryScreen() {
    val viewModel: AppViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val pageSize = 100

    var sales by remember { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var canLoadMore by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var opError by remember { mutableStateOf<String?>(null) }
    var confirmCancel by remember { mutableStateOf<SaleEntity?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // إصلاح الفحص L13: البحث على مستوى القاعدة (اسم/ملاحظات) أو نطاق يومي كامل —
    // كان محصوراً في الصفحات المحمَّلة فقط فـ"لا نتائج" كانت مضللة.
    // التأخير 250ms يعمل debounce طبيعياً (أثران متتاليان يعيدان جدولة التاخير).
    LaunchedEffect(refreshKey, query) {
        if (query.isNotBlank()) delay(250)
        val q = query.trim()
        if (q.isEmpty()) {
            totalCount = viewModel.getSalesCount()
            val first = viewModel.getSalesPaged(pageSize, 0)
            sales = first
            canLoadMore = first.size == pageSize
        } else {
            val dayRange = parseSaleDay(q)
            sales = if (dayRange != null) viewModel.getSalesBetween(dayRange.first, dayRange.second)
                    else viewModel.searchSalesByNameOrNotes(q)
            totalCount = sales.size
            canLoadMore = false
        }
        if (refreshKey > 0) opError = null
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(title = "سجل المبيعات", subtitle = "كل العمليات ($totalCount)",
            icon = Icons.Filled.ReceiptLong)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("بحث بالاسم أو التاريخ (مثل 2026/9/2)") },
                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                singleLine = true, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            opError?.let { msg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            StaggeredReveal(0) {
                    Card(shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        if (sales.isEmpty()) {
                            EmptyHint(
                                if (query.isBlank()) "لا توجد مبيعات بعد — سجّل أول عملية من تبويب «الصرف»"
                                else "لا نتائج مطابقة لبحثك في قاعدة البيانات"
                            )
                        } else {
                            sales.forEachIndexed { i, sale ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("بيع لـ «${sale.customerName}»",
                                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text("${sale.unitsSold} أسطوانة · ${if (sale.status == SaleStatus.PAID) "سدد" else "بالأجل"} · ${timeAgoLocal(sale.saleDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${Money.format(sale.totalAmount)} ريال",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (sale.status == SaleStatus.PAID) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error)
                                IconButton(onClick = { confirmCancel = sale }) {
                                    Icon(Icons.Filled.Delete, "إلغاء البيع",
                                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (i < sales.lastIndex) DividerSoft()
                        }
                    }
                }
            }

            if (canLoadMore && query.isBlank()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val more = viewModel.getSalesPaged(pageSize, sales.size)
                            sales = sales + more
                            canLoadMore = more.size == pageSize
                        }
                    },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                ) { Text("تحميل المزيد (${sales.size}/$totalCount)") }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    confirmCancel?.let { sale ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("إلغاء هذا البيع؟") },
            text = { Text("ستُرجَع ${sale.unitsSold} أسطوانة إلى المخزون وتُصحَّح مبالغ الزبون «${sale.customerName}» (${Money.format(sale.totalAmount)} ريال).") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.cancelSale(sale.id) { err ->
                            confirmCancel = null
                            if (err != null) {
                                // رسالة الرفض (مثل: تحصيلات لاحقة) تظهر للمستخدم ولا تضيع
                                opError = err
                            }
                            refreshKey++
                        }
                    }
                }) { Text("نعم، إلغاء", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("تراجع") } }
        )
    }
}

/** يحلل التاريخ المكتوب (2026/9/2 أو 2026/09/02) إلى نطاق اليوم الكامل (من 00:00 حتى 24:00). */
private fun parseSaleDay(q: String): Pair<Long, Long>? {
    for (fmt in listOf("yyyy/M/d", "yyyy/MM/dd")) {
        val sdf = SimpleDateFormat(fmt, Locale.getDefault())
        sdf.isLenient = false
        val d = try { sdf.parse(q) } catch (e: Exception) { null } ?: continue
        val cal = Calendar.getInstance().apply {
            time = d
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val from = cal.timeInMillis
        return from to from + 24L * 3600 * 1000
    }
    return null
}
