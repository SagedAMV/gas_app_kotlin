package com.dabb.business.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dabb.business.ui.components.sharedAppViewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.ui.animation.BreathingIndicator
import com.dabb.business.ui.animation.MiniSpinner
import com.dabb.business.ui.animation.SegmentedLiquidToggle
import com.dabb.business.ui.animation.StaggerSpeed
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.theme.DangerRed
import com.dabb.business.ui.theme.SlateText
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.util.Money
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CustomersScreen(onOpenCustomer: (String) -> Unit) {
    val viewModel: AppViewModel = sharedAppViewModel()
    val scope = rememberCoroutineScope()
    val customers = viewModel.allCustomers
    var showNew by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshKey) { viewModel.refreshCustomers() }

    // العيب 20: بحث حي بالاسم/الهاتف (debounce 250ms — نفس نمط شاشة الصرف)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CustomerEntity>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.isBlank()) { results = emptyList(); searching = false }
        else {
            searching = true
            delay(250)   // كتابة متصلة تلغي السابقة تلقائياً (تغير المفتاح)
            results = viewModel.searchCustomers(query)
            searching = false
        }
    }

    // العيب 13: فلتر حقيقي — قائمة واحدة تُبنى من الحالة، لا قائمتان متطابقتان
    var filter by remember { mutableStateOf(0) }
    val shown = remember(customers, filter, query, results) {
        when {
            query.isNotBlank() -> results
            filter == 1 -> customers.filter { it.balancePiasters() > 0L }
                .sortedByDescending { it.balancePiasters() }
            else -> customers
        }
    }
    val debtorsCount = customers.count { it.balancePiasters() > 0L }
    val maxDebt = customers.filter { it.balancePiasters() > 0L }
        .maxOfOrNull { it.balancePiasters() } ?: 0L

    // العيب 6أ: LazyColumn — الصفوف تُركَّب عند ظهورها فقط (كانت كلها eagerly)
    Column(Modifier.fillMaxSize()) {
        AppHeader(title = "الزبائن", subtitle = "الدين والتحصيل", icon = Icons.Filled.Person)

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("بحث بالاسم أو رقم الهاتف") },
                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searching) MiniSpinner(size = 16.dp, color = MaterialTheme.colorScheme.primary)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            SegmentedLiquidToggle(
                options = listOf("الكل (${customers.size})", "المدينون ($debtorsCount)"),
                selectedIndex = filter,
                onSelect = { filter = it },
                accent = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            query.isNotBlank() -> "نتائج البحث (${shown.size})"
                            filter == 1 -> "الزبائن المدينون (${shown.size})"
                            else -> "كل الزبائن (${customers.size})"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.weight(1f))
                    // العيب 13: الزر ظاهر في الوضعين — كان يختفي في تبويب المدينين
                    TextButton(onClick = { showNew = true }) {
                        Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("زبون جديد")
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        EmptyHint(
                            when {
                                query.isNotBlank() -> "لا نتائج مطابقة لبحثك"
                                filter == 1 -> "لا يوجد دَين على أي زبون الآن"
                                else -> "لا يوجد زبائن بعد — أنشئ زبوناً أو سجّل أول بيع"
                            }
                        )
                    }
                }
            } else {
                // key يمنع إعادة تركيب خاطئة عند الفلترة/البحث — وid ثابت
                itemsIndexed(shown, key = { _, c -> c.id }) { i, c ->
                    // Stagger لأول 8 صفوف فقط — داخل Lazy لا يصح تأخير يتزايد مع فهرس التمرير
                    StaggeredReveal(minOf(i, 8), speed = StaggerSpeed.Fast) {
                        Card(shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            CustomerRow(c, maxDebt) { onOpenCustomer(c.id) }
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        NewCustomerDialog(
            onDismiss = { showNew = false },
            onCreate = { name, phone ->
                scope.launch { showNew = false; refreshKey++ }
            }
        )
    }
}

@Composable
private fun NewCustomerDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    val viewModel: AppViewModel = sharedAppViewModel()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("زبون جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("الاسم") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("التلفون (اختياري)") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                if (err != null) Text(err!!, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { err = "أدخل اسم الزبون"; return@TextButton }
                viewModel.createCustomer(name.trim(), phone.trim()) { e ->
                    if (e == null) { onCreate(name.trim(), phone.trim()) } else err = e
                }
            }) { Text("حفظ الزبون", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun CustomerRow(c: CustomerEntity, maxDebt: Long = 0L, onClick: () -> Unit) {
    val balance = c.balancePiasters()
    // إصلاح الفحص M6: > 0L — كان يُعرض زبون مدَّين بـ 1 ريال «مسدّد»
    val inDebt = balance > 0L
    // §6.3: لون مبلغ الدَّين ديناميكي — يشتد نحو الأحمر كلما اقترب من أعلى دَين
    val accent = if (inDebt) {
        val t = if (maxDebt > 0L) (balance.toFloat() / maxDebt.toFloat()).coerceIn(0.15f, 1f) else 1f
        lerp(SlateText, DangerRed, t)
    } else MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // §6.3: نبض خافت خلف حرف الزبون المدين فقط — تمييز بصري فوري بلا قراءة رقم
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            if (inDebt) BreathingIndicator(size = 38.dp, color = DangerRed.copy(alpha = 0.35f))
            Box(Modifier.size(33.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Text(c.name.trim().take(1), color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(if (c.phone.isNotBlank()) c.phone else "بدون تلفون",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (inDebt) {
                Text("${Money.format(balance)} ريال", style = MaterialTheme.typography.titleSmall, color = accent,
                    fontWeight = FontWeight.Bold)
                Text("دَين متبقٍ", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("مسدّد", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun DividerSoft() {
    HorizontalDivider(Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
internal fun EmptyHint(text: String) {
    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
