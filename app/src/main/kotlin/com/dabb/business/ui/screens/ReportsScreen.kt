package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dabb.business.ui.animation.AnimatedMoney
import com.dabb.business.ui.animation.AnimatedNumber
import com.dabb.business.ui.animation.AnimatedProgressBar
import com.dabb.business.ui.animation.BreathingIndicator
import com.dabb.business.ui.animation.StaggeredReveal
import com.dabb.business.ui.animation.safeFraction
import com.dabb.business.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * واجهة التقارير — إجمالي المخزون، المباع، الدخل، الدين
 *
 * الأنميشنات المدمجة من المعرض:
 * - #63: بطاقات التقارير تدخل متتابعة
 * - #97: كل الأرقام تعدّ تصاعدياً من صفر حتى القيمة الفعلية
 * - #62: أشرطة نسب تُملأ بسلاسة تحت كل رقم
 * - #16: نبض أحمر على بطاقة الدين (تنبيه دائم)
 * - #57: مؤشر دوّار أثناء التحديث اليدوي
 * - #84: زر التحديث يتحوّل (↻ ↔ دائرة)
 */
@Composable
fun ReportsScreen() {
    val scope = rememberCoroutineScope()
    val viewModel: AppViewModel = viewModel()
    val available = viewModel.availableCount
    val sold = viewModel.soldCount
    val totalPaid = viewModel.totalPaid
    val totalCredit = viewModel.totalCredit
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshInventory()
        viewModel.refreshStats()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📊 التقارير", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.weight(1f))
            // زر تحديث — يتحول إلى دائرة أثناء التحديث (المعرض #57 / #84)
            TextButton(
                onClick = {
                    scope.launch {
                        refreshing = true
                        viewModel.refreshInventory()
                        viewModel.refreshStats()
                        delay(500)
                        refreshing = false
                    }
                }
            ) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("↻ تحديث")
                }
            }
        }

        val totalUnits = available + sold

        StaggeredReveal(index = 0) {
            ReportCard(
                title = "المخزون المتاح",
                color = MaterialTheme.colorScheme.primary
            ) {
                AnimatedNumber(
                    value = available,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(" أسطوانة", style = MaterialTheme.typography.bodyMedium)
            }
        }

        StaggeredReveal(index = 1) {
            ReportCard(
                title = "المباع",
                color = MaterialTheme.colorScheme.secondary
            ) {
                AnimatedNumber(
                    value = sold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(" أسطوانة", style = MaterialTheme.typography.bodyMedium)
            }
        }

        StaggeredReveal(index = 2) {
            ReportCard(
                title = "إجمالي المدفوع",
                color = MaterialTheme.colorScheme.tertiary
            ) {
                AnimatedMoney(
                    value = totalPaid,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(" جنيه", style = MaterialTheme.typography.bodyMedium)
            }
        }

        StaggeredReveal(index = 3) {
            ReportCard(
                title = "إجمالي الدين (بالآجل)",
                color = MaterialTheme.colorScheme.error,
                pulse = true
            ) {
                AnimatedMoney(
                    value = totalCredit,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(" جنيه", style = MaterialTheme.typography.bodyMedium)
            }
        }

        StaggeredReveal(index = 4) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📈 نسبة المباع من إجمالي المخزون", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    // شريط نسب يُملأ بسلاسة (المعرض #62)
                    AnimatedProgressBar(
                        progress = safeFraction(sold, totalUnits),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${if (totalUnits == 0) 0 else sold * 100 / totalUnits}٪ من الأسطوانات بيعت",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    title: String,
    color: Color,
    pulse: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pulse) {
                    BreathingIndicator(size = 10.dp, color = color)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                content()
            }
        }
    }
}
