package com.dabb.business.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.CylinderEntity
import com.dabb.business.model.SaleEntity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة المخزون — التصميم الاحترافي الجديد:
 * - شريط علوي متدرّج بهوية العلامة (AppHeader)
 * - بطاقة Hero: إجمالي المخزون + متوفر/مباع + شريط نسبة مباع
 * - زر أساسي واحد بارز (تسجيل بيع جديد) + أزرار ثانوية
 * - سجل «آخر الحركات» بيانات حقيقية من قاعدة البيانات
 * الأنميشنات: #97 عدّادات، #63 ظهور متتابع، #16 نبض متوفر، #13 شارة الإضافة
 */
@Composable
fun InventoryScreen(onNavigateToDispense: () -> Unit, onNavigateToReports: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewModel: AppViewModel = viewModel()
    val available = viewModel.availableCount
    val sold = viewModel.soldCount
    val totalUnits = available + sold
    val recent = viewModel.recentSales

    var showAdded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppHeader(
            title = "دبب البترول",
            subtitle = "إدارة مخزون الأسطوانات",
            icon = Icons.Filled.Notifications
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== بطاقة Hero (أهم عنصر في الشاشة) =====
            StaggeredReveal(index = 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(listOf(TealDeep, MaterialTheme.colorScheme.primary))
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "إجمالي الأسطوانات المسجّلة",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedNumber(
                                value = totalUnits,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                durationMillis = 800
                            )
                            Text(
                                " أسطوانة",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // متوفر + نبض أخضر (المعرض #16)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.13f))
                                    .padding(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AnimatedNumber(
                                            value = available,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        BreathingIndicator(size = 8.dp, color = Color(0xFF4ADE80))
                                    }
                                    Text(
                                        "متوفر الآن",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.13f))
                                    .padding(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    AnimatedNumber(
                                        value = sold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        "مباع",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // نسبة المباع (المعرض #62)
                        AnimatedProgressBar(
                            progress = safeFraction(sold, totalUnits),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = Color.White.copy(alpha = 0.22f),
                            height = 6.dp
                        )
                    }
                }
            }

            // شارة «تمت الإضافة» — تنزلق (المعرض #13)
            AnimatedVisibility(
                visible = showAdded,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(tween(250))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "تمت إضافة أسطوانة جديدة إلى المخزون",
                            color = SuccessGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ===== زر أساسي بارز (المعرض #04/#01) =====
            StaggeredReveal(index = 1) {
                GlowingButton(
                    onClick = onNavigateToDispense,
                    leadingIcon = Icons.Filled.ShoppingCart
                ) {
                    Text("تسجيل بيع جديد", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            // ===== أزرار ثانوية =====
            StaggeredReveal(index = 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.addCylinder(
                                    CylinderEntity(
                                        id = java.util.UUID.randomUUID().toString(),
                                        sizeLiters = 20,
                                        status = "AVAILABLE",
                                        acquiredFromStation = "محطة المورد",
                                        acquisitionCost = 12.5,
                                        acquiredDate = System.currentTimeMillis()
                                    )
                                )
                                showAdded = true
                                delay(2200)
                                showAdded = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة أسطوانة", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التقارير", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            // ===== آخر الحركات (بيانات حقيقية) =====
            StaggeredReveal(index = 3) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("آخر الحركات", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "اليوم",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (recent.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("لا توجد حركات بعد", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "سجّل أول عملية بيع من تبويب «الصرف»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        recent.forEachIndexed { index, sale ->
                            SaleRow(sale)
                            if (index < recent.lastIndex) {
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

/** صف حركة واحدة في السجل */
@Composable
private fun SaleRow(sale: SaleEntity) {
    val paid = sale.status == "PAID"
    val accent = if (paid) SuccessGreen else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (paid) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "بيع لـ «${sale.customerName}»",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                "${sale.unitsSold} أسطوانة · ${if (paid) "سدد" else "بالأجل"} · ${timeAgo(sale.saleDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${sale.totalAmount.toInt()} ج",
                style = MaterialTheme.typography.titleSmall,
                color = accent
            )
            Text(
                if (paid) "مدفوع" else "متبقّي",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** تحويل الطابع الزمني إلى «منذ…» بالعربية */
private fun timeAgo(millis: Long): String {
    val minutes = (System.currentTimeMillis() - millis) / 60000
    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        minutes < 1440 -> "منذ ${minutes / 60} ساعة"
        else -> "منذ ${minutes / 1440} يوم"
    }
}
