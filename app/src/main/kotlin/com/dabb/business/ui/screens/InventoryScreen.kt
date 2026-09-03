package com.dabb.business.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.SaleEntity
import com.dabb.business.model.SaleStatus
import com.dabb.business.ui.animation.AnimatedNumber
import com.dabb.business.ui.animation.AnimatedProgressBar
import com.dabb.business.ui.animation.BreathingIndicator
import com.dabb.business.ui.animation.GlowingButton
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.safeFraction
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.theme.SuccessGreen
import com.dabb.business.ui.theme.TealDeep
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.util.Money
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(
    onNavigateToDispense: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSales: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val viewModel: AppViewModel = viewModel()
    val available = viewModel.availableCount
    val sold = viewModel.soldCount
    val totalUnits = available + sold
    val recent = viewModel.recentSales

    var showAdded by remember { mutableStateOf(false) }
    var showIntake by remember { mutableStateOf(false) }
    var intakeError by remember { mutableStateOf<String?>(null) }

    if (showIntake) {
        StationIntakeDialog(
            error = intakeError,
            onDismiss = { showIntake = false },
            onConfirm = { units, cost, paidNow ->
                scope.launch {
                    var ok = true
                    viewModel.purchaseFromStation(units, cost, paidNow, "") { err ->
                        if (err != null) { ok = false; intakeError = err }
                    }
                    if (ok) {
                        showIntake = false; showAdded = true
                        kotlinx.coroutines.delay(2200); showAdded = false
                    }
                }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppHeader(title = "دبب البترول", subtitle = "إدارة مخزون الأسطوانات", icon = Icons.Filled.Notifications)

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StaggeredReveal(0) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                        .background(Brush.verticalGradient(listOf(TealDeep, MaterialTheme.colorScheme.primary)))
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("إجمالي الأسطوانات المسجّلة", color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedNumber(totalUnits, style = MaterialTheme.typography.displaySmall, color = Color.White)
                            Text(" أسطوانة", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.13f)).padding(10.dp)) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AnimatedNumber(available, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        BreathingIndicator(size = 8.dp, color = Color(0xFF4ADE80))
                                    }
                                    Text("متوفر الآن", color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.13f)).padding(10.dp)) {
                                Column {
                                    AnimatedNumber(sold, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                    Text("مباع", color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        AnimatedProgressBar(
                            progress = safeFraction(sold, totalUnits),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = Color.White.copy(alpha = 0.22f), height = 6.dp
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showAdded,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(tween(250))
            ) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SuccessGreen.copy(alpha = 0.12f)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("تم إدخال الأسطوانات إلى المخزون من المحطة", color = SuccessGreen,
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            StaggeredReveal(1) {
                GlowingButton(onClick = onNavigateToDispense, leadingIcon = Icons.Filled.ShoppingCart) {
                    Text("تسجيل بيع جديد", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            StaggeredReveal(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { intakeError = null; showIntake = true },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("سحب من المحطة", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    OutlinedButton(onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.BarChart, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("التقارير", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            StaggeredReveal(3) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("آخر الحركات", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("عرض الكل", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onNavigateToSales))
                }
                Spacer(Modifier.height(2.dp))
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    if (recent.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("لا توجد حركات بعد", style = MaterialTheme.typography.titleSmall)
                            Text("سجّل أول عملية بيع من تبويب «الصرف»", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        recent.forEachIndexed { index, sale ->
                            SaleRow(sale)
                            if (index < recent.lastIndex)
                                HorizontalDivider(Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SaleRow(sale: SaleEntity) {
    val paid = sale.status == SaleStatus.PAID
    val accent = if (paid) SuccessGreen else MaterialTheme.colorScheme.error
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Icon(if (paid) Icons.Filled.CheckCircle else Icons.Filled.Schedule, null,
                tint = accent, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("بيع لـ «${sale.customerName}»", style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${sale.unitsSold} أسطوانة · ${if (paid) "سدد" else "بالأجل"} · ${timeAgo(sale.saleDate)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (sale.notes.isNotBlank())
                Text("ملاحظة: ${sale.notes}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${Money.format(sale.totalAmount)} ريال", style = MaterialTheme.typography.titleSmall, color = accent)
            Text(if (paid) "مدفوع" else "متبقّي", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun timeAgo(millis: Long): String {
    val minutes = (System.currentTimeMillis() - millis) / 60000
    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        minutes < 1440 -> "منذ ${minutes / 60} ساعة"
        else -> "منذ ${minutes / 1440} يوم"
    }
}

/**
 * حوار سحب أسطوانات من المحطة (المورد) بالجملة — يدعم الآجل.
 * الكمية + تكلفة الوحدة + المدفوع فوراً؛ الباقي دَين للمحطة.
 */
@Composable
fun StationIntakeDialog(
    // إصلاح الفحص M1: خطأ العملية من ViewModel (كان يُبلع صامتاً في كلا الموضعين)
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (units: Int, costPiasters: Long, paidNowPiasters: Long) -> Unit
) {
    var unitsText by remember { mutableStateOf("10") }
    var costText by remember { mutableStateOf("") }
    var paidText by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    val units = unitsText.toIntOrNull() ?: 0
    val cost = Money.poundsToPiasters(costText)
    val paidNow = Money.poundsToPiasters(paidText)
    val total = units.toLong() * cost
    val shownError = err ?: error

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("سحب من المحطة", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تُضاف الأسطوانات للمخزون، والباقي عن المدفوع يُسجَّل ديناً للمحطة.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(unitsText, { unitsText = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("عدد الأسطوانات") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                // إصلاح الفحص H3: حد 10 أرقام — بلا حد كان يُدخل Long.MAX وفساد مالي
                OutlinedTextField(costText, { costText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                    label = { Text("تكلفة الوحدة (ريال)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(paidText, { paidText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                    label = { Text("المدفوع الآن (0 = آجل)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Text("الإجمالي: ${Money.format(total)} ريال · دَين المحطة: ${Money.format((total - paidNow).coerceAtLeast(0L))} ريال",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
                if (shownError != null) Text(shownError!!, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    units <= 0 || cost <= 0 -> err = "أدخل عدداً وتكلفة صحيحين"
                    units > Money.MAX_UNITS -> err = "العدد يتجاوز الحد المسموح (${Money.format(Money.MAX_UNITS.toLong())})"
                    paidNow > total -> err = "المدفوع أكبر من الإجمالي"
                    else -> onConfirm(units, cost, paidNow)
                }
            }) { Text("تأكيد الإدخال", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
