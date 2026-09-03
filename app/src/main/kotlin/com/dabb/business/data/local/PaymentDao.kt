package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dabb.business.model.PaymentEntity

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(p: PaymentEntity)

    // إصلاح الخطأ 10: ‏LIMIT 200 (الأحدث أولاً — فحص «التحصيلات اللاحقة» يبقى صحيحاً)
    @Query("SELECT * FROM payments WHERE customerId = :id ORDER BY paymentDate DESC LIMIT 200")
    suspend fun getByCustomer(id: String): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    suspend fun getAll(): List<PaymentEntity>

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments")
    suspend fun getTotalCollections(): Long

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments WHERE paymentDate >= :from")
    suspend fun getCollectionsSince(from: Long): Long

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments WHERE customerId = :id")
    suspend fun getCustomerCollections(id: String): Long

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    /** عدّاد خفيف لفحص الوجود (بدل تحميل حتى 200 صف). */
    @Query("SELECT COUNT(*) FROM payments WHERE customerId = :id")
    suspend fun countForCustomer(id: String): Int

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /** إصلاح الخطأ 9: تحديث الاسم المكرر عند تعديل اسم الزبون. */
    @Query("UPDATE payments SET customerName = :name WHERE customerId = :id")
    suspend fun updateCustomerName(id: String, name: String): Int
}
