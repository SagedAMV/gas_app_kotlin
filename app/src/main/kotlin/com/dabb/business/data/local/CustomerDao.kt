package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dabb.business.model.CustomerEntity

@Dao
interface CustomerDao {
    /** يُستخدم فقط لإنشاء زبون جديد (لا مبيعات بعد ← لا أطفال على المفتاح الأجنبي).
     *  ⚠️ محظور استخدامه لتحديث زبون له مبيعات: REPLACE = حذف ضمني ثم إدراج،
     *  والحذف يُحجَب بـ ON DELETE RESTRICT على sales.customerId فيفشل (P0). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(c: CustomerEntity)

    /** تحديث آمن: UPDATE عادي لا يحذف الصف ← لا يلمس المفتاح الأجنبي (إصلاح P0). */
    @Update
    suspend fun update(c: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    // العيب 6ب: سقف 2000 — الزبائن وحدهم كانوا بلا LIMIT بينما البحث 50
    // والمدينون 200 والمبيعات مرقّمة. حجم محلي واقعي لا يقاربه المستخدم.
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC LIMIT 2000")
    suspend fun getAll(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CustomerEntity?

    /** إصلاح الخطأ 10: ‏LIMIT 50 — اقتراحات البحث تكفي بها عشرات (حماية الذاكرة). */
    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%' ORDER BY name COLLATE NOCASE ASC LIMIT 50")
    suspend fun search(q: String): List<CustomerEntity>

    /** المدينون: كاش الحقول يطابق الرصيد المشتق (يُعاد حسابه في المعاملات).
     *  إصلاح الخطأ 10: ‏LIMIT 200 — الأعلى ديناً أولاً. */
    @Query("SELECT * FROM customers WHERE totalDebt > totalPaid ORDER BY (totalDebt - totalPaid) DESC LIMIT 200")
    suspend fun getCustomersWithDebt(): List<CustomerEntity>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /** ترقيم صفحات (إصلاح المشكلة 13). */
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllPaged(limit: Int, offset: Int): List<CustomerEntity>
}
