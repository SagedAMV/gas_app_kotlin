package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** عملية بيع. كل المبالغ بالقروش. */
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val cylinderIdsJson: String = "",
    val unitsSold: Int = 0,
    val pricePerUnit: Long = 0L,
    val totalAmount: Long = 0L,
    val amountPaid: Long = 0L,
    val status: SaleStatus = SaleStatus.CREDIT,
    val saleDate: Long = 0L,
    val notes: String = ""
)
