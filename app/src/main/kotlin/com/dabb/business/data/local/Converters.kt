package com.dabb.business.data.local

import androidx.room.TypeConverter

/**
 * تحويل القوائم النصية — لتخزين cylinderIds في SQLite
 * المهارة: ㊹ (Type Converter Pattern) + ㊷ (SOLID)
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",")
}
