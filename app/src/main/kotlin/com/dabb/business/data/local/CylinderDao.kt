package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dabb.business.model.CylinderEntity

@Dao
interface CylinderDao {
    @Insert
    suspend fun insert(c: CylinderEntity)

    @Query("SELECT COUNT(*) FROM cylinders WHERE status = 'AVAILABLE'")
    suspend fun getAvailableCount(): Int

    @Query("SELECT COUNT(*) FROM cylinders WHERE status = 'SOLD'")
    suspend fun getSoldCount(): Int

    @Query("SELECT * FROM cylinders")
    suspend fun getAll(): List<CylinderEntity>

    @Query("UPDATE cylinders SET status = 'SOLD' WHERE id IN (:ids)")
    suspend fun markSold(ids: List<String>)
}
