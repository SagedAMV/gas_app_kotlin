package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val phone: String = "",
    val totalDebt: Double = 0.0,
    val totalPaid: Double = 0.0,
    val createdAt: Long = 0L
) {
    /**
     * رصيد الزبون — يتم حسابه من البيانات مباشرة
     */
    fun currentBalance(): Double = totalDebt - totalPaid
}
