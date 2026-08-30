package com.dabb.business.data.local

import androidx.room.TypeConverter
import com.dabb.business.model.CylinderStatus
import com.dabb.business.model.SaleStatus

/**
 * تحويلات Room: قوائم المعرّفات (لإرجاع/تتبّع أسطوانات عملية ملغاة) + الحالات enum.
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",").filter { it.isNotBlank() }

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
