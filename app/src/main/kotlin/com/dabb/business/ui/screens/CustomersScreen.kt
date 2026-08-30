package com.dabb.business.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.CustomerEntity
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.components.AppHeader
import com.dabb.business.ui.viewmodel.AppViewModel
import com.dabb.business.util.Money
import kotlinx.coroutines.launch

@Composable
fun CustomersScreen(onOpenCustomer: (String) -> Unit) {
    val viewModel: AppViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val customers = viewModel.allCustomers
    var showNew by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshKey) { viewModel.refreshCustomers() }

    val debtors = remember(customers) {
        customers.filter { it.balancePiasters() > 1L }
            .sortedByDescending { it.balancePiasters() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AppHeader(title = "الزبائن", subtitle = "الدين والتحصيل", icon = Icons.Filled.Person)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StaggeredReveal(0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("الزبائن المدينون", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${debtors.size}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error)
                }
            }

            StaggeredReveal(1) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    if (debtors.isEmpty()) EmptyHint("لا يوجد دَين على أي زبون الآن")
                    else debtors.forEachIndexed { i, c ->
                        CustomerRow(c) { onOpenCustomer(c.id) }
                        if (i < debtors.lastIndex) DividerSoft()
                    }
                }
            }

            StaggeredReveal(2) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("كل الزبائن (${customers.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showNew = true }) {
                        Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("زبون جديد")
                    }
                }
            }
            StaggeredReveal(3) {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    if (customers.isEmpty()) EmptyHint("لا يوجد زبائن بعد — أنشئ زبوناً أو سجّل أول بيع")
                    else customers.forEachIndexed { i, c ->
                        CustomerRow(c) { onOpenCustomer(c.id) }
                        if (i < customers.lastIndex) DividerSoft()
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
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
    val viewModel: AppViewModel = viewModel()
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
private fun CustomerRow(c: CustomerEntity, onClick: () -> Unit) {
    val balance = c.balancePiasters()
    val inDebt = balance > 1L
    val accent = if (inDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
            Text(c.name.trim().take(1), color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.ExtraBold)
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
