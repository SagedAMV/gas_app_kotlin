package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** سحب كمية أسطوانات من المحطة (المورد). المبالغ بالقروش. */
@Entity(tableName = "station_purchases")
data class StationPurchaseEntity(
    @PrimaryKey val id: String,
    val units: Int = 0,
    val costPerUnit: Long = 0L,
    val totalAmount: Long = 0L,
    val amountPaid: Long = 0L,
    val purchaseDate: Long = 0L,
    val notes: String = ""
)
