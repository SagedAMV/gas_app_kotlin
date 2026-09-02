package com.dabb.business

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dabb.business.data.local.AppDatabase
import com.dabb.business.data.local.SettingsStore
import com.dabb.business.model.CustomerEntity
import com.dabb.business.ui.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * إصلاح المشكلة 17: اختبارات آلية لتدفق الأعمال الجوهري:
 * بيع → تحصيل → عكس تحصيل → إلغاء بيع → أرصدة.
 *
 * تعمل على قاعدة في الذاكرة (Room.inMemoryDatabaseBuilder) عبر الباني الداخلي
 * لـ AppViewModel — بلا لمس بيانات الجهاز. تُشغَّل بـ:
 * ./gradlew :app:connectedDebugAndroidTest
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AppViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var vm: AppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vm = AppViewModel(app, db, SettingsStore(app))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun sale_payment_cancel_flow_keeps_balances_correct() = runTest {
        // ① سحب 3 أسطوانات من المحطة (تكلفة 10,000 للواحدة، بلا دفع)
        vm.purchaseFromStation(3, 10_000L, 0L, "").join()
        assertEquals(3, db.cylinderDao().getAvailableCount())
        assertEquals(30_000L, db.stationDao().getStationBalance())

        // ② بيع آجل لزبون «جديد» (2 × 25,000) — يختبر أيضاً إصلاح المشكلة 1:
        //    الزبون المؤقت من شاشة الصرف يجب أن يُحفظ في القاعدة
        val tempCustomer = CustomerEntity(
            id = UUID.randomUUID().toString(),
            name = "زبون اختبار",
            phone = "777000111",
            createdAt = System.currentTimeMillis()
        )
        var saleError: String? = "لم تُستدع النتيجة"
        vm.recordSale(tempCustomer, 2, 25_000L, payNow = false, notes = "") { saleError = it }.join()
        assertEquals(null, saleError)

        val saved = db.customerDao().findByName("زبون اختبار")
        assertNotNull("الزبون الجديد يجب أن يُحفظ (المشكلة 1)", saved)
        val custId = saved!!.id
        assertEquals(50_000L, db.saleDao().getCustomerBalance(custId))
        assertEquals(1, db.cylinderDao().getAvailableCount())

        // ③ تحصيل 20,000 من الزبون → الرصيد 30,000
        // مهلة حقيقية صغيرة حتى يختلف طابع التحصيل الزمني عن طابع البيع
        // (شرط «التحصيلات اللاحقة» يقارن بـ > حصراً)
        Thread.sleep(15)
        var payError: String? = "لم تُستدع النتيجة"
        vm.recordCustomerPayment(custId, 20_000L, "دفعة أولى") { payError = it }.join()
        assertEquals(null, payError)
        assertEquals(30_000L, db.saleDao().getCustomerBalance(custId))

        // ④ إلغاء البيع الآن يجب أن يُرفض — توجد تحصيلات لاحقة (المشكلة 3)
        val saleId = db.saleDao().getByCustomer(custId).first().id
        var cancelError: String? = null
        vm.cancelSale(saleId) { cancelError = it }.join()
        assertNotNull("الإلغاء يجب أن يُرفض مع وجود تحصيلات لاحقة", cancelError)
        assertEquals(1, db.saleDao().getByCustomer(custId).size)

        // ⑤ عكس التحصيل → الرصيد يعود 50,000
        val paymentId = db.paymentDao().getByCustomer(custId).first().id
        vm.reverseCustomerPayment(paymentId).join()
        assertEquals(50_000L, db.saleDao().getCustomerBalance(custId))

        // ⑥ الآن إلغاء البيع ينجح: الأسطوانتان تعودان للمخزون والدين يصفر
        var cancelError2: String? = "لم تُستدع النتيجة"
        vm.cancelSale(saleId) { cancelError2 = it }.join()
        assertEquals(null, cancelError2)
        assertEquals(3, db.cylinderDao().getAvailableCount())
        assertEquals(0L, db.saleDao().getCustomerBalance(custId))
    }

    @Test
    fun station_purchase_cancel_removes_its_cylinders() = runTest {
        vm.purchaseFromStation(2, 12_000L, 5_000L, "").join()
        val purchase = db.stationDao().getPurchases().first()
        assertEquals(2, db.cylinderDao().getAvailableCount())

        var err: String? = "لم تُستدع النتيجة"
        vm.cancelStationPurchase(purchase.id) { err = it }.join()
        assertEquals(null, err)
        assertEquals(0, db.cylinderDao().getAvailableCount())
        assertEquals(0L, db.stationDao().getStationBalance())
    }

    @Test
    fun oversell_is_rejected() = runTest {
        vm.purchaseFromStation(1, 10_000L, 0L, "").join()
        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(), name = "زبون", phone = "",
            createdAt = System.currentTimeMillis()
        )
        var err: String? = null
        vm.recordSale(customer, 5, 25_000L, payNow = true, notes = "") { err = it }.join()
        assertTrue("البيع فوق المخزون يجب أن يُرفض", err != null)
        assertEquals(1, db.cylinderDao().getAvailableCount())
    }
}
