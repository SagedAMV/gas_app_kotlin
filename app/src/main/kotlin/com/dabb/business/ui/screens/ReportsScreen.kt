package com.dabb.business.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.AnimatedNumber
import com.dabb.business.ui.animation.AnimatedProgressBar
import com.dabb.business.ui.animation.BreathingIndicator
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.safeFraction
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.theme.SuccessGreen
import com.dabb.business.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة التقارير — التصميم الاحترافي الجديد:
 * - شبكة 2×2 إحصاءات بأيقونات ملونة
 * - رسم دائري (Donut) لنسبة التحصيل
 * - شريط نسبة المباع + قائمة أعلى المدينين (بيانات حقيقية)
 * الأنميشنات: #97 عدّادات، #62 أشرطة، #16 نبض الدين، #57 زر التحديث
 */
@Composable
fun ReportsScreen() {
    val scope = rememberCoroutineScope()
    val viewModel: AppViewModel = viewModel()
    val available = viewModel.availableCount
    val sold = viewModel.soldCount
    val totalPaid = viewModel.totalPaid
    val totalCredit = viewModel.totalCredit
    val debtors = viewModel.topDebtors
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshInventory()
        viewModel.refreshStats()
        viewModel.refreshDebtors()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader(
            title = "التقارير",
            subtitle = "نظرة شاملة على المحل",
            icon = Icons.Filled.Refresh,
            trailing = {
                // زر تحديث — يتحول إلى دائرة أثناء العمل (المعرض #57)
                TextButton(
                    onClick = {
                        scope.launch {
                            refreshing = true
                            viewModel.refreshInventory()
                            viewModel.refreshStats()
                            viewModel.refreshRecent()
                            viewModel.refreshDebtors()
                            delay(600)
                            refreshing = false
                        }
                    }
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "تحديث", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== شبكة الإحصاءات 2×2 =====
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    icon = Icons.Filled.Inventory2,
                    tint = MaterialTheme.colorScheme.primary,
                    value = { AnimatedNumber(available, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
                    label = "متوفر",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Filled.TrendingUp,
                    tint = MaterialTheme.colorScheme.secondary,
                    value = { AnimatedNumber(sold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
                    label = "مباع",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    icon = Icons.Filled.Payments,
                    tint = MaterialTheme.colorScheme.tertiary,
                    value = {
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedMoney(totalPaid, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(" ج", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    },
                    label = "المدفوع",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.Filled.Schedule,
                    tint = MaterialTheme.colorScheme.error,
                    value = {
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedMoney(totalCredit, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(" ج", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    },
                    label = "الدين المتبقّي",
                    modifier = Modifier.weight(1f),
                    pulse = true
                )
            }

            // ===== رسم دائري: نسبة التحصيل =====
            StaggeredReveal(index = 1) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            paid = totalPaid,
                            credit = totalCredit,
                            modifier = Modifier.size(104.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("التحصيل", style = MaterialTheme.typography.titleSmall)
                            LegendDot(color = MaterialTheme.colorScheme.primary, text = "مدفوع: ${totalPaid.toInt()} ج")
                            LegendDot(color = MaterialTheme.colorScheme.error, text = "دين: ${totalCredit.toInt()} ج")
                            Text(
                                "من إجمالي مبيعات ${(totalPaid + totalCredit).toInt()} ج",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ===== شريط نسبة المباع (المعرض #62) =====
            StaggeredReveal(index = 2) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row {
                            Text(
                                "نسبة الأسطوانات المباعة",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${if (available + sold == 0) 0 else sold * 100 / (available + sold)}٪",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AnimatedProgressBar(
                            progress = safeFraction(sold, available + sold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "$sold مباعة من أصل ${available + sold}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ===== أعلى الزبائن ديناً =====
            StaggeredReveal(index = 3) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("أعلى الزبائن ديناً", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    BreathingIndicator(size = 8.dp, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "يحتاجون متابعة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (debtors.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("لا توجد ديون حالياً 🎉".replace(" 🎉", ""), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "كل الزبائن سدّدوا — عمل رائع!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        debtors.forEachIndexed { index, c ->
                            DebtorRow(c)
                            if (index < debtors.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/** مربع إحصائية صغير بأيقونة ملونة */
@Composable
private fun StatTile(
    icon: ImageVector,
    tint: Color,
    value: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    pulse: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                value()
                if (pulse) {
                    Spacer(modifier = Modifier.width(5.dp))
                    BreathingIndicator(size = 7.dp, color = tint)
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** رسم دائري لنسبة التحصيل — مرسوم بـ Canvas */
@Composable
private fun DonutChart(paid: Double, credit: Double, modifier: Modifier = Modifier) {
    val total = paid + credit
    val paidFraction = if (total <= 0.0) 0f else (paid / total).toFloat().coerceIn(0f, 1f)
    // ألوان مُقرأة خارج نطاق الرسم (لا يمكن قراءة الثيم داخل Canvas)
    val trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
    val paidColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            // المسار الخلفي
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // الجزء المدفوع
            if (paidFraction > 0f) {
                drawArc(
                    color = paidColor,
                    startAngle = -90f,
                    sweepAngle = 360f * paidFraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(paidFraction * 100).toInt()}٪",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "محصَّل",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** نقطة لون في وسيلة الإيضاح */
@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** صف زبون مدين */
@Composable
private fun DebtorRow(c: CustomerEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                c.name.trim().take(1),
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (c.phone.isNotBlank()) c.phone else "بدون تلفون",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${c.currentBalance().toInt()} ج",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            Icons.Filled.MonetizationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.size(15.dp)
        )
    }
}
