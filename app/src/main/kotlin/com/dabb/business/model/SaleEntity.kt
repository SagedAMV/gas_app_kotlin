package com.dabb.business.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** عملية بيع. كل المبالغ بالقروش.
 *  إصلاح المشكلة 8: مفتاح أجنبي نحو customers يمنع مبيعات لزبائن غير موجودين،
 *  وRESTRICT يمنع حذف زبون ما زالت له مبيعات. الفهرس على customerId يمنع
 *  فحص الجدول كاملاً عند تعديل الجدول الأب (تحذير Room). */
@Entity(
    tableName = "sales",
    foreignKeys = [ForeignKey(
        entity = CustomerEntity::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index(value = ["customerId"])]
)
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
