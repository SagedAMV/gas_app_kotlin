package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dabb.business.model.CustomerEntity

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(c: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%' ORDER BY name COLLATE NOCASE ASC")
    suspend fun search(q: String): List<CustomerEntity>

    /** المدينون: كاش الحقول يطابق الرصيد المشتق (يُعاد حسابه في المعاملات). */
    @Query("SELECT * FROM customers WHERE totalDebt > totalPaid ORDER BY (totalDebt - totalPaid) DESC")
    suspend fun getCustomersWithDebt(): List<CustomerEntity>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
