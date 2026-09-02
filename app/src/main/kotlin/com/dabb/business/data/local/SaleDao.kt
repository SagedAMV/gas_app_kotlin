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

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    suspend fun getAll(): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE saleDate >= :from ORDER BY saleDate DESC")
    suspend fun getSince(from: Long): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: String): SaleEntity?

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteById(id: String): Int

    // ===== المجاميع الكلية =====
    @Query("SELECT COALESCE(SUM(totalAmount),0) FROM sales")
    suspend fun getTotalSales(): Long

    @Query("SELECT COALESCE(SUM(amountPaid),0) FROM sales")
    suspend fun getSaleAmountPaid(): Long

    // ===== المجاميع خلال فترة =====
    @Query("SELECT COALESCE(SUM(totalAmount),0) FROM sales WHERE saleDate >= :from")
    suspend fun getTotalSalesSince(from: Long): Long

    @Query("SELECT COALESCE(SUM(amountPaid),0) FROM sales WHERE saleDate >= :from")
    suspend fun getSalePaidSince(from: Long): Long

    // ===== رصيد زبون واحد (مصدر الحقيقة المشتق) =====
    @Query(
        "SELECT COALESCE((SELECT SUM(totalAmount) FROM sales WHERE customerId = :id),0) " +
        "- COALESCE((SELECT SUM(amountPaid) FROM sales WHERE customerId = :id),0) " +
        "- COALESCE((SELECT SUM(amount) FROM payments WHERE customerId = :id),0)"
    )
    suspend fun getCustomerBalance(id: String): Long

    @Query("SELECT COALESCE(SUM(totalAmount),0) FROM sales WHERE customerId = :id")
    suspend fun getCustomerSalesTotal(id: String): Long

    @Query("SELECT COALESCE(SUM(amountPaid),0) FROM sales WHERE customerId = :id")
    suspend fun getCustomerSalePaid(id: String): Long

    // ===== ترقيم صفحات (إصلاح المشكلة 13 — منع تحميل الجدول كاملاً) =====
    @Query("SELECT * FROM sales ORDER BY saleDate DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaged(limit: Int, offset: Int): List<SaleEntity>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getCount(): Int
}
