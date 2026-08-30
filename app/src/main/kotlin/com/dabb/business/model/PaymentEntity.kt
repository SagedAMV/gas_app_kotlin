package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** دفعة تحصيل من زبون (سداد كلي/جزئي). المبلغ بالقروش. */
@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val amount: Long = 0L,
    val paymentDate: Long = 0L,
    val notes: String = ""
)
