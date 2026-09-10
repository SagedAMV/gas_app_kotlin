package com.dabb.business.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dabb.business.ui.components.sharedAppViewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.AnimatedProgressBar
import com.dabb.business.ui.animation.FullScreenSuccess
import com.dabb.business.ui.animation.IconPulseButton
import com.dabb.business.ui.animation.MiniSpinner
import com.dabb.business.ui.animation.Motion
import com.dabb.business.ui.animation.SegmentedLiquidToggle
import com.dabb.business.ui.animation.StaggerSpeed
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.SuccessKind
import com.dabb.business.ui.animation.errorFlash
import com.dabb.business.ui.animation.motionDuration
import com.dabb.business.ui.animation.safeFraction
import com.dabb.business.ui.animation.shakeEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.util.Money
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة الصرف — بيع بالكمية بخصم حقيقي للمخزون داخل معاملة واحدة،
 * فحص مخزون حي، وسعر افتراضي من الإعدادات.
 */
@Composable
fun DispenseScreen() {
    var customerQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var units by remember { mutableStateOf(1) }
    var priceText by remember { mutableStateOf<String?>(null) }
    var payNow by remember { mutableStateOf(false) }
    // إصلاح الفحص 4: دفعة جزئية وقت البيع (تظهر عند اختيار «بالأجل»).
    var partialText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var phoneForNew by remember { mutableStateOf("") }
    var shakeKey by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    // §6.2: نوع النجاح + نبضات العدّادات + حقل الخطأ الموضعي
    var successKind by remember { mutableStateOf(SuccessKind.GENERIC) }
    var successAmount by remember { mutableStateOf("") }
    var errorField by remember { mutableStateOf(-1) }
    val unitsPulse = remember { Animatable(1f) }
    val totalPulse = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    // نبضة العدّاد الميكانيكي عند كل تغيير كمية (§6.2.3)
    LaunchedEffect(units) {
        if (units > 1) {
            unitsPulse.snapTo(1f); unitsPulse.animateTo(1.14f, tween(55)); unitsPulse.animateTo(1f, tween(65))
        }
    }
    val viewModel: AppViewModel = sharedAppViewModel()
    val scope = rememberCoroutineScope()
    val available = viewModel.availableCount
    val effPrice = priceText ?: viewModel.defaultPricePiasters.toString()  // رقم مجرد (العيب 3)
    // إصلاح خطأ إضافي اكتُشف أثناء المشكلة 14: السعر الافتراضي المنسّق يحتوي فواصل
    // آلاف ("25,000") وtoDoubleOrNull لا يفهمها — فكان البيع يفشل دائماً دون لمس الحقل.
    val pricePiasters = Money.poundsToPiasters(effPrice)  // الفواصل تُنزَع مركزياً في Money (العيب 3)
    val totalPiasters = units.toLong() * pricePiasters

    // نبضة توسّع المجموع عند تغيّر الرقم فعلياً (§6.2.6) — نُقل بعد التعريف (خطأ ترجمة)
    LaunchedEffect(totalPiasters) {
        totalPulse.snapTo(1f); totalPulse.animateTo(1.06f, tween(90)); totalPulse.animateTo(1f, tween(110))
    }
    // إصلاح الفحص 4: المدفوع الآن = كامل المبلغ (سدد الآن) أو الدفعة الجزئية أو 0.
    val paidNowPiasters = if (payNow) totalPiasters else Money.poundsToPiasters(partialText)
    val stockShort = units > available

    LaunchedEffect(customerQuery) {
        if (customerQuery.isBlank()) { results = emptyList(); searching = false }
        else {
            searching = true; delay(250)
            results = viewModel.searchCustomers(customerQuery); searching = false
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1500)
            showSuccess = false
            customerQuery = ""; results = emptyList(); selectedCustomer = null
            phoneForNew = ""; units = 1; priceText = null; payNow = false; partialText = ""; notes = ""; errorText = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "صرف أسطوانة", subtitle = "تسجيل بيع جديد لزبون", icon = Icons.Filled.Person)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StaggeredReveal(0) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Inventory2, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("المتوفر في المخزون الآن:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text("$available", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(" أسطوانة", style = MaterialTheme.typography.labelMedium)
                }
            }

            StepHeader("١", "الزبون")
            OutlinedTextField(
                value = customerQuery,
                onValueChange = { customerQuery = it; if (selectedCustomer != null) selectedCustomer = null },
                label = { Text("بحث بالاسم أو التلفون") },
                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = { if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) },
                singleLine = true, shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeEffect(if (errorField == 0) shakeKey else 0)
                    .errorFlash(if (errorField == 0) shakeKey else 0)
            )
            // §6.2.2: بطاقة الزبون المختار تتمدد أعلى النموذج بدل الاستبدال الفوري
            AnimatedVisibility(
                visible = selectedCustomer != null,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + androidx.compose.animation.fadeIn(tween(motionDuration(220))),
                exit = shrinkVertically(tween(motionDuration(180))) + fadeOut(tween(150))
            ) {
                selectedCustomer?.let { c ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(c.name)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                Text(if (c.phone.isNotBlank()) c.phone else "بدون تلفون",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "دينه: ${Money.format(c.balancePiasters())} ريال",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (c.balancePiasters() > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (results.isNotEmpty()) {
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(6.dp)) {
                        results.take(4).forEachIndexed { index, c ->
                            StaggeredReveal(index, speed = StaggerSpeed.Fast) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { selectedCustomer = c; customerQuery = c.name }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(c.name)
                                    Spacer(Modifier.width(10.dp))
                                    Text(c.name, style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text("دينه: ${Money.format(c.balancePiasters())} ريال",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            if (customerQuery.trim().length >= 2 && results.isEmpty() && !searching && selectedCustomer == null) {
                AnimatedVisibility(visible = true,
                    enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it / 2 })) {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("زبون جديد: «${customerQuery.trim()}»",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            OutlinedTextField(
                                value = phoneForNew, onValueChange = { phoneForNew = it },
                                label = { Text("رقم التلفون (اختياري)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                            )
                            Button(onClick = {
                                selectedCustomer = CustomerEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    name = customerQuery.trim(), phone = phoneForNew.trim(),
                                    createdAt = System.currentTimeMillis()
                                )
                            }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp)); Text("اعتماد هذا الزبون")
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedCustomer != null,
                enter = expandVertically(tween(350)) + fadeIn(tween(350)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(250))
            ) {
                selectedCustomer?.let { c ->
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(c.name); Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("الدين الحالي: ${Money.format(c.balancePiasters())} ريال",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                            }
                            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            StepHeader("٢", "التفاصيل")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("عدد الأسطوانات", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .shakeEffect(if (errorField == 3) shakeKey else 0)
                            .errorFlash(if (errorField == 3) shakeKey else 0)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconPulseButton(
                            icon = Icons.Filled.Remove, contentDescription = "نقص",
                            onClick = { if (units > 1) units-- },
                            tint = MaterialTheme.colorScheme.primary, iconSize = 20
                        )
                        Text("$units", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = unitsPulse.value
                                    scaleY = unitsPulse.value
                                },
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconPulseButton(
                            icon = Icons.Filled.Add, contentDescription = "زيادة",
                            onClick = { if (units < available) units++ },
                            tint = MaterialTheme.colorScheme.primary, iconSize = 20
                        )
                    }
                    AnimatedVisibility(visible = stockShort) {
                        Text("⚠ المتوفر $available فقط", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("سعر الوحدة (ريال)", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = effPrice,
                        onValueChange = { v ->
                            // إصلاح المشكلة 14: أرقام فقط ونقطة عشرية واحدة كحد أقصى —
                            // "25..5" لم تعد ممكنة (كانت تُفسَّر صفراً بلا تنبيه).
                            // إصلاح الفحص H3: حد 11 خانة — بلا حد كان يُدخل Long.MAX وفساد مالي.
                            val filtered = v.filter { it.isDigit() || it == '.' }.take(11)
                            if (filtered.count { it == '.' } <= 1) priceText = filtered
                        },
                        singleLine = true, suffix = { Text("ريال", style = MaterialTheme.typography.labelMedium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // إصلاح الفحص 82: سعر 0 = منحة/عينة — مسموح ومُعلن بوضوح قبل التأكيد.
                    if (pricePiasters == 0L) Text(
                        "سعر 0 = منحة/عينة (بيع بلا مقابل)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            StepHeader("٣", "حالة السداد")
            // §6.2.4: مفتاح انزلاقي مزدوج بخلفية «بلوب» متحركة بدل تبديل اللون الفوري
            SegmentedLiquidToggle(
                options = listOf("سدد الآن", "بالأجل"),
                selectedIndex = if (payNow) 0 else 1,
                onSelect = { i ->
                    payNow = i == 0
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                accent = if (payNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            // إصلاح الفحص 4 + §6.2.5: حقل الدفعة الجزئية يتوسّع بنابض + شريط تقدم
            // بلون يتدرّج من كهرماني إلى أخضر كلما اقترب من السداد الكامل
            AnimatedVisibility(
                visible = !payNow,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + androidx.compose.animation.fadeIn(tween(motionDuration(200))),
                exit = shrinkVertically(tween(motionDuration(180))) + fadeOut(tween(150))
            ) {
                Column {
                    OutlinedTextField(
                        value = partialText,
                        onValueChange = { partialText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                        label = { Text("دفعة جزئية الآن (اختياري)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shakeEffect(if (errorField == 2) shakeKey else 0)
                            .errorFlash(if (errorField == 2) shakeKey else 0)
                    )
                    if (totalPiasters > 0 && paidNowPiasters > 0) {
                        Spacer(Modifier.height(6.dp))
                        AnimatedProgressBar(
                            progress = safeFraction(paidNowPiasters.toDouble(), totalPiasters.toDouble()),
                            color = MaterialTheme.colorScheme.primary,
                            dynamicColor = true,
                            height = 5.dp
                        )
                        Text(
                            "مدفوع الآن ${Money.format(paidNowPiasters)} من ${Money.format(totalPiasters)} ريال",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("ملاحظات (اختياري)") }, minLines = 2,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())

            AnimatedVisibility(visible = errorText != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(errorText ?: "", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الإجمالي", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Box(Modifier.graphicsLayer {
                            scaleX = totalPulse.value; scaleY = totalPulse.value
                        }) {
                            AnimatedMoney(value = Money.piastersToPounds(totalPiasters),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface, durationMillis = 300)
                        }
                        Text(" ريال", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        errorText = null
                        when {
                            selectedCustomer == null -> { errorText = "اختر الزبون أو أنشئه أولاً"; shakeKey++; errorField = 0 }
                            units <= 0 -> { errorText = "أدخل عدداً صحيحاً"; shakeKey++; errorField = 3 }
                            // (السعر 0 مسموح — منحة/عينة — ويظهر تنويه تحت حقل السعر — الفحص 82)
                            paidNowPiasters > totalPiasters -> { errorText = "الدفعة الجزئية أكبر من الإجمالي"; shakeKey++; errorField = 2 }
                            stockShort -> { errorText = "المخزون لا يكفي — المتوفر $available فقط"; shakeKey++; errorField = 3 }
                            else -> {
                                busy = true
                                scope.launch {
                                    // كشف تحفيزي وحيد: أول بيع في اليوم (§5.3)
                                    val firstToday = viewModel.isFirstSaleToday()
                                    viewModel.recordSale(selectedCustomer!!, units, pricePiasters, paidNowPiasters, notes) { err ->
                                        busy = false
                                        if (err == null) {
                                            successKind = when {
                                                firstToday -> SuccessKind.FIRST_SALE_TODAY
                                                payNow -> SuccessKind.SALE_CASH
                                                else -> SuccessKind.SALE_CREDIT
                                            }
                                            successAmount = Money.format(totalPiasters)
                                            showSuccess = true
                                        } else { errorText = err; shakeKey++; errorField = -1 }
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (payNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    AnimatedContent(
                        targetState = busy,
                        transitionSpec = {
                            (fadeIn(tween(motionDuration(120))) + scaleIn(
                                initialScale = 0.85f,
                                animationSpec = tween(motionDuration(120))
                            )) togetherWith fadeOut(tween(100))
                        },
                        label = "confirmBtn"
                    ) { isLoading ->
                        if (isLoading) MiniSpinner(size = 18.dp, color = Color.White)
                        else {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("تأكيد البيع", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    FullScreenSuccess(visible = showSuccess, message = "تم تسجيل البيع",
        subMessage = "تم خصم الأسطوانات من المخزون",
        kind = successKind, amount = successAmount,
        onDismiss = { showSuccess = false })
}

@Composable
private fun StepHeader(number: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center) {
            Text(number, color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun Avatar(name: String) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center) {
        Text(name.trim().take(1), color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}
