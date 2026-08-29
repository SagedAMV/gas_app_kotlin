package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val cylinderIdsJson: String = "", // القائمة مفصولة بفاصلة — يُحوّل عبر TypeConverter
    val unitsSold: Int = 0,
    val pricePerUnit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val status: String = "CREDIT", // PAID / CREDIT
    val saleDate: Long = 0L,
    val notes: String = ""
)
