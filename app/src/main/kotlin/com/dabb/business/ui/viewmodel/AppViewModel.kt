package com.dabb.business.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dabb.business.data.local.AppDatabase
import com.dabb.business.model.CylinderEntity
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.SaleEntity
import kotlinx.coroutines.launch

/**
 * ViewModel موحد — يربط الواجهة بقاعدة البيانات بدون تسرب ذاكرة
 * يدعم: المخزون + الإحصائيات + آخر الحركات + أعلى المدينين
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app.applicationContext)
    private val cylDao = db.cylinderDao()
    private val custDao = db.customerDao()
    private val saleDao = db.saleDao()

    // حالة واجهة المخزون
    var availableCount by mutableStateOf(0)
        private set
    var soldCount by mutableStateOf(0)
        private set

    // حالة الدخل والدين
    var totalPaid by mutableStateOf(0.0)
        private set
    var totalCredit by mutableStateOf(0.0)
        private set

    // آخر الحركات (لشاشة المخزون)
    var recentSales by mutableStateOf<List<SaleEntity>>(emptyList())
        private set

    // أعلى الزبائن ديناً (لشاشة التقارير)
    var topDebtors by mutableStateOf<List<CustomerEntity>>(emptyList())
        private set

    init {
        refreshInventory()
        refreshStats()
        refreshRecent()
        refreshDebtors()
    }

    fun refreshInventory() {
        viewModelScope.launch {
            availableCount = cylDao.getAvailableCount()
            soldCount = cylDao.getSoldCount()
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            totalPaid = saleDao.getTotalPaid() ?: 0.0
            totalCredit = saleDao.getTotalCredit() ?: 0.0
        }
    }

    fun refreshRecent() {
        viewModelScope.launch {
            recentSales = saleDao.getAll().take(6)
        }
    }

    fun refreshDebtors() {
        viewModelScope.launch {
            topDebtors = custDao.getCustomersWithDebt()
                .sortedByDescending { it.currentBalance() }
                .take(3)
        }
    }

    /** إضافة أسطوانة جديدة */
    fun addCylinder(c: CylinderEntity) {
        viewModelScope.launch {
            cylDao.insert(c)
            refreshInventory()
        }
    }

    /** صرف أسطوانات — يُحدّث المخزون + يسجل البيع + يحدّث الدين */
    fun dispenseAndRecord(
        cylinderIds: List<String>,
        customer: CustomerEntity,
        sale: SaleEntity
    ) {
        viewModelScope.launch {
            // ١. صرف الأسطوانات من المخزون
            cylDao.markSold(cylinderIds)
            // ٢. تحديث أو إنشاء الزبون
            custDao.insertOrUpdate(customer)
            // ٣. تسجيل البيع
            saleDao.insert(sale)
            // ٤. تحديث كل الإحصائيات
            refreshInventory()
            refreshStats()
            refreshRecent()
            refreshDebtors()
        }
    }

    suspend fun searchCustomers(q: String): List<CustomerEntity> = custDao.search(q)
    suspend fun getCustomer(id: String): CustomerEntity? = custDao.getById(id)
}
