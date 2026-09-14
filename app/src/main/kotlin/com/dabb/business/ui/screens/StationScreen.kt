package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dabb.business.ui.components.sharedAppViewModel
import com.dabb.business.model.StationPaymentEntity
import com.dabb.business.model.StationPurchaseEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.FullScreenSuccess
import com.dabb.business.ui.animation.LiquidFillGauge
import com.dabb.business.ui.animation.Motion
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.SuccessKind
import com.dabb.business.ui.animation.entryTilt
import com.dabb.business.ui.animation.motionDuration
import com.dabb.business.ui.animation.safeFraction
import androidx.compose.ui.graphics.Color
import com.dabb.business.ui.theme.DangerRed
import com.dabb.business.ui.theme.WarmAmber
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.ui.viewmodel.StationData
import com.dabb.business.util.Money
import kotlinx.coroutines.launch

@Composable
fun StationScreen() {
    val viewModel: AppViewModel = sharedAppViewModel()
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<StationData?>(null) }
    var showIntake by remember { mutableStateOf(false) }
    var showPay by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    // إصلاح المشكلة 11: إلغاء سحب/تسديد خاطئ
    var confirmCancelPurchase by remember { mutableStateOf<StationPurchaseEntity?>(null) }
    var confirmCancelPayment by remember { mutableStateOf<StationPaymentEntity?>(null) }
    var opError by remember { mutableStateOf<String?>(null) }
    var intakeError by remember { mutableStateOf<String?>(null) }
    // §5.3: نجاح متخصص — سحب من المحطة / تسديد لها
    var successKind by remember { mutableStateOf(SuccessKind.GENERIC) }
    var successAmount by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    // العيب 22: نبضة زمن موحدة لكل صفوف «منذ X» في الشاشة
    val nowTick = rememberNowTick()

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
                        // إصلاح الفحص M6: > 0L — دَين = 1 ريال دَينٌ حقيقي
                        containerColor = if (balance > 0L) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.padding(18.dp)) {
                        // §6.4: خزان يفرغ كلما سدّدت — كهرماني→أحمر مع ارتفاع الدَّين
                        val totalPurchases = d?.totalPurchases ?: 0L
                        LiquidFillGauge(
                            fraction = safeFraction(balance.toDouble(), totalPurchases.toDouble()),
                            reversed = true,
                            colorLow = WarmAmber,
                            colorHigh = DangerRed,
                            bodyColor = Color.White,
                            label = "الدَّين"
                        )
                        Spacer(Modifier.padding(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text("دَين المحطة المتبقي", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Row {
                                AnimatedMoney(Money.piastersToPounds(balance),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (balance > 0L) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary)
                                Text(" ريال", style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("إجمالي سحب: ${Money.format(totalPurchases)} ريال",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            StaggeredReveal(1) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { intakeError = null; showIntake = true }, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("سحب جديد", style = MaterialTheme.typography.labelMedium)
                    }
                    // إصلاح الفحص M6: كان معطلاً عند دَين = 1 ريال (المتبقي بعد التقريب)
                    OutlinedButton(onClick = { showPay = true }, enabled = balance > 0L,
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
                        PurchaseRow(p, now = nowTick, onCancel = { confirmCancelPurchase = p })
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
                        StationPaymentRow(p, now = nowTick, onCancel = { confirmCancelPayment = p })
                        if (i < payments.lastIndex && i < 29) DividerSoft()
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }

    if (showIntake) {
        // إصلاح الفحص M1: فشل السحب (مثل: تجاوز الحد/المدفوع أكبر) لم يعد صامتاً
        StationIntakeDialog(
            error = intakeError,
            onDismiss = { showIntake = false },
            onConfirm = { units, cost, paidNow ->
                scope.launch {
                    viewModel.purchaseFromStation(units, cost, paidNow, "") { e ->
                        if (e == null) {
                            showIntake = false; refreshKey++
                            successKind = SuccessKind.STATION_INTAKE
                            successAmount = Money.format(units.toLong() * cost)
                            showSuccess = true
                        } else intakeError = e
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
                        if (e == null) {
                            showPay = false; refreshKey++
                            successKind = SuccessKind.STATION_PAYMENT
                            successAmount = Money.format(amount)
                            showSuccess = true
                        } else opError = e   // العيب 21: الحوار يبقى مفتوحاً والخطأ يُعرض محلياً
                    }
                }
            }
        )
    }

    // §5.3: نجاح متخصص حسب العملية
    FullScreenSuccess(
        visible = showSuccess,
        message = if (successKind == SuccessKind.STATION_INTAKE) "تم سحب الأسطوانات" else "تم تسديد الدفعة",
        subMessage = if (successKind == SuccessKind.STATION_INTAKE) "أُضيفت للمخزون وسُجّل الدَّين"
        else "انخفض دَين المحطة",
        kind = successKind,
        amount = successAmount,
        onDismiss = { showSuccess = false }
    )

    opError?.let { msg ->
        // يُعرض داخل نافذة حوارية بسيطة — لا يفسد تخطيط الشاشة
        AlertDialog(
            onDismissRequest = { opError = null },
            shape = RoundedCornerShape(20.dp),
            icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تعذّر الإلغاء") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { opError = null }) { Text("حسناً") } }
        )
    }

    // إصلاح المشكلة 11: تأكيد إلغاء سحب من المحطة
    confirmCancelPurchase?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmCancelPurchase = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("إلغاء هذا السحب؟") },
            text = { Text("سيُحذف سجل السحب (${p.units} أسطوانة · ${Money.format(p.totalAmount)} ريال) وتُحذف أسطواناته من المخزون — يُسمح فقط إذا لم تُبَع أيٌّ منها.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.cancelStationPurchase(p.id) { err ->
                            confirmCancelPurchase = null
                            opError = err
                            refreshKey++
                        }
                    }
                }) { Text("نعم، إلغاء", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmCancelPurchase = null }) { Text("تراجع") } }
        )
    }

    // إصلاح المشكلة 11: تأكيد إلغاء تسديد للمحطة
    confirmCancelPayment?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmCancelPayment = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("إلغاء هذا التسديد؟") },
            text = { Text("سيعود المبلغ (${Money.format(p.amount)} ريال) ديناً على المحل للمحطة.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.cancelStationPayment(p.id) { err ->
                            confirmCancelPayment = null
                            opError = err
                            refreshKey++
                        }
                    }
                }) { Text("نعم، إلغاء", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmCancelPayment = null }) { Text("تراجع") } }
        )
    }
}

