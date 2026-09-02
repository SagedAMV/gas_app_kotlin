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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.FullScreenSuccess
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.shakeEffect
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
    var notes by remember { mutableStateOf("") }
    var phoneForNew by remember { mutableStateOf("") }
    var shakeKey by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val viewModel: AppViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val available = viewModel.availableCount
    val effPrice = priceText ?: Money.format(viewModel.defaultPricePiasters)
    // إصلاح خطأ إضافي اكتُشف أثناء المشكلة 14: السعر الافتراضي المنسّق يحتوي فواصل
    // آلاف ("25,000") وtoDoubleOrNull لا يفهمها — فكان البيع يفشل دائماً دون لمس الحقل.
    val pricePiasters = Money.poundsToPiasters(effPrice.replace(",", ""))
    val totalPiasters = units.toLong() * pricePiasters
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
            phoneForNew = ""; units = 1; priceText = null; payNow = false; notes = ""; errorText = null
        }
    }

    Column(Modifier.fillMaxSize().shakeEffect(shakeKey)) {
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
                singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )

            if (results.isNotEmpty()) {
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(6.dp)) {
                        results.take(4).forEachIndexed { index, c ->
                            StaggeredReveal(index) {
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
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (units > 1) units-- }) {
                            Icon(Icons.Filled.Remove, "نقص", tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("$units", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        IconButton(onClick = { units++ }) {
                            Icon(Icons.Filled.Add, "زيادة", tint = MaterialTheme.colorScheme.primary)
                        }
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
                            val filtered = v.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) priceText = filtered
                        },
                        singleLine = true, suffix = { Text("ريال", style = MaterialTheme.typography.labelMedium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            StepHeader("٣", "حالة السداد")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(4.dp)
            ) {
                SegOption("سدد الآن", payNow, MaterialTheme.colorScheme.primary, Icons.Filled.CheckCircle,
                    { payNow = true }, Modifier.weight(1f))
                SegOption("بالأجل", !payNow, MaterialTheme.colorScheme.error, Icons.Filled.Schedule,
                    { payNow = false }, Modifier.weight(1f))
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
                        AnimatedMoney(value = Money.piastersToPounds(totalPiasters),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface, durationMillis = 300)
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
                            selectedCustomer == null -> { errorText = "اختر الزبون أو أنشئه أولاً"; shakeKey++ }
                            units <= 0 || pricePiasters <= 0 -> { errorText = "أدخل عدداً وسعراً صحيحين"; shakeKey++ }
                            stockShort -> { errorText = "المخزون لا يكفي — المتوفر $available فقط"; shakeKey++ }
                            else -> {
                                busy = true
                                scope.launch {
                                    viewModel.recordSale(selectedCustomer!!, units, pricePiasters, payNow, notes) { err ->
                                        busy = false
                                        if (err == null) showSuccess = true
                                        else { errorText = err; shakeKey++ }
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
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("تأكيد البيع", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    FullScreenSuccess(visible = showSuccess, message = "تم تسجيل البيع",
        subMessage = "تم خصم الأسطوانات من المخزون", onDismiss = { showSuccess = false })
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

@Composable
private fun SegOption(label: String, selected: Boolean, accent: Color,
                       icon: androidx.compose.ui.graphics.vector.ImageVector,
                       onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) accent else Color.Transparent, tween(220), label = "bg")
    val fg by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, tween(220), label = "fg")
    Row(modifier.clip(RoundedCornerShape(11.dp)).background(bg).clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
