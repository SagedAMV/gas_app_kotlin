package com.dabb.business.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * أسطوانة غاز 20 لتر.
 * المخزون يُدار بالكمية: حالة AVAILABLE (متوفرة) أو SOLD (مباعة).
 * التكلفة بالقروش (Long)؛ soldDate تُضبط عند البيع لحساب ربح فترة زمنية.
 */
@Entity(tableName = "cylinders")
data class CylinderEntity(
    @PrimaryKey val id: String,
    val sizeLiters: Int = 20,
    val status: CylinderStatus = CylinderStatus.AVAILABLE,
    val acquiredFromStation: String = "",
    val acquisitionCost: Long = 0L,
    val acquiredDate: Long = 0L,
    val soldDate: Long = 0L
)
