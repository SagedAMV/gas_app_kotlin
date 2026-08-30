package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dabb.business.model.PaymentEntity

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(p: PaymentEntity)

    @Query("SELECT * FROM payments WHERE customerId = :id ORDER BY paymentDate DESC")
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

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
