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

    /** خصم بيع: تعليم عدد من المتوفر كمباع — صفوف حقيقية.
     *  إصلاح المشكلة 9: ‏ORDER BY acquiredDate ASC يضمن FIFO حقيقياً (الأقدم يُصرف أولاً). */
    @Query("UPDATE cylinders SET status = 'SOLD', soldDate = :now WHERE id IN (SELECT id FROM cylinders WHERE status = 'AVAILABLE' ORDER BY acquiredDate ASC, id ASC LIMIT :units)")
    suspend fun markSoldByQuantity(units: Int, now: Long): Int

    /** إلغاء بيع: إرجاع أسطوانات عملية ملغاة إلى المخزون.
     *  إصلاح المشكلة 2: شرط status='SOLD' AND soldDate=:soldDate يضمن ألا تُعاد
     *  أسطوانة بِيعت لاحقاً في عملية أخرى — تُعاد فقط أسطوانات هذا البيع تحديداً. */
    @Query("UPDATE cylinders SET status = 'AVAILABLE', soldDate = 0 WHERE id IN (:ids) AND status = 'SOLD' AND soldDate = :soldDate")
    suspend fun markAvailable(ids: List<String>, soldDate: Long): Int

    /** أسطوانات ما زالت متوفرة واكتُسبت في لحظة محددة — لإلغاء سحب محطة بأمان (المشكلة 11). */
    @Query("SELECT id FROM cylinders WHERE status = 'AVAILABLE' AND acquiredDate = :acquiredDate LIMIT :limit")
    suspend fun getAvailableIdsByAcquiredDate(acquiredDate: Long, limit: Int): List<String>

    /** حذف أسطوانات محددة — يُستخدم مع إلغاء سحب المحطة (المشكلة 11). */
    @Query("DELETE FROM cylinders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int
}
