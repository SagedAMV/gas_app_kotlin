package com.dabb.business.data.local

import androidx.room.TypeConverter
import com.dabb.business.model.CylinderStatus
import com.dabb.business.model.SaleStatus

/**
 * تحويلات Room: حالات enum كاسم الحالة.
 * (محوّلا List<String> كانا كوداً ميتاً — الكيانات تخزن cylinderIdsJson نصاً
 *  مباشرة — أُزيلتا في الفحص L11.)
 */
class Converters {
    @TypeConverter
    fun cylinderStatusToString(s: CylinderStatus): String = s.name

    @TypeConverter
    fun stringToCylinderStatus(v: String): CylinderStatus =
        runCatching { CylinderStatus.valueOf(v) }.getOrDefault(CylinderStatus.AVAILABLE)

    @TypeConverter
    fun saleStatusToString(s: SaleStatus): String = s.name

    @TypeConverter
    fun stringToSaleStatus(v: String): SaleStatus =
        runCatching { SaleStatus.valueOf(v) }.getOrDefault(SaleStatus.CREDIT)
}
