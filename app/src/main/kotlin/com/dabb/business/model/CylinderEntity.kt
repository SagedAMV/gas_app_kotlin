package com.dabb.business.model

/**
 * Entity للقاعدة — أسطوانة غاز 20 لتر
 * المهارة: ㊼ (OOP + Room Entity)
 * مهارة دفاعية: لا يمكن إرسال اسطوانة مباعة كـ AVAILABLE مباشرة (تحتاج التحقق من DAO)
 */
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cylinders")
data class CylinderEntity(
    @PrimaryKey val id: String,
    val sizeLiters: Int = 20,
    val status: String = "AVAILABLE", // AVAILABLE / SOLD
    val acquiredFromStation: String = "",
    val acquisitionCost: Double = 0.0,
    val acquiredDate: Long = 0L
)
