package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.StationPaymentEntity
import com.dabb.business.model.StationPurchaseEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.ui.viewmodel.StationData
import com.dabb.business.util.Money
import kotlinx.coroutines.launch

@Composable
fun StationScreen() {
    val viewModel: AppViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<StationData?>(null) }
    var showIntake by remember { mutableStateOf(false) }
    var showPay by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) { data = viewModel.getStationData() }
    val d = data
    val balance = d?.balance ?: 0L

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(title = "المحطة (المورد)", subtitle = "السحب بالآجل والتسديد",
            icon = Icons.Filled.LocalGasStation)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StaggeredReveal(0) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (balance > 1L) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("دَين المحطة المتبقي", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            AnimatedMoney(Money.piastersToPounds(balance),
                                style = MaterialTheme.typography.displaySmall,
                                color = if (balance > 1L) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary)
                            Text(" ريال", style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("إجمالي سحب: ${Money.format(d?.totalPurchases ?: 0L)} ريال",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            StaggeredReveal(1) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { showIntake = true }, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("سحب جديد", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = { showPay = true }, enabled = balance > 1L,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.Payments, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("سداد دفعة", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            StaggeredReveal(2) { Text("سجل السحوبات", style = MaterialTheme.typography.titleMedium) }
            StaggeredReveal(3) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    val purchases = d?.purchases ?: emptyList()
                    if (purchases.isEmpty()) EmptyHint("لا يوجد سحب من المحطة بعد")
                    else purchases.take(30).forEachIndexed { i, p ->
                        PurchaseRow(p)
                        if (i < purchases.lastIndex && i < 29) DividerSoft()
                    }
                }
            }

            StaggeredReveal(4) { Text("سجل تسديدات المحطة", style = MaterialTheme.typography.titleMedium) }
            StaggeredReveal(5) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    val payments = d?.payments ?: emptyList()
                    if (payments.isEmpty()) EmptyHint("لم تُسدّد أي دفعة للمحطة بعد")
                    else payments.take(30).forEachIndexed { i, p ->
                        StationPaymentRow(p)
                        if (i < payments.lastIndex && i < 29) DividerSoft()
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showIntake) {
        StationIntakeDialog(
            onDismiss = { showIntake = false },
            onConfirm = { units, cost, paidNow ->
                scope.launch {
                    viewModel.purchaseFromStation(units, cost, paidNow, "") { e ->
                        if (e == null) { showIntake = false; refreshKey++ }
                    }
                }
            }
        )
    }
    if (showPay) {
        PaymentDialog(
            title = "سداد دفعة للمحطة", maxPiasters = balance,
            onDismiss = { showPay = false },
            onConfirm = { amount, note ->
                scope.launch {
                    viewModel.payStation(amount, note) { e ->
                        if (e == null) { showPay = false; refreshKey++ }
                    }
                }
            }
        )
    }
}

@Composable
private fun PurchaseRow(p: StationPurchaseEntity) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(Icons.Filled.LocalShipping, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("سحب ${p.units} أسطوانة", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text("${timeAgoLocal(p.purchaseDate)} · دُفع ${Money.format(p.amountPaid)} ريال",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text("${Money.format(p.totalAmount)} ريال", style = MaterialTheme.typography.titleSmall)
            if (p.totalAmount - p.amountPaid > 1L)
                Text("آجل ${Money.format(p.totalAmount - p.amountPaid)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StationPaymentRow(p: StationPaymentEntity) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("سداد للمحطة", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(timeAgoLocal(p.paymentDate), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("−${Money.format(p.amount)} ريال", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
    }
}
