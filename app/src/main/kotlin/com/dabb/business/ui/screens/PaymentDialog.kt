package com.dabb.business.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dabb.business.util.Money

/**
 * حوار إدخال مبلغ مشترك (تحصيل من زبون / سداد للمحطة).
 * @param maxPiasters الحد الأقصى المسموح به بالقروش (الرصيد/الدَين).
 */
@Composable
fun PaymentDialog(
    title: String,
    maxPiasters: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountPiasters: Long, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المتاح: ${Money.format(maxPiasters)} ريال",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("المبلغ (ريال)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("ملاحظة (اختياري)") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                if (err != null) Text(err!!, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Money.poundsToPiasters(amountText)
                when {
                    amount <= 0L -> err = "أدخل مبلغاً أكبر من صفر"
                    amount > maxPiasters + 1L -> err = "المبلغ أكبر من المتاح (${Money.format(maxPiasters)} ج)"
                    else -> onConfirm(amount, note)
                }
            }) { Text("تأكيد", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
