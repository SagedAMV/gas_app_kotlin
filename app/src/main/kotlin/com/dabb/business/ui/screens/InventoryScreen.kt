package com.dabb.business.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.model.CylinderEntity
import com.dabb.business.ui.animation.AnimatedStat
import com.dabb.business.ui.animation.GlowingButton
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.theme.AppTheme
import com.dabb.business.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * واجهة المخزون — عرض عددي + زر إضافة + زر صرف
 * الأنميشنات المدمجة من المعرض:
 * - #04/#01: زر متوهّج يغوص عند اللمس
 * - #97: العدّادات تعدّ تصاعدياً عند تغيّر الرقم
 * - #16: مؤشر نبض أخضر بجانب «متوفر»
 * - #63: ظهور متتابع للبطاقات
 * - #13: شارة «تمت الإضافة» تنزلق من الأسفل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(onNavigateToDispense: () -> Unit, onNavigateToReports: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewModel: AppViewModel = viewModel()
    val available = viewModel.availableCount
    val sold = viewModel.soldCount

    var showAdded by remember { mutableStateOf(false) }

    // دعم RTL تلقائي للعربية
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    // بطاقة إحصائيات رئيسية — ظهور متتابع (المعرض #63)
                    StaggeredReveal(index = 0) {
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
                                    // متوفر مع نبض أخضر (المعرض #16)
                                    AnimatedStat(
                                        value = available,
                                        label = "متوفر",
                                        valueColor = MaterialTheme.colorScheme.primary,
                                        pulse = true
                                    )
                                    AnimatedStat(
                                        value = sold,
                                        label = "مباع",
                                        valueColor = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    // شارة «تمت الإضافة» — تنزلق من الأسفل (المعرض #13)
                    AnimatedVisibility(
                        visible = showAdded,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(tween(300)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(tween(250))
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = ColorSuccess.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "✓ تمت إضافة أسطوانة جديدة إلى المخزون",
                                color = ColorSuccess,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // أزرار العمليات — زر متوهّج (المعرض #04) + ظهور متتابع (المعرض #63)
                    StaggeredReveal(index = 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlowingButton(
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
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("➕ إضافة أسطوانة", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }

                            FilledTonalButton(
                                onClick = onNavigateToDispense,
                                modifier = Modifier.weight(1f)
                            ) { Text("🔄 صرف / بيع", maxLines = 1) }

                            FilledTonalButton(
                                onClick = onNavigateToReports,
                                modifier = Modifier.weight(1f)
                            ) { Text("📊 تقارير", maxLines = 1) }
                        }
                    }

                    // بطاقة نصائح — ظهور متتابع (المعرض #63)
                    StaggeredReveal(index = 2) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("💡 تلميح", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "كل أسطوانة تضيفها من المحطة تُسجَّل هنا كمتوفرة، وكل بيع يقلّل العداد تلقائياً بعد صرفه من شاشة «صرف / بيع».",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ColorSuccess = androidx.compose.ui.graphics.Color(0xFF2E7D5B)
