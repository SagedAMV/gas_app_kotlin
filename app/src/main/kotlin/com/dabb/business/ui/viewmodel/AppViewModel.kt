package com.dabb.business.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.dabb.business.data.local.AppDatabase
import com.dabb.business.data.local.SettingsStore
import com.dabb.business.model.CylinderEntity
import com.dabb.business.model.CylinderStatus
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.PaymentEntity
import com.dabb.business.model.SaleEntity
import com.dabb.business.model.SaleStatus
import com.dabb.business.model.StationPaymentEntity
import com.dabb.business.model.StationPurchaseEntity
import com.dabb.business.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** فترة التقرير الزمنية. */
enum class ReportPeriod(val label: String) {
    ALL("الكل"), MONTH("الشهر"), WEEK("الأسبوع"), DAY("اليوم");

    /** إصلاح الفحص L17: "اليوم" = من منتصف الليل المحلي (كما يتوقعه المستخدم)
     *  وليس آخر 24 ساعة. الأسبوع/الشهر نافذة متدحرجة مقصودة. */
    fun startMillis(now: Long = System.currentTimeMillis()): Long = when (this) {
        ALL -> 0L
        DAY -> {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = now
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        WEEK -> now - 7L * 24 * 3600 * 1000
        MONTH -> now - 30L * 24 * 3600 * 1000
    }
}

/**
 * ViewModel موحّد. كل عملية مالية/مخزونية داخل withTransaction واحدة:
 * إمّا تنجح كاملة أو تُلغى كاملة. كل المبالغ بالقروش (Long).
 */
class AppViewModel internal constructor(
    app: Application,
    private val db: AppDatabase,
    private val settings: SettingsStore
) : AndroidViewModel(app) {

    /** الباني العام للشاشات — قاعدة البيانات الحقيقية + الإعدادات.
     *  (الداخلي يسمح للاختبارات بحقن قاعدة في الذاكرة — المشكلة 17). */
    constructor(app: Application) : this(
        app,
        AppDatabase.getInstance(app.applicationContext),
        SettingsStore(app.applicationContext)
    )

    private val cylDao = db.cylinderDao()
    private val custDao = db.customerDao()
    private val saleDao = db.saleDao()
    private val payDao = db.paymentDao()
    private val stationDao = db.stationDao()

    // المخزون
    var availableCount by mutableStateOf(0); private set
    var soldCount by mutableStateOf(0); private set

    // الماليات (للفترة المختارة)
    var totalSales by mutableStateOf(0L); private set
    var totalPaid by mutableStateOf(0L); private set
    var totalCredit by mutableStateOf(0L); private set
    var totalCost by mutableStateOf(0L); private set
    var profit by mutableStateOf(0L); private set
    var stationBalance by mutableStateOf(0L); private set

    /** ماليات "الكل" (خارج الفلترة) — إصلاح الفحص M2: «الدين المتبقي» بلا
     *  فلترة فترة فلا يصبح سالباً عند تحصيل دين قديم داخل الفترة.
     *  الدونات والدين المتبقي تستخدمها لأنها "حالة حالية" لا "حركة فترة". */
    var allTimeSales by mutableStateOf(0L); private set
    var allTimePaid by mutableStateOf(0L); private set

    var recentSales by mutableStateOf<List<SaleEntity>>(emptyList()); private set
    var allSales by mutableStateOf<List<SaleEntity>>(emptyList()); private set
    var topDebtors by mutableStateOf<List<CustomerEntity>>(emptyList()); private set
    var allCustomers by mutableStateOf<List<CustomerEntity>>(emptyList()); private set

    var reportPeriod by mutableStateOf(ReportPeriod.ALL); private set
    var defaultPricePiasters by mutableStateOf(settings.defaultPricePiasters); private set

    var busy by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    init { refreshAll() }

    fun clearError() { errorMessage = null }

    fun setPeriod(p: ReportPeriod) { reportPeriod = p; refreshStats(); refreshRecent() }

    fun setDefaultPrice(piasters: Long) {
        if (piasters > 0) { settings.defaultPricePiasters = piasters; defaultPricePiasters = piasters }
    }

    fun refreshAll() {
        refreshInventory(); refreshStats(); refreshRecent(); refreshDebtors(); refreshCustomers()
    }

    fun refreshInventory() = launch {
        availableCount = cylDao.getAvailableCount()
        soldCount = cylDao.getSoldCount()
    }

    fun refreshStats() = launch {
        val from = reportPeriod.startMillis()
        val salesTotal: Long; val salePaid: Long; val collections: Long; val cost: Long
        if (from == 0L) {
            salesTotal = saleDao.getTotalSales()
            salePaid = saleDao.getSaleAmountPaid()
            collections = payDao.getTotalCollections()
            cost = cylDao.getSoldCost()
        } else {
            salesTotal = saleDao.getTotalSalesSince(from)
            salePaid = saleDao.getSalePaidSince(from)
            collections = payDao.getCollectionsSince(from)
            cost = cylDao.getSoldCostSince(from)
        }
        totalSales = salesTotal
        totalPaid = salePaid + collections
        totalCost = cost
        profit = salesTotal - cost
        stationBalance = stationDao.getStationBalance()
        // «الدين المتبقي» = الدَّين الحالي الكلي (بلا فلترة فترة):
        // كل ما بِيع − كل ما حُصِّل (عند البيع + التحصيلات اللاحقة).
        // بعد إصلاح الفحص 7 قد يمتلك زبونٌ رصيداً دائناً (سالباً) — يظهر في ملفه،
        // وصافي هذا العداد يُعرض بحد أدنى 0.
        val allSales = saleDao.getTotalSales()
        val allPaid = saleDao.getSaleAmountPaid() + payDao.getTotalCollections()
        allTimeSales = allSales
        allTimePaid = allPaid
        totalCredit = (allSales - allPaid).coerceAtLeast(0L)
    }

    fun refreshRecent() = launch {
        val from = reportPeriod.startMillis()
        // إصلاح المشكلة 13: تُحمَّل صفحة أولى فقط بدل الجدول كاملاً — بلا خطر OOM.
        val list = if (from == 0L) saleDao.getPaged(50, 0) else saleDao.getSince(from)
        allSales = list
        recentSales = list.take(6)
    }

    /** صفحة من المبيعات لسجل المبيعات الكامل (المشكلة 13). */
    suspend fun getSalesPaged(limit: Int, offset: Int): List<SaleEntity> =
        saleDao.getPaged(limit, offset)

    suspend fun getSalesCount(): Int = saleDao.getCount()

    /** إصلاح الفحص L13: بحث على مستوى القاعدة — الاسم أو الملاحظات. */
    suspend fun searchSalesByNameOrNotes(q: String): List<SaleEntity> =
        saleDao.searchByNameOrNotes(q.trim())

    /** نطاق يومي كامل (لبحث التاريخ في سجل المبيعات). */
    suspend fun getSalesBetween(from: Long, to: Long): List<SaleEntity> =
        saleDao.getBetween(from, to)

    fun refreshDebtors() = launch { topDebtors = custDao.getCustomersWithDebt().take(3) }

    fun refreshCustomers() = launch { allCustomers = custDao.getAll() }

    // ===== الزبائن =====
    suspend fun searchCustomers(q: String): List<CustomerEntity> =
        if (q.isBlank()) emptyList() else custDao.search(q.trim())

    suspend fun getCustomerDetail(id: String): CustomerDetail {
        val customer = custDao.getById(id)
        val sales = saleDao.getByCustomer(id)
        val payments = payDao.getByCustomer(id)
        val salesTotal = saleDao.getCustomerSalesTotal(id)
        val paid = saleDao.getCustomerSalePaid(id) + payDao.getCustomerCollections(id)
        val balance = salesTotal - paid
        return CustomerDetail(customer, sales, payments, salesTotal, paid, balance)
    }

    fun updateCustomer(id: String, name: String, phone: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            require(name.isNotBlank()) { "الاسم لا يمكن أن يكون فارغاً" }
            val dup = custDao.findByName(name.trim())
            if (dup != null && dup.id != id) error("يوجد زبون مسجّل بنفس الاسم «${name.trim()}»")
            db.withTransaction {
                val c = custDao.getById(id) ?: error("الزبون غير موجود")
                val newName = name.trim()
                // إصلاح P0: UPDATE — REPLACE كان يُسقط التعديل لأي زبون له مبيعات
                // (حذف ضمني محجوب بـ ON DELETE RESTRICT) بلا أي رسالة واضحة.
                custDao.update(c.copy(name = newName, phone = phone.trim()))
                // إصلاح الخطأ 9: الاسم مكرر في sales وpayments — يُحدَّث فيهما
                // أيضاً حتى لا تظهر التقارير والسجلات أسماء قديمة.
                saleDao.updateCustomerName(id, newName)
                payDao.updateCustomerName(id, newName)
            }
            refreshCustomers(); refreshDebtors()
        }
    }

