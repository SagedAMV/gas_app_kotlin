package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dabb.business.model.PaymentEntity

/**
 * إصلاح الفحص 97: تحوّل الـ DAO من واجهة إلى صنف مجرد حتى يكون رفض
 * التحصيل الصفري/السالب داخل طبقة البيانات نفسها (insertValidated)
 * لا في المتصل فقط — أي مسار مستقبلي يمر من هنا محصَّن تلقائياً.
 */
@Dao
abstract class PaymentDao {
    @Insert
    abstract suspend fun insert(p: PaymentEntity)

    /** إدخال محصَّن (الفحص 97): يرفض أي دفعة تحصيل ≤ 0 على مستوى الـ DAO نفسه. */
    suspend fun insertValidated(p: PaymentEntity) {
        require(p.amount > 0L) { "مبلغ التحصيل يجب أن يكون أكبر من صفر" }
        insert(p)
    }

    // إصلاح الخطأ 10: LIMIT 200 (الأحدث أولاً — فحص «التحصيلات اللاحقة» يبقى صحيحاً)
    @Query("SELECT * FROM payments WHERE customerId = :id ORDER BY paymentDate DESC LIMIT 200")
    abstract suspend fun getByCustomer(id: String): List<PaymentEntity>

    // فحص 2026-09-14: دالة getAll() حُذفت — لم يكن لها أي مستدعٍ في التطبيق
    // (كل القوائم تُجلب بزبون أو بمجاميع — كود ميت)

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments")
    abstract suspend fun getTotalCollections(): Long

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments WHERE paymentDate >= :from")
    abstract suspend fun getCollectionsSince(from: Long): Long

    @Query("SELECT COALESCE(SUM(amount),0) FROM payments WHERE customerId = :id")
    abstract suspend fun getCustomerCollections(id: String): Long

    @Query("SELECT * FROM payments WHERE id = :id")
    abstract suspend fun getById(id: String): PaymentEntity?

    /** عدّاد خفيف لفحص الوجود (بدل تحميل حتى 200 صف). */
    @Query("SELECT COUNT(*) FROM payments WHERE customerId = :id")
    abstract suspend fun countForCustomer(id: String): Int

    @Query("DELETE FROM payments WHERE id = :id")
    abstract suspend fun deleteById(id: String): Int

    /** إصلاح الخطأ 9: تحديث الاسم المكرر عند تعديل اسم الزبون. */
    @Query("UPDATE payments SET customerName = :name WHERE customerId = :id")
    abstract suspend fun updateCustomerName(id: String, name: String): Int
}
