package com.dabb.business

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dabb.business.data.local.AppDatabase
import com.dabb.business.data.local.SettingsStore
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.PaymentEntity
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
        vm.recordSale(tempCustomer, 2, 25_000L, paidNowPiasters = 0L, notes = "") { saleError = it }.join()
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
        vm.recordSale(customer, 5, 25_000L, paidNowPiasters = 0L, notes = "") { err = it }.join()
        assertTrue("البيع فوق المخزون يجب أن يُرفض", err != null)
        assertEquals(1, db.cylinderDao().getAvailableCount())
    }

    /**
     * اختبار انحداري للـ P0 (فحص 2026-09-03): recomputeCustomer كانت تستخدم
     * INSERT OR REPLACE، والحذف الضمني محجوب بـ ON DELETE RESTRICT على sales —
     * فكان كل بيع/تحصيل/تعديل يُسقط بالمفتاح الأجنبي. هذا الاختبار كان سيفشل
     * قبل الإصلاح (saleError != null) وهو الذي كان ينقص التغطية.
     */
    @Test
    fun sale_then_update_customer_with_sales_succeeds() = runTest {
        vm.purchaseFromStation(2, 10_000L, 0L, "").join()
        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(), name = "الاسم الأصلي", phone = "",
            createdAt = System.currentTimeMillis()
        )
        var saleError: String? = "لم تُستدع النتيجة"
        vm.recordSale(customer, 1, 25_000L, paidNowPiasters = 0L, notes = "") { saleError = it }.join()
        assertEquals("البيع يجب أن ينجح", null, saleError)

        var updateError: String? = "لم تُستدع النتيجة"
        vm.updateCustomer(customer.id, "الاسم الجديد", "777123456") { updateError = it }.join()
        assertEquals("تعديل زبون له مبيعات يجب أن ينجح", null, updateError)
        assertEquals("الاسم الجديد", db.customerDao().getById(customer.id)?.name)
        val sales = db.saleDao().getByCustomer(customer.id)
        assertEquals(1, sales.size)
        assertEquals("الاسم الجديد", sales.first().customerName)
    }

    /**
     * اختبار انحداري للـ H3 (فحص 2026-09-03): مبالغ بلا سقف كانت تشبع
     * Math.round عند Long.MAX ثم overflow في units*price — فساد مالي صامت.
     */
    @Test
    fun oversized_price_and_units_are_rejected() = runTest {
        vm.purchaseFromStation(1, 10_000L, 0L, "").join()
        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(), name = "زبون", phone = "",
            createdAt = System.currentTimeMillis()
        )
        var err: String? = null
        vm.recordSale(customer, 1, com.dabb.business.util.Money.MAX_AMOUNT + 1,
            paidNowPiasters = 0L, notes = "") { err = it }.join()
        assertNotNull("سعر يتجاوز السقف يجب أن يُرفض", err)
        assertEquals(0, db.saleDao().getCount())

        var err2: String? = null
        vm.purchaseFromStation(com.dabb.business.util.Money.MAX_UNITS + 1, 10_000L, 0L, "") {
            err2 = it
        }.join()
        assertNotNull("كمية تتجاوز السقف يجب أن تُرفض", err2)
    }

    /** سداد المحطة فوق الدَّين يجب أن يُرفض ولا يمس الرصيد. */
    @Test
    fun station_payment_over_balance_is_rejected() = runTest {
        vm.purchaseFromStation(2, 10_000L, 0L, "").join()
        assertEquals(20_000L, db.stationDao().getStationBalance())
        var err: String? = null
        vm.payStation(30_000L, "") { err = it }.join()
        assertNotNull("سداد أكبر من دَين المحطة يجب أن يُرفض", err)
        assertEquals(20_000L, db.stationDao().getStationBalance())
        assertEquals(0, db.stationDao().getPayments().size)
    }

    /**
     * إصلاح الفحص 4 (فحص 2026-09-09): دفع جزئي وقت البيع —
     * سجل واحد بمبلغ مدفوع جزئي، والباقي دَين.
     */
    @Test
    fun partial_payment_at_sale_time_records_remaining_debt() = runTest {
        vm.purchaseFromStation(1, 10_000L, 0L, "").join()
        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(), name = "زبون جزئي", phone = "",
            createdAt = System.currentTimeMillis()
        )
        var err: String? = "لم تُستدع النتيجة"
        vm.recordSale(customer, 1, 25_000L, paidNowPiasters = 10_000L, notes = "") { err = it }.join()
        assertEquals("البيع بالدفع الجزئي يجب أن ينجح", null, err)
        val saved = db.customerDao().findByName("زبون جزئي")
        assertNotNull(saved)
        val sale = db.saleDao().getByCustomer(saved!!.id).first()
        assertEquals(10_000L, sale.amountPaid)
        assertEquals(15_000L, db.saleDao().getCustomerBalance(saved.id))
    }

    /**
     * إصلاح الفحص 7 (فحص 2026-09-09): السداد الزائد ينتج رصيداً
     * دائناً (سالباً) لصالح الزبون — لا يُرفض.
     */
    @Test
    fun overpayment_creates_credit_balance() = runTest {
        vm.purchaseFromStation(1, 10_000L, 0L, "").join()
        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(), name = "زبون دائن", phone = "",
            createdAt = System.currentTimeMillis()
        )
        vm.recordSale(customer, 1, 25_000L, paidNowPiasters = 0L, notes = "") { }.join()
        val saved = db.customerDao().findByName("زبون دائن")
        assertNotNull(saved)
        var err: String? = "لم تُستدع النتيجة"
        vm.recordCustomerPayment(saved!!.id, 30_000L, "سداد زائد") { err = it }.join()
        assertEquals("التحصيل الزائد يجب أن يُقبل", null, err)
        assertEquals(-5_000L, db.saleDao().getCustomerBalance(saved.id))
    }

    /**
     * إصلاح الفحص 82 (فحص 2026-09-09): بيع المنحة بسعر 0 —
     * يُقبل بلا استثناء حسابي والرصيد لا يتأثر.
     */
    @Test
    fun zero_price_gift_sale_is_accepted() = runTest {
        vm.purchaseFromStation(1, 10_000L, 0L, "").join()
        val customer = CustomerEntity(
            id = UUID.randomUUID().toString(), name = "زبون منحة", phone = "",
            createdAt = System.currentTimeMillis()
        )
        var err: String? = "لم تُستدع النتيجة"
        vm.recordSale(customer, 1, 0L, paidNowPiasters = 0L, notes = "عينة") { err = it }.join()
        assertEquals("بيع المنحة بسعر 0 يجب أن يُقبل", null, err)
        val saved = db.customerDao().findByName("زبون منحة")
        assertNotNull(saved)
        val sale = db.saleDao().getByCustomer(saved!!.id).first()
        assertEquals(0L, sale.totalAmount)
        assertEquals(0L, db.saleDao().getCustomerBalance(saved.id))
        assertEquals(0, db.cylinderDao().getAvailableCount())
    }

    /**
     * إصلاح الفحص 97 (فحص 2026-09-09): طبقة الـ DAO نفسها
     * (insertValidated) ترفض التحصيل الصفري/السالب.
     */
    @Test
    fun dao_rejects_zero_payment() {
        val result = runCatching {
            db.paymentDao().insertValidated(
                PaymentEntity(
                    id = UUID.randomUUID().toString(), customerId = "x",
                    customerName = "x", amount = 0L, paymentDate = 1L, notes = ""
                )
            )
        }
        assertTrue("التحصيل الصفري يجب أن يُرفض داخل PaymentDao", result.isFailure)
    }
}