    fun deleteCustomer(id: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            db.withTransaction {
                // عدّاد خفيف بدل تحميل حتى 200 صف (الفحص L12)
                if (saleDao.countForCustomer(id) > 0 || payDao.countForCustomer(id) > 0)
                    error("لا يمكن حذف زبون له عمليات بيع أو دفعات. ألغِ عملياته أولاً")
                custDao.deleteById(id)
            }
            refreshCustomers(); refreshDebtors()
        }
    }

    /** إنشاء زبون جديد (مع منع تكرار الاسم). */
    fun createCustomer(name: String, phone: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            val n = name.trim()
            require(n.isNotBlank()) { "أدخل اسم الزبون" }
            val dup = custDao.findByName(n)
            if (dup != null) error("يوجد زبون بنفس الاسم «$n»")
            custDao.insertOrUpdate(
                CustomerEntity(
                    id = UUID.randomUUID().toString(),
                    name = n, phone = phone.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
            refreshCustomers()
        }
    }

    // ===== البيع =====
fun recordSale(
    customer: CustomerEntity, units: Int, pricePiasters: Long,
    paidNowPiasters: Long, notes: String, onResult: (String?) -> Unit = {}
) = launch {
    runOp(onResult) {
        // إصلاح الفحص 82: السعر 0 مسموح (منحة/عينة). السالب مرفوض دائماً —
        // جوهر الفحص 15 («منع المبالغ السالبة») محفوظ كاملاً.
        require(units > 0 && pricePiasters in 0..Money.MAX_AMOUNT) {
            "أدخل عدداً صحيحاً وسعراً غير سالب (الحد الأقصى ${Money.format(Money.MAX_AMOUNT)} ريال)"
        }
        val totalAmountNow = units.toLong() * pricePiasters
        // إصلاح الفحص 4: دفع جزئي وقت البيع — النطاق [0, الإجمالي]
        // بنفس منطق سحب المحطة (purchaseFromStation).
        require(paidNowPiasters in 0..totalAmountNow) {
            "المدفوع الآن خارج النطاق (0 إلى ${Money.format(totalAmountNow)} ريال)"
        }
        val available = cylDao.getAvailableCount()
        if (available < units) error("المخزون لا يكفي — المتوفر $available أسطوانة فقط")

            val existing = custDao.getById(customer.id) ?: custDao.findByName(customer.name.trim())
            val custId = existing?.id ?: customer.id
            val base = existing ?: customer
            // طابع زمني موحّد فريد للأسطوانات والبيع — أساس المطابقة الدقيقة
            // عند الإلغاء (المشكلة 2 + تحصين الفحص L9 ضد تصادم ميلي ثانية)
            val now = freshNow()

            db.withTransaction {
                // إصلاح المشكلة 1: الزبون الجديد القادم من شاشة الصرف كان لا يُحفظ —
                // الآن يُنشأ داخل نفس المعاملة قبل أي عملية تعتمد عليه.
                if (existing == null) {
                    custDao.insertOrUpdate(base.copy(createdAt = now))
                }
                val toSell = cylDao.getAvailable(units)
                check(toSell.size == units) { "تعذّر تخصيص الأسطوانات — أعد المحاولة" }
                val ids = toSell.map { it.id }
                val updated = cylDao.markSoldByQuantity(units, now)
                check(updated == units) { "تعذّر خصم المخزون — أعد المحاولة" }

val amount = totalAmountNow
// إصلاح الفحص 4: المبلغ المدفوع وقت البيع — كامل أو جزئي أو صفر.
val amountPaid = paidNowPiasters
                saleDao.insert(
                    SaleEntity(
                        id = UUID.randomUUID().toString(),
                        customerId = custId,
                        customerName = base.name.trim(),
                        // إصلاح الفحص 33: تخزين JSON حقيقي ["id","id",...] — والقراءة
// (parseCylinderIds) تدعم الصفوف القديمة بصيغة CSV بلا هجرة.
cylinderIdsJson = ids.joinToString(",", "[", "]") { "\"$it\"" },
                        unitsSold = units,
                        pricePerUnit = pricePiasters,
                        totalAmount = amount,
                        amountPaid = amountPaid,
                        // إصلاح الفحص 4: الدفع الجزئي يُسجَّل CREDIT (عليه متبقٍ) حتى يسدد كاملاً.
status = if (amount - amountPaid <= 0L) SaleStatus.PAID else SaleStatus.CREDIT,
                        saleDate = now,
                        notes = notes.trim()
                    )
                )
                recomputeCustomer(custId)
            }
            refreshAll()
        }
    }

    // ===== تحصيل دَين =====
    fun recordCustomerPayment(customerId: String, amountPiasters: Long, notes: String,
                              onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
require(amountPiasters in 1..Money.MAX_AMOUNT) { "أدخل مبلغاً صحيحاً" }
val customer = custDao.getById(customerId) ?: error("الزبون غير موجود")
// إصلاح الفحص 7: السداد الزائد مسموح — الفارق يتحول رصيداً دائناً (سالب)
// لصالح الزبون عبر getCustomerBalance. (سداد المحطة فوق دَينها يبقى مرفوضاً.)
db.withTransaction {
payDao.insertValidated(
                    PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        customerId = customerId,
                        customerName = customer.name,
                        amount = amountPiasters,
                        paymentDate = freshNow(),
                        notes = notes.trim()
                    )
                )
                recomputeCustomer(customerId)
            }
            refreshAll()
        }
    }

    /** عكس (حذف) دفعة تحصيل خاطئة — يُعيد المبلغ ديناً على الزبون. */
    fun reverseCustomerPayment(paymentId: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            val p = payDao.getById(paymentId) ?: error("الدفعة غير موجودة")
            db.withTransaction {
                payDao.deleteById(paymentId)
                recomputeCustomer(p.customerId)
            }
            refreshAll()
        }
    }

    // ===== إلغاء بيع =====
    fun cancelSale(saleId: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            val sale = saleDao.getById(saleId) ?: error("البيع غير موجود")
            db.withTransaction {
                // إصلاح المشكلة 3: منع الإلغاء إذا وُجدت تحصيلات لاحقة لنفس الزبون —
                // وإلا فقد الزبون نقداً دُفع مقابل بيع اختفى من السجلات.
                val laterPayment = payDao.getByCustomer(sale.customerId)
                    .any { it.paymentDate > sale.saleDate }
                if (laterPayment)
                    error("يوجد تحصيلات لاحقة على هذا البيع. ألغِ التحصيلات أولاً ثم ألغِ البيع.")
                val ids = parseCylinderIds(sale.cylinderIdsJson)
                // إصلاح المشكلة 2: تُعاد فقط الأسطوانات ما زالت معلّمة بهذا البيع
                // (مطابقة soldDate) — أسطوانة أُعيد بيعها لاحقاً لا تُمس.
                // إصلاح الخطأ 2 (الجديد): التحقق من العدد المُعاد — إن نقص، تُلغى
                // المعاملة كاملة بدل حذف البيع وترك أسطوانات مفقودة للأبد.
                if (ids.isNotEmpty()) {
                    val restored = cylDao.markAvailable(ids, sale.saleDate)
                    check(restored == ids.size) {
                        "تعذّر إرجاع ${ids.size - restored} أسطوانة من هذا البيع — أُلغي الإلغاء"
                    }
                }
                saleDao.deleteById(saleId)
                recomputeCustomer(sale.customerId)
            }
            refreshAll()
        }
    }

    // ===== المحطة (المورد) =====
    fun purchaseFromStation(units: Int, costPiasters: Long, paidNowPiasters: Long,
                            notes: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            require(units in 1..Money.MAX_UNITS) { "أدخل عدداً صحيحاً (الحد الأقصى ${Money.format(Money.MAX_UNITS.toLong())})" }
            require(costPiasters in 1..Money.MAX_AMOUNT) {
                "التكلفة خارج النطاق المسموح (الحد الأقصى ${Money.format(Money.MAX_AMOUNT)} ريال)"
            }
            val total = units.toLong() * costPiasters
            // إصلاح الفحص H3: النطاق [0, total] — القيمة السالبة كانت تمرّ
            // (الفحص كان <= total فقط) فتضخّم دَين المحطة صامتاً.
            require(paidNowPiasters in 0..total) { "المدفوع خارج النطاق المسموح" }
            val now = freshNow()
            // إصلاح الخطأ 3: معرّف السحب يُولَّد أولاً ويُختم على كل أسطوانة —
            // الرابط الصريح يلغي التباس acquiredDate المتطابق بين سحبتين.
            val purchaseId = UUID.randomUUID().toString()
            db.withTransaction {
                val cylinders = List(units) {
                    CylinderEntity(
                        id = UUID.randomUUID().toString(),
                        sizeLiters = 20,
                        status = CylinderStatus.AVAILABLE,
                        acquiredFromStation = "محطة المورد",
                        acquisitionCost = costPiasters,
                        acquiredDate = now,
                        purchaseId = purchaseId
                    )
                }
                cylDao.insertAll(cylinders)
                stationDao.insertPurchase(
                    StationPurchaseEntity(
                        id = purchaseId,
                        units = units,
                        costPerUnit = costPiasters,
                        totalAmount = total,
                        amountPaid = paidNowPiasters,
                        purchaseDate = now,
                        notes = notes.trim()
                    )
                )
            }
            refreshAll()
        }
    }

    fun payStation(amountPiasters: Long, notes: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            require(amountPiasters in 1..Money.MAX_AMOUNT) { "أدخل مبلغاً صحيحاً" }
            val balance = stationDao.getStationBalance()
            if (balance <= 0) error("لا يوجد دَين للمحطة")
            if (amountPiasters > balance) error("المبلغ أكبر من دَين المحطة")
            db.withTransaction {
                stationDao.insertPayment(
                    StationPaymentEntity(
                        id = UUID.randomUUID().toString(),
                        amount = amountPiasters,
                        paymentDate = freshNow(),
                        notes = notes.trim()
                    )
                )
            }
            refreshAll()
        }
    }

    /**
     * إصلاح المشكلة 11: إلغاء سحب من المحطة — يُسمح فقط إذا كانت أسطواناته
     * ما زالت في المخزون (لم تُبَع بعد)، وتُحذف الأسطوانات مع سجل السحب
     * داخل معاملة واحدة حتى لا يختل الرصيد.
     */
    fun cancelStationPurchase(purchaseId: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            val p = stationDao.getPurchaseById(purchaseId) ?: error("السحب غير موجود")
            db.withTransaction {
                // إصلاح الخطأ 3: البحث بـ purchaseId الصريح بدل acquiredDate الغامض
                val ids = cylDao.getAvailableIdsByPurchase(purchaseId, p.units)
                if (ids.size < p.units)
                    error("لا يمكن الإلغاء — بعض أسطوانات هذا السحب بِيعت بالفعل. ألغِ تلك المبيعات أولاً.")
                cylDao.deleteByIds(ids)
                stationDao.deletePurchaseById(purchaseId)
            }
            refreshAll()
        }
    }

    /** إصلاح المشكلة 11: إلغاء تسديد للمحطة — يعود المبلغ ديناً على المحل. */
    fun cancelStationPayment(paymentId: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            stationDao.getPaymentById(paymentId) ?: error("التسديد غير موجود")
            db.withTransaction {
                stationDao.deletePaymentById(paymentId)
            }
            refreshAll()
        }
    }

    suspend fun getStationData(): StationData {
        val purchases = stationDao.getPurchases()
        val payments = stationDao.getPayments()
        val totalPurchases = stationDao.getTotalPurchases()
        val balance = stationDao.getStationBalance()
        return StationData(purchases, payments, totalPurchases, balance)
    }

    // ===== نسخ احتياطي / استعادة =====
    suspend fun exportDatabase(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = getApplication<Application>()
            // إصلاح الخطأ 11: التحقق من نجاح الـ checkpoint — تصدير بلا دمج WAL
            // قد يُنتج نسخة احتياطية ناقصة البيانات.
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                if (cursor.moveToFirst()) {
                    val busy = cursor.getInt(0) // 0 = اكتمل، غير ذلك = مشغول/خطأ
                    if (busy != 0) error("تعذّر تجهيز القاعدة للتصدير (checkpoint مشغول) — أعد المحاولة")
                }
            }
            val src = ctx.getDatabasePath(AppDatabase.DB_NAME)
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            } ?: error("تعذّر فتح ملف الوجهة")
        }.exceptionOrNull()?.message
    }

    suspend fun importDatabase(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = getApplication<Application>()
            // إصلاح المشكلة 4 (السابقة): إغلاق القاعدة وتصفير الـ Singleton قبل
            // أي كتابة — النسخ فوق قاعدة مفتوحة يفسد الملف.
            AppDatabase.shutdown()
            val dst = ctx.getDatabasePath(AppDatabase.DB_NAME)
            val incoming = File(dst.path + ".incoming")
            val backup = File(dst.path + ".bak")
            // إصلاح الفحص M4: الملف المستورد يُكتب مؤقتاً أولاً — لا نلمس
            // القاعدة الحية حتى يثبت صلاحية النسخة.
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                incoming.outputStream().use { input.copyTo(it) }
            } ?: error("تعذّر فتح ملف النسخة")
            validateBackupFile(incoming)?.let { problem ->
                incoming.delete()
                error(problem)
            }
            // نسخة الأمان القديمة تُحذف (الحالية سليمة الآن وسيؤمَّن ما بعدها).
            backup.delete()
            if (dst.exists()) dst.copyTo(backup, overwrite = true)
            // القاعدة تعمل بوضع WAL — حذف الملفين المساعدين وإلا أُفسدت النسخة الجديدة.
            File(dst.path + "-wal").delete()
            File(dst.path + "-shm").delete()
            if (!incoming.renameTo(dst)) incoming.copyTo(dst, overwrite = true)
            incoming.delete()
            // إعادة تشغيل التطبيق ليفتح القاعدة الجديدة من الصفر بحالة نظيفة.
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } ?: error("تعذّر تحضير إعادة التشغيل")
            ctx.startActivity(intent)
            // إصلاح الخطأ 4: ‏startActivity غير متزامن — الخروج الفوري قد يقتل
            // العملية قبل انطلاق النشاط الجديد. إصلاح الفحص L14: مهلة ثانية
            // كاملة (500ms قصرت على الأجهزة البطيئة فغلق التطبيق بلا إعادة فتح).
            Handler(Looper.getMainLooper()).postDelayed({
                Runtime.getRuntime().exit(0)
            }, 1000)
        }.exceptionOrNull()?.message
    }

    /**
     * إصلاح الفحص M4: فحص النسخة قبل الكتابة — ملف تالف أو غير صالح (المخزن
     * يقبل أي نوع من الملفات) لم يعد يمسح البيانات الأصلية.
     * يعيد رسالة المشكلة، أو null إن كانت صالحة.
     */
    private fun validateBackupFile(file: File): String? {
        if (!file.exists() || file.length() == 0L) return "ملف النسخة الاحتياطية فارغ"
        return try {
            android.database.sqlite.SQLiteDatabase
                .openDatabase(file.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
                .use { db ->
                    val ok = db.rawQuery("PRAGMA integrity_check", null).use { c ->
                        c.moveToFirst() && c.getString(0) == "ok"
                    }
                    if (!ok) return@use "ملف النسخة فاسد (فحص السلامة فشل)"
                    val version = db.rawQuery("PRAGMA user_version", null).use { c ->
                        if (c.moveToFirst()) c.getInt(0) else 0
                    }
                    if (version !in 1..AppDatabase.VERSION)
                        return@use "إصدار النسخة غير مدعوم (v$version — هذا التطبيق يدعم v1 إلى v${AppDatabase.VERSION})"
                    val tables = db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN " +
                            "('cylinders','customers','sales','payments','station_purchases','station_payments')",
                        null
                    ).use { c ->
                        val s = mutableSetOf<String>()
                        while (c.moveToNext()) s.add(c.getString(0))
                        s
                    }
                    if (tables.size != 6)
                        return@use "ملف النسخة ناقص الجداول (المتوقع 6 — وُجد ${tables.size})"
                    null
                }
        } catch (e: Exception) {
            "الملف المحدد ليس قاعدة بيانات صالحة"
        }
    }

    // ===== أدوات داخلية =====

    /**
     * إصلاح الفحص 33: قراءة cylinderIdsJson بصيغة JSON الحقيقية،
     * مع دعم الصفوف القديمة (CSV: "id,id") بلا هجرة — UUID لا يحتوي
     * فواصل ولا علامات اقتباس فالتحليل آمن حرفياً.
     */
    private fun parseCylinderIds(stored: String): List<String> {
        val t = stored.trim()
        return if (t.startsWith("[")) {
            t.removePrefix("[").removeSuffix("]")
                .split(",").map { it.trim().trim('"') }.filter { it.isNotBlank() }
        } else {
            t.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
    }

    /**
     * طابع زمني فريد ومتزايد (حماية من تصادم ميلي ثانية):
     * فحص L9 أثبت أن بيعين بنفس soldDate يفشلان في عزل الإلغاء، وأن تحصيل
     * بنفس طابع البيع لا يُعد «لاحقاً». عبر الواجهة غير قابل للتحقيق
     * عملياً، لكن هذا السطر يغلق الفجوة جذرياً بلا تغيير مخطط.
     */
    private var lastTimestamp = 0L

    private fun freshNow(): Long = synchronized(this) {
        val next = maxOf(System.currentTimeMillis(), lastTimestamp + 1)
        lastTimestamp = next
        next
    }

    private suspend fun recomputeCustomer(id: String) {
        val c = custDao.getById(id) ?: return
        val debt = saleDao.getCustomerSalesTotal(id)
        val paid = saleDao.getCustomerSalePaid(id) + payDao.getCustomerCollections(id)
        // إصلاح P0: UPDATE بدل INSERT OR REPLACE — REPLACE حذف ضمني محجوب
        // بـ RESTRICT على sales، فكان كل بيع/تحصيل/إلغاء يسقط كاملاً.
        custDao.update(c.copy(totalDebt = debt, totalPaid = paid))
    }

    /** إصلاح الخطأ 8: تُعيد Job — فيمكن للانتظار (join) في الاختبارات أن يترجم. */
    private fun launch(block: suspend () -> Unit): Job =
        viewModelScope.launch {
            try {
                busy = true
                block()
                errorMessage = null
            } catch (t: Throwable) {
                errorMessage = t.message ?: "حدث خطأ غير متوقع"
            } finally {
                busy = false
            }
        }

    /** حاجز تزامن ذرّي: يمنع تشغيل عمليتين معدِّلتين معاً (إصلاح المشكلة 15).
     *  mutableStateOf وحده لا يكفي — الضغطتان السريعان كانتا تتجاوزانه. */
    private val opRunning = AtomicBoolean(false)

    private suspend fun runOp(onResult: (String?) -> Unit, block: suspend () -> Unit) {
        if (!opRunning.compareAndSet(false, true)) {
            onResult("توجد عملية قيد التنفيذ — انتظر لحظة")
            return
        }
        var err: String? = null
        try {
            block()
        } catch (t: Throwable) {
            err = t.message ?: "حدث خطأ"
            errorMessage = err
        } finally {
            opRunning.set(false)
        }
        onResult(err)
    }
}

data class CustomerDetail(
    val customer: CustomerEntity?,
    val sales: List<SaleEntity>,
    val payments: List<PaymentEntity>,
    val totalBought: Long,
    val totalPaid: Long,
    val balance: Long
)

data class StationData(
    val purchases: List<StationPurchaseEntity>,
    val payments: List<StationPaymentEntity>,
    val totalPurchases: Long,
    val balance: Long
)
