package com.dabb.business.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dabb.business.model.StationPaymentEntity
import com.dabb.business.model.StationPurchaseEntity

@Dao
interface StationDao {
    @Insert
    suspend fun insertPurchase(p: StationPurchaseEntity)

    // إصلاح الخطأ 10: ‏LIMIT 200 (الواجهة تعرض أحدث 30 أصلاً)
    @Query("SELECT * FROM station_purchases ORDER BY purchaseDate DESC LIMIT 200")
    suspend fun getPurchases(): List<StationPurchaseEntity>

    @Query("SELECT COALESCE(SUM(totalAmount),0) FROM station_purchases")
    suspend fun getTotalPurchases(): Long

    // فحص 2026-09-14: حُذفت دوال الفترة (getPurchasesSince/getPaidSince/getPurchasePaid)
    // — لم تُستخدم في أي شاشة (التقارير تعتمد مجاميع الكل) — كود ميت

    @Insert
    suspend fun insertPayment(p: StationPaymentEntity)

    // إصلاح الخطأ 10: ‏LIMIT 200
    @Query("SELECT * FROM station_payments ORDER BY paymentDate DESC LIMIT 200")
    suspend fun getPayments(): List<StationPaymentEntity>

    // فحص 2026-09-14: دالة getTotalPaid() حُذفت كذلك — بلا مستدعٍ (رصيد
    // المحطة يُشتق مباشرة في استعلام واحد — دَين المحطة المتبقي أدناه)

    /** دَين المحطة المتبقي = السحوبات − ما دُفع عند السحب − التسديدات. */
    @Query(
        "SELECT COALESCE((SELECT SUM(totalAmount) FROM station_purchases),0) " +
        "- COALESCE((SELECT SUM(amountPaid) FROM station_purchases),0) " +
        "- COALESCE((SELECT SUM(amount) FROM station_payments),0)"
    )
    suspend fun getStationBalance(): Long

    // ===== إلغاء عمليات المحطة (إصلاح المشكلة 11) =====
    @Query("SELECT * FROM station_purchases WHERE id = :id")
    suspend fun getPurchaseById(id: String): StationPurchaseEntity?

    @Query("DELETE FROM station_purchases WHERE id = :id")
    suspend fun deletePurchaseById(id: String): Int

    @Query("SELECT * FROM station_payments WHERE id = :id")
    suspend fun getPaymentById(id: String): StationPaymentEntity?

    @Query("DELETE FROM station_payments WHERE id = :id")
    suspend fun deletePaymentById(id: String): Int
}
