package com.dabb.business.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dabb.business.ui.components.sharedAppViewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.BreathingIndicator
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.theme.SuccessGreen
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.ui.viewmodel.ReportPeriod
import com.dabb.business.util.Money
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    onOpenCustomer: (String) -> Unit = {},
    onOpenCustomers: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val viewModel: AppViewModel = sharedAppViewModel()
    val available = viewModel.availableCount
    val sold = viewModel.soldCount
    val totalPaid = viewModel.totalPaid
    val totalCredit = viewModel.totalCredit
    val totalSales = viewModel.totalSales
    val totalCost = viewModel.totalCost
    val profit = viewModel.profit
    val stationDebt = viewModel.stationBalance
    // إصلاح الفحص M2: الدين والدونات «حالة حالية» (كل الفترات) —
    // فلترة الفترة كانت تُنتج «ديناً سالباً» عند تحصيل دين قديم.
    val allTimePaid = viewModel.allTimePaid
    val allTimeSales = viewModel.allTimeSales
    val debtors = viewModel.topDebtors
    val period = viewModel.reportPeriod
    var refreshing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(
            title = "التقارير", subtitle = "نظرة شاملة على المحل", icon = Icons.Filled.Refresh,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, "الإعدادات", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    TextButton(onClick = {
                        scope.launch { refreshing = true; viewModel.refreshAll(); delay(500); refreshing = false }
                    }) {
                        if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Filled.Refresh, "تحديث", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        )

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // فلترة الفترة الزمنية
            StaggeredReveal(0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { viewModel.setPeriod(p) },
                            label = { Text(p.label) }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Filled.Inventory2, MaterialTheme.colorScheme.primary,
                    { Text("$available", style = MaterialTheme.typography.titleLarge) }, "متوفر", Modifier.weight(1f))
                StatTile(Icons.Filled.TrendingUp, MaterialTheme.colorScheme.secondary,
                    { Text("$sold", style = MaterialTheme.typography.titleLarge) }, "مباع", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Filled.Payments, MaterialTheme.colorScheme.tertiary,
                    { MoneyTile(totalPaid) }, "المحصّل (${period.label})", Modifier.weight(1f))
                StatTile(Icons.Filled.Schedule, MaterialTheme.colorScheme.error,
                    { MoneyTile(totalCredit) }, "الدين المتبقّي (كامل)", Modifier.weight(1f), pulse = true)
            }

            // الربح ودَين المحطة
            StaggeredReveal(1) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MoneyLine("إجمالي المبيعات", totalSales)
                        MoneyLine("تكلفة الأسطوانات المباعة", totalCost)
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("الربح التقديري", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            AnimatedMoney(Money.piastersToPounds(profit),
                                style = MaterialTheme.typography.titleMedium, color = SuccessGreen)
                            Text(" ريال", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                        }
                        // إصلاح الفحص M6: > 0L — دَين 1 ريال يُعرض (كان يُخفى)
                        if (stationDebt > 0L) MoneyLine("دَين المحطة (المورد)", stationDebt,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            StaggeredReveal(2) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            paid = Money.piastersToPounds(allTimePaid),
                            credit = Money.piastersToPounds(totalCredit),
                            modifier = Modifier.size(104.dp)
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("التحصيل (كل الفترات)", style = MaterialTheme.typography.titleSmall)
                            LegendDot(MaterialTheme.colorScheme.primary, "مدفوع: ${Money.format(allTimePaid)} ريال")
                            LegendDot(MaterialTheme.colorScheme.error, "دين: ${Money.format(totalCredit)} ريال")
                            Text("من إجمالي مبيعات ${Money.format(allTimeSales)} ريال",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            StaggeredReveal(3) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("أعلى الزبائن ديناً", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("عرض الكل", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onOpenCustomers))
                }
                Spacer(Modifier.height(2.dp))
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    if (debtors.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("لا توجد ديون حالياً", style = MaterialTheme.typography.titleSmall)
                            Text("كل الزبائن سدّدوا — عمل رائع!", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        debtors.forEachIndexed { index, c ->
                            DebtorRow(c) { onOpenCustomer(c.id) }
                            if (index < debtors.lastIndex)
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
private fun MoneyTile(piasters: Long) {
    Row(verticalAlignment = Alignment.Bottom) {
        AnimatedMoney(Money.piastersToPounds(piasters),
            style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(" ريال", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp))
    }
}

@Composable
private fun MoneyLine(label: String, piasters: Long, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        AnimatedMoney(Money.piastersToPounds(piasters),
            style = MaterialTheme.typography.titleSmall, color = color)
        Text(" ريال", style = MaterialTheme.typography.labelSmall,
            color = if (color == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else color,
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun StatTile(icon: ImageVector, tint: Color, value: @Composable () -> Unit,
                     label: String, modifier: Modifier = Modifier, pulse: Boolean = false) {
    Card(modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                value()
                if (pulse) { Spacer(Modifier.width(5.dp)); BreathingIndicator(size = 7.dp, color = tint) }
            }
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DonutChart(paid: Double, credit: Double, modifier: Modifier = Modifier) {
    val total = paid + credit
    val paidFraction = if (total <= 0.0) 0f else (paid / total).toFloat().coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
    val paidColor = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round))
            if (paidFraction > 0f) {
                drawArc(color = paidColor, startAngle = -90f, sweepAngle = 360f * paidFraction,
                    useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(paidFraction * 100).toInt()}٪", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Text("محصَّل", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DebtorRow(c: CustomerEntity, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center) {
            Text(c.name.trim().take(1), color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(if (c.phone.isNotBlank()) c.phone else "بدون تلفون",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${Money.format(c.balancePiasters())} ريال", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Filled.MonetizationOn, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(15.dp))
    }
}