@Composable
private fun PurchaseRow(p: StationPurchaseEntity, now: Long = System.currentTimeMillis(), onCancel: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(Modifier.entryTilt()) {
            Icon(Icons.Filled.LocalShipping, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("سحب ${p.units} أسطوانة", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text("${timeAgoLocal(p.purchaseDate, now)} · دُفع ${Money.format(p.amountPaid)} ريال",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            Text("${Money.format(p.totalAmount)} ريال", style = MaterialTheme.typography.titleSmall)
            // إصلاح فحص 2026-09-14: > 0L — شرط «> 1L» كان يُخفي دَيناً قيمته 1 ريال
            // (نفس قاعدة الفحص M6 المطبقة في كل مواضع الدَّين الأخرى)
            if (p.totalAmount - p.amountPaid > 0L)
                Text("آجل ${Money.format(p.totalAmount - p.amountPaid)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Delete, "إلغاء السحب",
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StationPaymentRow(p: StationPaymentEntity, now: Long = System.currentTimeMillis(), onCancel: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(Modifier.entryTilt()) {
            Icon(Icons.Filled.Payments, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("سداد للمحطة", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(timeAgoLocal(p.paymentDate, now), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("−${Money.format(p.amount)} ريال", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Delete, "إلغاء التسديد",
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}
