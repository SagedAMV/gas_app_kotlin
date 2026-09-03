package com.dabb.business.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** فترة التقرير الزمنية. */
enum class ReportPeriod(val label: String) {
    ALL("الكل"), MONTH("الشهر"), WEEK("الأسبوع"), DAY("اليوم");

    fun startMillis(now: Long = System.currentTimeMillis()): Long = when (this) {
        ALL -> 0L
        DAY -> now - 24L * 3600 * 1000
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
        totalCredit = salesTotal - totalPaid
        totalCost = cost
        profit = salesTotal - cost
        stationBalance = stationDao.getStationBalance()
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
                custDao.insertOrUpdate(c.copy(name = name.trim(), phone = phone.trim()))
            }
            refreshCustomers(); refreshDebtors()
        }
    }

    fun deleteCustomer(id: String, onResult: (String?) -> Unit = {}) = launch {
        runOp(onResult) {
            db.withTransaction {
                val sales = saleDao.getByCustomer(id)
                val payments = payDao.getByCustomer(id)
                if (sales.isNotEmpty() || payments.isNotEmpty())
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
        payNow: Boolean, notes: String, onResult: (String?) -> Unit = {}
    ) = launch {
        runOp(onResult) {
            require(units > 0 && pricePiasters > 0) { "أدخل عدداً وسعراً صحيحين" }
            val available = cylDao.getAvailableCount()
            if (available < units) error("المخزون لا يكفي — المتوفر $available أسطوانة فقط")

            val existing = custDao.getById(customer.id) ?: custDao.findByName(customer.name.trim())
            val custId = existing?.id ?: customer.id
            val base = existing ?: customer
            // طابع زمني موحّد للأسطوانات والبيع — أساس المطابقة الدقيقة عند الإلغاء (المشكلة 2)
            val now = System.currentTimeMillis()

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

                val amount = units.toLong() * pricePiasters
                val amountPaid = if (payNow) amount else 0L
                saleDao.insert(
                    SaleEntity(
                        id = UUID.randomUUID().toString(),
                        customerId = custId,
                        customerName = base.name.trim(),
                        cylinderIdsJson = ids.joinToString(","),
                        unitsSold = units,
                        pricePerUnit = pricePiasters,
                        totalAmount = amount,
                        amountPaid = amountPaid,
                        status = if (payNow) SaleStatus.PAID else SaleStatus.CREDIT,
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
            require(amountPiasters > 0) { "أدخل مبلغاً أكبر من صفر" }
            val customer = custDao.getById(customerId) ?: error("الزبون غير موجود")
            val balance = saleDao.getCustomerBalance(customerId)
            if (balance <= 0) error("لا يوجد دَين على هذا الزبون")
            if (amountPiasters > balance) error("المبلغ أكبر من الدين المتبقي")
            db.withTransaction {
                payDao.insert(
                    PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        customerId = customerId,
                        customerName = customer.name,
                        amount = amountPiasters,
                        paymentDate = System.currentTimeMillis(),
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
                val ids = sale.cylinderIdsJson.split(",").filter { it.isNotBlank() }
                // إصلاح المشكلة 2: تُعاد فقط الأسطوانات ما زالت معلّمة بهذا البيع
                // (مطابقة soldDate) — أسطوانة أُعيد بيعها لاحقاً لا تُمس.
                if (ids.isNotEmpty()) cylDao.markAvailable(ids, sale.saleDate)
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
            require(units > 0 && costPiasters > 0) { "أدخل عدداً وتكلفة صحيحين" }
            val total = units.toLong() * costPiasters
            require(paidNowPiasters <= total) { "المدفوع أكبر من الإجمالي" }
            val now = System.currentTimeMillis()
            db.withTransaction {
                val cylinders = List(units) {
                    CylinderEntity(
                        id = UUID.randomUUID().toString(),
                        sizeLiters = 20,
                        status = CylinderStatus.AVAILABLE,
                        acquiredFromStation = "محطة المورد",
                        acquisitionCost = costPiasters,
                        acquiredDate = now
                    )
                }
                cylDao.insertAll(cylinders)
                stationDao.insertPurchase(
                    StationPurchaseEntity(
                        id = UUID.randomUUID().toString(),
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
            require(amountPiasters > 0) { "أدخل مبلغاً أكبر من صفر" }
            val balance = stationDao.getStationBalance()
            if (balance <= 0) error("لا يوجد دَين للمحطة")
            if (amountPiasters > balance) error("المبلغ أكبر من دَين المحطة")
            db.withTransaction {
                stationDao.insertPayment(
                    StationPaymentEntity(
                        id = UUID.randomUUID().toString(),
                        amount = amountPiasters,
                        paymentDate = System.currentTimeMillis(),
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
                val ids = cylDao.getAvailableIdsByAcquiredDate(p.purchaseDate, p.units)
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
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
            val src = ctx.getDatabasePath(AppDatabase.DB_NAME)
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            } ?: error("تعذّر فتح ملف الوجهة")
        }.exceptionOrNull()?.message
    }

    suspend fun importDatabase(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = getApplication<Application>()
            // إصلاح المشكلة 4: إغلاق القاعدة وتصفير الـ Singleton قبل أي كتابة —
            // النسخ فوق قاعدة مفتوحة يفسد الملف.
            AppDatabase.shutdown()
            val dst = ctx.getDatabasePath(AppDatabase.DB_NAME)
            // القاعدة تعمل بوضع WAL — حذف الملفين المساعدين وإلا أُفسدت النسخة الجديدة.
            File(dst.path + "-wal").delete()
            File(dst.path + "-shm").delete()
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                dst.outputStream().use { input.copyTo(it) }
            } ?: error("تعذّر فتح ملف النسخة")
            // إعادة تشغيل التطبيق ليفتح القاعدة الجديدة من الصفر بحالة نظيفة.
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            } ?: error("تعذّر تحضير إعادة التشغيل")
            ctx.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }.exceptionOrNull()?.message
    }

    // ===== أدوات داخلية =====
    private suspend fun recomputeCustomer(id: String) {
        val c = custDao.getById(id) ?: return
        val debt = saleDao.getCustomerSalesTotal(id)
        val paid = saleDao.getCustomerSalePaid(id) + payDao.getCustomerCollections(id)
        custDao.insertOrUpdate(c.copy(totalDebt = debt, totalPaid = paid))
    }

    private fun launch(block: suspend () -> Unit) {
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
