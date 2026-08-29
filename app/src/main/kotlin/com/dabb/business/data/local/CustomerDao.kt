package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dabb.business.model.CustomerEntity

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(c: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%'")
    suspend fun search(q: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE totalDebt > totalPaid")
    suspend fun getCustomersWithDebt(): List<CustomerEntity>

    @Update
    suspend fun update(c: CustomerEntity)
}
