package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** سداد دفعة للمحطة (المورد). المبلغ بالقروش. */
@Entity(tableName = "station_payments")
data class StationPaymentEntity(
    @PrimaryKey val id: String,
    val amount: Long = 0L,
    val paymentDate: Long = 0L,
    val notes: String = ""
)
