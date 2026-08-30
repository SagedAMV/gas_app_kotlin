package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * زبون. totalDebt/totalPaid بالقروش ككاش يُعاد حسابه من المعاملات داخل
 * معاملة واحدة، والمصدر الموثوق للرصيد هو المجاميع المشتقة في SaleDao/PaymentDao.
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String = "",
    val phone: String = "",
    val totalDebt: Long = 0L,
    val totalPaid: Long = 0L,
    val createdAt: Long = 0L
) {
    /** الرصيد المتبقي (دَين) بالقروش. */
    fun balancePiasters(): Long = totalDebt - totalPaid
}
