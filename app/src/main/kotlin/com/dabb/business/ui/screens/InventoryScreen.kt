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
import com.dabb.business.ui.components.sharedAppViewModel
import com.dabb.business.model.SaleEntity
import com.dabb.business.model.SaleStatus
import com.dabb.business.ui.animation.AnimatedNumber
import com.dabb.business.ui.animation.AnimatedProgressBar
import com.dabb.business.ui.animation.BreathingIndicator
import com.dabb.business.ui.animation.GlowingButton
import com.dabb.business.ui.animation.LiquidFillGauge
import com.dabb.business.ui.animation.Motion
import com.dabb.business.ui.animation.SkeletonCard
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.errorFlash
import com.dabb.business.ui.animation.motionDuration
import com.dabb.business.ui.animation.motionLoopsAllowed
import com.dabb.business.ui.animation.safeFraction
import com.dabb.business.ui.animation.shakeEffect
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
    val viewModel: AppViewModel = sharedAppViewModel()
    val available = viewModel.availableCount
    val sold = viewModel.soldCount
    val totalUnits = available + sold
    val recent = viewModel.recentSales

    var showAdded by remember { mutableStateOf(false) }
    var showIntake by remember { mutableStateOf(false) }
    var intakeError by remember { mutableStateOf<String?>(null) }
    // العيب 18: الهيكل مرتبط بحالة تحميل حقيقية (dataLoaded في ViewModel)
    // لا بمؤقت 1500ms كان يومض شيمراً عند كل عودة للتبويب ولو جاهزة البيانات
    val nowTick = rememberNowTick()
    // §6.1: سهم اتجاه التغيّر عند أي تحديث حي لعدّاد المتوفر
    var lastAvailable by remember { mutableStateOf(available) }
    var deltaDir by remember { mutableStateOf(0) }
    LaunchedEffect(available) {
        if (available != lastAvailable) {
            deltaDir = if (available > lastAvailable) 1 else -1
            lastAvailable = available
            delay(700); deltaDir = 0
        }
    }

    if (showIntake) {
        StationIntakeDialog(
            error = intakeError,
            onDismiss = { showIntake = false },
            onConfirm = { units, cost, paidNow ->
                // العيب 14: النتيجة داخل الـ callback حصراً — الدالة تعيد Job
                // وتنفّذ في coroutine آخر، فقياس ok بعد الإطلاق مباشرة كان يقرأ
                // true دائماً ⇒ حوار يُغلق وشارة نجاح خضراء حتى عند الرفض
                viewModel.purchaseFromStation(units, cost, paidNow, "") { err ->
                    if (err == null) {
                        showIntake = false; showAdded = true
                        scope.launch { kotlinx.coroutines.delay(2200); showAdded = false }
                    } else intakeError = err
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
                    // §6.1: مقياس سائل حي — أسطوانة تمتلئ بموجتين بنسبة المتوفر
                    Row(Modifier.padding(18.dp)) {
                        LiquidFillGauge(
                            fraction = safeFraction(available, totalUnits),
                            colorLow = Color(0xFF2A6B5E),
                            colorHigh = Color(0xFF4ADE80),
                            bodyColor = Color.White,
                            label = "المتوفر"
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("إجمالي الأسطوانات المسجّلة", color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                AnimatedNumber(totalUnits, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                                Text(" أسطوانة", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.13f)).padding(10.dp)) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AnimatedNumber(available, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                            // سهم التغيّر الحي: أخضر للأعلى/أحمر للأسفل — وميض 700ms
                                            if (deltaDir != 0) {
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    if (deltaDir > 0) "▲" else "▼",
                                                    color = if (deltaDir > 0) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(Modifier.width(4.dp))
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
                        }
                    }
                }
            }

            // §6.1: شريط علوي ينزلق ثم يتقلّص لشارة صغيرة (تحوّل حجم لا اختفاء مفاجئ)
            AnimatedVisibility(
                visible = showAdded,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(motionDuration(Motion.SHEET_IN), easing = Motion.EaseOutQuint)) + fadeIn(tween(motionDuration(300))),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(motionDuration(220))) + fadeOut(tween(motionDuration(180)))
            ) {
                var compact by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(1400); compact = true }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clip(RoundedCornerShape(if (compact) 50.dp else 14.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(if (compact) 15.dp else 18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (compact) "أُضيفت للمخزون" else "تم إدخال الأسطوانات إلى المخزون من المحطة",
                            color = SuccessGreen,
                            style = if (compact) MaterialTheme.typography.labelMedium
                            else MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
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
                    // §6.1: تمايل لطيف للشاحنة كل بضع ثوانٍ — يشتد كلما انخفض المخزون
                    val nudge = remember { Animatable(0f) }
                    val loopsAllowed = motionLoopsAllowed()
                    LaunchedEffect(available) {
                        while (true) {
                            delay(if (available <= 2) 2000L else if (available <= 5) 3000L else 4000L)
                            if (loopsAllowed && available < 10) {
                                nudge.animateTo(6f, tween(120))
                                nudge.animateTo(-6f, tween(240))
                                nudge.animateTo(0f, tween(120))
                            }
                        }
                    }
                    OutlinedButton(onClick = { intakeError = null; showIntake = true },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.LocalShipping, null,
                            modifier = Modifier
                                .size(17.dp)
                                .graphicsLayer { rotationZ = nudge.value })
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
                if (recent.isEmpty() && !viewModel.dataLoaded) {
                    // §7.3: شيمر لأول جلب فقط — بعدها «فارغ فعلاً» يعرض رسالته مباشرة
                    SkeletonCard(3)
                } else Card(shape = RoundedCornerShape(18.dp),
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
                            SaleRow(sale, nowTick)
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
private fun SaleRow(sale: SaleEntity, now: Long) {
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
            Text("${sale.unitsSold} أسطوانة · ${if (paid) "سدد" else "بالأجل"} · ${timeAgo(sale.saleDate, now)}",
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

internal fun timeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    // العيب 22: طابع مستقبلي (ساعة الجهاز قُدّمت) لا يعرض «الآن» للأبد —
    // القيم السالبة تُحبس في صفر = «الآن» لحظتها فقط، والآنَ يُمرَّر من
    // مؤقّت الشاشة فتتحدث «منذ X دقيقة» كل دقيقة بدل أن تتقادم ساكنة
    val minutes = ((now - millis) / 60000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        minutes < 1440 -> "منذ ${minutes / 60} ساعة"
        else -> "منذ ${minutes / 1440} يوم"
    }
}

/**
 * نبضة زمن واحدة لكل شاشة (ليس لكل صف) — توقظ كل 60 ثانية فتُعيد حساب
 * «منذ X دقيقة» في كل الصفوف دفعة واحدة (العيب 22).
 */
@Composable
internal fun rememberNowTick(): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); now = System.currentTimeMillis() }
    }
    return now
}

/**
 * حوار سحب أسطوانات من المحطة (المورد) بالجملة — يدعم الآجل.
 * الكمية + تكلفة الوحدة + المدفوع فوراً؛ الباقي دَين للمحطة.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var shakeKey by remember { mutableIntStateOf(0) }
    var errField by remember { mutableStateOf(-1) }

    val units = unitsText.toIntOrNull() ?: 0
    val cost = Money.poundsToPiasters(costText)
    val paidNow = Money.poundsToPiasters(paidText)
    val total = units.toLong() * cost
    val shownError = err ?: error

    // §6.4: الشاحنة تدخل من الحافة وتتوقف في المنتصف عند فتح الورقة
    val truckX = remember { Animatable(-320f) }
    LaunchedEffect(Unit) {
        truckX.animateTo(0f, tween(motionDuration(560), easing = Motion.EaseOutQuint))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalShipping, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer { translationX = truckX.value }
                )
                Spacer(Modifier.width(10.dp))
                Text("سحب من المحطة", style = MaterialTheme.typography.titleLarge)
            }
            Text("تُضاف الأسطوانات للمخزون، والباقي عن المدفوع يُسجَّل ديناً للمحطة.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(unitsText, { unitsText = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text("عدد الأسطوانات") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeEffect(if (errField == 0) shakeKey else 0)
                    .errorFlash(if (errField == 0) shakeKey else 0))
            // إصلاح الفحص H3: حد 10 أرقام — بلا حد كان يُدخل Long.MAX وفساد مالي
            OutlinedTextField(costText, { costText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                label = { Text("تكلفة الوحدة (ريال)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeEffect(if (errField == 1) shakeKey else 0)
                    .errorFlash(if (errField == 1) shakeKey else 0))
            OutlinedTextField(paidText, { paidText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                label = { Text("المدفوع الآن (0 = آجل)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeEffect(if (errField == 2) shakeKey else 0)
                    .errorFlash(if (errField == 2) shakeKey else 0))
            Text("الإجمالي: ${Money.format(total)} ريال · دَين المحطة: ${Money.format((total - paidNow).coerceAtLeast(0L))} ريال",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
            if (shownError != null) Text(shownError!!, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                Spacer(Modifier.width(10.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        when {
                            units <= 0 -> { err = "أدخل عدداً صحيحاً"; shakeKey++; errField = 0 }
                            cost <= 0 -> { err = "أدخل تكلفة صحيحة"; shakeKey++; errField = 1 }
                            units > Money.MAX_UNITS -> { err = "العدد يتجاوز الحد المسموح (${Money.format(Money.MAX_UNITS.toLong())})"; shakeKey++; errField = 0 }
                            paidNow > total -> { err = "المدفوع أكبر من الإجمالي"; shakeKey++; errField = 2 }
                            else -> onConfirm(units, cost, paidNow)
                        }
                    },
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("تأكيد الإدخال", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
