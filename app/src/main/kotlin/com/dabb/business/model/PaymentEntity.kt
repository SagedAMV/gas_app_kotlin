package com.dabb.business.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * دفعة تحصيل من زبون (سداد كلي/جزئي). المبلغ بالقروش.
 * العيب 11 (تقرير 2026-09-10): مفتاح أجنبي نحو customers — دفاع ثانٍ في
 * المخطط نفسه بعد دفاع ViewModel، فلا تُكتب تحصيلات يتيمة تفسد الأرصدة
 * المشتقة (مثل sales منذ v3).
 */
@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = CustomerEntity::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index(value = ["customerId"])]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val customerId: String = "",
    val customerName: String = "",
    val amount: Long = 0L,
    val paymentDate: Long = 0L,
    val notes: String = ""
)
