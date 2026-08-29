package com.dabb.business.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dabb.business.model.CylinderEntity
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.SaleEntity

@Database(
    entities = [CylinderEntity::class, CustomerEntity::class, SaleEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cylinderDao(): CylinderDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gas_db.sqlite"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
