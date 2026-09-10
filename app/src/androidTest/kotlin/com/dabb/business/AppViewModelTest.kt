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
    fun dao_rejects_zero_payment() = runTest {
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

    /**
     * العيب 2 (تقرير 2026-09-10): القفل التدرّجي يصمد عبر «إعادة تشغيل التطبيق» —
     * الحالة تُقرأ من SharedPreferences لا من ذاكرة الشاشة، والمدة تتصاعد.
     */
    @Test
    fun lockout_survives_app_restart() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = SettingsStore(app)
        try {
            store.setPin("1234")
            // 5 محاولات خاطئة ⇒ قفل الدرجة الأولى (60 ثانية)
            repeat(5) { store.registerPinFailure(now = 1_000L) }
            assertTrue(store.pinLockRemainingMs(now = 2_000L) > 0L)
            // «إعادة تشغيل» = كائن جديد يقرأ نفس التفضيلات
            val afterRestart = SettingsStore(app)
            assertTrue(afterRestart.pinLockRemainingMs(now = 2_000L) > 0L)
            // التصعيد: 10 محاولات ⇒ الدرجة الثانية (5 دقائق)
            repeat(5) { store.registerPinFailure(now = 3_000L) }
            assertTrue(SettingsStore(app).pinLockRemainingMs(now = 3_000L) > 60_000L)
            // التصفير ينهي القفل
            store.resetPinFailures()
            assertEquals(0L, afterRestart.pinLockRemainingMs(now = 4_000L))
        } finally {
            store.resetPinFailures(); store.clearPin()
        }
    }

    /**
     * العيب 11 + م2 (تقرير 2026-09-10): هجرة 4→5 — إعادة بناء payments
     * بمفتاح أجنبي + CHECK، وstation_purchases بقيود CHECK.
     *
     * لا exported schemas في المشروع (exportSchema=false تاريخياً) فلا يمكن
     * MigrationTestHelper — لذا نبني قاعدة v4 يدوياً بـ SQL خام (مطابقة
     * لما تنتجه الهجرات 1→4) ثم نفتحها بـ Room بكل الهجرات: الفتح يشغّل
     * 4→5 ويتحقق من تطابق المخطط بالكامل — أي خلل يفشل الاختبار بصوت عالٍ.
     */
    @Test
    fun migration_4_to_5_adds_fk_and_checks() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val name = "migration_test_${System.nanoTime()}.sqlite"
        val file = app.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        try {
            // ══ v4 كما تنتجه الهجرات 1→4 (قبل العيب 11) ══
            val raw = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
            try {
                raw.execSQL(
                    "CREATE TABLE `cylinders` (`id` TEXT NOT NULL, `sizeLiters` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `acquiredFromStation` TEXT NOT NULL, " +
                        "`acquisitionCost` INTEGER NOT NULL, `acquiredDate` INTEGER NOT NULL, " +
                        "`soldDate` INTEGER NOT NULL DEFAULT 0, `purchaseId` TEXT NOT NULL DEFAULT '', " +
                        "PRIMARY KEY(`id`))"
                )
                raw.execSQL(
                    "CREATE TABLE `customers` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`phone` TEXT NOT NULL, `totalDebt` INTEGER NOT NULL, `totalPaid` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                raw.execSQL(
                    "CREATE TABLE `sales` (`id` TEXT NOT NULL, `customerId` TEXT NOT NULL, " +
                        "`customerName` TEXT NOT NULL, `cylinderIdsJson` TEXT NOT NULL, " +
                        "`unitsSold` INTEGER NOT NULL, `pricePerUnit` INTEGER NOT NULL, " +
                        "`totalAmount` INTEGER NOT NULL, `amountPaid` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `saleDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`), FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE RESTRICT)"
                )
                raw.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)")
                raw.execSQL(
                    "CREATE TABLE `payments` (`id` TEXT NOT NULL, `customerId` TEXT NOT NULL, " +
                        "`customerName` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                        "`paymentDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                raw.execSQL(
                    "CREATE TABLE `station_purchases` (`id` TEXT NOT NULL, `units` INTEGER NOT NULL, " +
                        "`costPerUnit` INTEGER NOT NULL, `totalAmount` INTEGER NOT NULL, " +
                        "`amountPaid` INTEGER NOT NULL, `purchaseDate` INTEGER NOT NULL, " +
                        "`notes` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                raw.execSQL(
                    "CREATE TABLE `station_payments` (`id` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                        "`paymentDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                // بيانات يجب أن تنجو من الهجرة
                raw.execSQL("INSERT INTO customers VALUES ('c1','زبون الاختبار','050',5000,2000,1)")
                raw.execSQL("INSERT INTO payments VALUES ('p1','c1','زبون الاختبار',2000,10,'دفعة')")
                raw.execSQL("INSERT INTO station_purchases VALUES ('s1',10,2000,20000,5000,20,'سحب')")
                raw.execSQL("PRAGMA user_version = 4")
            } finally {
                raw.close()
            }

            // ══ الفتح بـ Room: يشغّل 4→5 ويتحقق من المخطط كاملاً ══
            val db = androidx.room.Room.databaseBuilder(app, AppDatabase::class.java, name)
                .addMigrations(
                    AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                    AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5
                )
                .build()
            try {
                // البيانات نجت
                val payments = db.paymentDao().getByCustomer("c1")
                assertEquals(1, payments.size)
                assertEquals(2000L, payments.first().amount)

                val w = db.openHelper.writableDatabase
                // المفتاح الأجنبي مفعّل: تحصيل لزبون وهمي يُرفض
                val fk = runCatching {
                    w.execSQL("INSERT INTO payments VALUES ('p_bad','ghost','x',100,1,'')")
                }
                assertTrue("FK يجب أن يرفض التحصيل لزبون غير موجود", fk.isFailure)

                // CHECK مفعّل: كمية سالبة في سحب المحطة تُرفض
                val chk = runCatching {
                    w.execSQL("INSERT INTO station_purchases VALUES ('s_bad',-5,1,1,1,1,'')")
                }
                assertTrue("CHECK يجب أن يرفض الكمية السالبة", chk.isFailure)

                // والمفتاح الأجنبي ظاهر فعلاً في المخطط
                val fks = w.query("PRAGMA foreign_key_list(payments)").use { c ->
                    mutableListOf<String>().also { while (c.moveToNext()) it.add(c.getString(2)) }
                }
                assertTrue("payments يجب أن يشير إلى customers", fks.contains("customers"))
            } finally {
                db.close()
            }
        } finally {
            file.delete()
            java.io.File(file.path + "-wal").delete()
            java.io.File(file.path + "-shm").delete()
        }
    }
}
