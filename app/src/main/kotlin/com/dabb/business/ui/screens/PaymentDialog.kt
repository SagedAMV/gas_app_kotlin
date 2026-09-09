package com.dabb.business.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dabb.business.ui.animation.Motion
import com.dabb.business.ui.animation.errorFlash
import com.dabb.business.ui.animation.motionDuration
import com.dabb.business.ui.animation.shakeEffect
import com.dabb.business.util.Money

/**
 * حوار إدخال مبلغ مشترك — محوَّل من Dialog مركزي إلى Bottom Sheet متحرك
 * بالكامل وفق دليل إعادة التصميم §5.4:
 *  - دخول slideInVertically + fadeIn بمنحنى EaseOutQuint (توقّف ناعم)
 *  - الخلفية المعتمة تتدرّج شفافيتها بالتوازي (سلوك ModalBottomSheet)
 *  - عند الخطأ: اهتزاز موضعي على حقل المبلغ المخالف تحديداً + وميض حد أحمر
 *
 * @param maxPiasters الحد الأقصى المسموح به (الرصيد/الدَّين)
 * @param allowExceedMax true = تحصيل زائد مسموح (رصيد دائن — الفحص 7)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    title: String,
    maxPiasters: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountPiasters: Long, note: String) -> Unit,
    allowExceedMax: Boolean = false
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var shakeKey by remember { mutableStateOf(0) }

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
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                "المتاح: ${Money.format(maxPiasters)} ريال",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            // إصلاح الفحص 7: تحصيل زائد مسموح للزبائن — الفارق رصيد دائن لصالحهم
            if (allowExceedMax) Text(
                "يمكن إدخال مبلغ أكبر — الفارق يُسجَّل رصيداً دائناً للزبون",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            // حقل المبلغ: اهتزاز ووميض موضعيان عند الخطأ (§5.4/§6.2.7)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(11) },
                label = { Text("المبلغ (ريال)") }, singleLine = true,
                isError = err != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeEffect(shakeKey)
                    .errorFlash(shakeKey)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("ملاحظة (اختياري)") }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (err != null) Text(
                err!!, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val amount = Money.poundsToPiasters(amountText)
                        when {
                            amount <= 0L -> { err = "أدخل مبلغاً أكبر من صفر"; shakeKey++ }
                            !allowExceedMax && amount > maxPiasters -> {
                                err = "المبلغ أكبر من المتاح (${Money.format(maxPiasters)} ريال)"
                                shakeKey++
                            }
                            else -> onConfirm(amount, note)
                        }
                    },
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("تأكيد", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
