package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
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
import kotlinx.coroutines.launch

@Composable
fun SalesHistoryScreen() {
    val viewModel: AppViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val sales = viewModel.allSales
    var confirmCancel by remember { mutableStateOf<SaleEntity?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) { viewModel.refreshRecent() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(title = "سجل المبيعات", subtitle = "كل العمليات (${sales.size})",
            icon = Icons.Filled.ReceiptLong)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StaggeredReveal(0) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    if (sales.isEmpty()) {
                        EmptyHint("لا توجد مبيعات بعد — سجّل أول عملية من تبويب «الصرف»")
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
            Spacer(Modifier.height(6.dp))
        }
    }

    confirmCancel?.let { sale ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("إلغاء هذا البيع؟") },
            text = { Text("ستُرجَع ${sale.unitsSold} أسطوانة إلى المخزون وتُصحَّح مبالغ الزبون «${sale.customerName}» (${Money.format(sale.totalAmount)} ج).") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.cancelSale(sale.id) { _ -> confirmCancel = null; refreshKey++ }
                    }
                }) { Text("نعم، إلغاء", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("تراجع") } }
        )
    }
}
