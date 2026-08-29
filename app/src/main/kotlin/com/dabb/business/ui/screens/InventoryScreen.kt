package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.dabb.business.data.local.AppDatabase
import com.dabb.business.ui.theme.AppTheme
import com.dabb.business.ui.animation.BreathingIndicator
import com.dabb.business.ui.animation.GlowingButton
import com.dabb.business.ui.viewmodel.AppViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

/**
 * واجهة المخزون — عرض عددي + زر إضافة + زر صرف
 * التصميم الحديث: Material 3 + بطاقات ناعمة + ألوان هادئة
 * المهارة: ㊹ (UI Pattern) + ㊶ (Clean) + ㉛ (إبداعي — اختيار ألوان مريحة)
 */
@Composable
fun InventoryScreen(onNavigateToDispense: () -> Unit, onNavigateToReports: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: AppViewModel = viewModel()

    // قاعدة البيانات — يُنشأ مرة واحدة (Singleton pattern داخل App)
    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "gas_db.sqlite").build()
    }

    var available by remember { mutableIntStateOf(0) }
    var sold by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        scope.launch {
            available = db.cylinderDao().getAvailableCount()
            sold = db.cylinderDao().getSoldCount()
        }
    }

    // دعم RTL تلقائي للعربية (المهارة ١٠٩ — التكيف مع أسلوب المستخدم)
    CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        AppTheme {
            Scaffold(
                topBar = { TopAppBar(title = { Text("📦 المخزون") }) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // بطاقة إحصائيات رئيسية
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("حالة المخزون", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StatBox(value = "$available", label = "متوفر")
                                StatBox(value = "$sold", label = "مباع")
                            }
                        }
                    }

                    // أزرار العمليات
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    viewModel.addCylinder(
                                        com.dabb.business.model.CylinderEntity(
                                            id = java.util.UUID.randomUUID().toString(),
                                            sizeLiters = 20,
                                            status = "AVAILABLE",
                                            acquiredFromStation = "محطة المورد",
                                            acquisitionCost = 12.5,
                                            acquiredDate = System.currentTimeMillis()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("➕ إضافة أسطوانة") }

                        FilledTonalButton(
                            onClick = onNavigateToDispense,
                            modifier = Modifier.weight(1f)
                        ) { Text("🔄 صرف / بيع") }

                        FilledTonalButton(
                            onClick = onNavigateToReports,
                            modifier = Modifier.weight(1f)
                        ) { Text("📊 تقارير") }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
