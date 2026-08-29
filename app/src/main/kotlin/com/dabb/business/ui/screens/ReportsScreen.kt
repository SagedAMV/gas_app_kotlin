package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.dabb.business.data.local.AppDatabase

/**
 * واجهة التقارير — إجمالي المخزون، المباع، الدخل، الدين
 * التصميم: بطاقات ملونة بوضوح للتفريق السريع
 * مهارة: ㊹ (UI) + ㊿ (State) + ㊷ (SOLID — تقارير منفصلة عن المنطق)
 */
@Composable
fun ReportsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { Room.databaseBuilder(context, AppDatabase::class.java, "gas_db.sqlite").build() }

    var available by remember { mutableIntStateOf(0) }
    var sold by remember { mutableIntStateOf(0) }
    var totalPaid by remember { mutableDoubleStateOf(0.0) }
    var totalCredit by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(Unit) {
        available = db.cylinderDao().getAvailableCount()
        sold = db.cylinderDao().getSoldCount()
        totalPaid = db.saleDao().getTotalPaid() ?: 0.0
        totalCredit = db.saleDao().getTotalCredit() ?: 0.0
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📊 التقارير", style = MaterialTheme.typography.headlineSmall)

        ReportCard(title = "المخزون المتاح", value = "$available أسطوانة", color = MaterialTheme.colorScheme.primary)
        ReportCard(title = "المباع", value = "$sold أسطوانة", color = MaterialTheme.colorScheme.secondary)
        ReportCard(title = "إجمالي المدفوع", value = "$totalPaid جنيه", color = MaterialTheme.colorScheme.tertiary)
        ReportCard(title = "إجمالي الدين (بالآجل)", value = "$totalCredit جنيه", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun ReportCard(title: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
        }
    }
}
