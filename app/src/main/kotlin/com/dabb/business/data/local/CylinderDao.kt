package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dabb.business.model.CylinderEntity
import com.dabb.business.model.CylinderStatus

@Dao
interface CylinderDao {
    @Insert
    suspend fun insert(c: CylinderEntity)

    @Insert
    suspend fun insertAll(cylinders: List<CylinderEntity>)

    @Query("SELECT COUNT(*) FROM cylinders WHERE status = 'AVAILABLE'")
    suspend fun getAvailableCount(): Int

    @Query("SELECT COUNT(*) FROM cylinders WHERE status = 'SOLD'")
    suspend fun getSoldCount(): Int

    @Query("SELECT * FROM cylinders")
    suspend fun getAll(): List<CylinderEntity>

    @Query("SELECT * FROM cylinders WHERE status = 'AVAILABLE' LIMIT :limit")
    suspend fun getAvailable(limit: Int): List<CylinderEntity>

    @Query("SELECT COALESCE(SUM(acquisitionCost),0) FROM cylinders WHERE status = 'SOLD'")
    suspend fun getSoldCost(): Long

    /** تكلفة الأسطوانات المباعة خلال فترة (لربح الفترة الزمنية). */
    @Query("SELECT COALESCE(SUM(acquisitionCost),0) FROM cylinders WHERE status = 'SOLD' AND soldDate >= :from")
    suspend fun getSoldCostSince(from: Long): Long

    /** خصم بيع: تعليم عدد من المتوفر كمباع — صفوف حقيقية. */
    @Query("UPDATE cylinders SET status = 'SOLD', soldDate = :now WHERE id IN (SELECT id FROM cylinders WHERE status = 'AVAILABLE' LIMIT :units)")
    suspend fun markSoldByQuantity(units: Int, now: Long): Int

    /** إلغاء بيع: إرجاع أسطوانات عملية ملغاة إلى المخزون. */
    @Query("UPDATE cylinders SET status = 'AVAILABLE', soldDate = 0 WHERE id IN (:ids)")
    suspend fun markAvailable(ids: List<String>): Int
}
