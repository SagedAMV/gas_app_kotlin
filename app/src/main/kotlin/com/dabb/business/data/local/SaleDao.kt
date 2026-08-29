package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dabb.business.model.SaleEntity

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(s: SaleEntity)

    @Query("SELECT * FROM sales WHERE customerId = :id ORDER BY saleDate DESC")
    suspend fun getByCustomer(id: String): List<SaleEntity>

    @Query("SELECT SUM(amountPaid) FROM sales")
    suspend fun getTotalPaid(): Double?

    @Query("SELECT SUM(totalAmount - amountPaid) FROM sales WHERE status = 'CREDIT'")
    suspend fun getTotalCredit(): Double?

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    suspend fun getAll(): List<SaleEntity>
}
