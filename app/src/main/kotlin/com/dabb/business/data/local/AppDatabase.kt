package com.dabb.business.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dabb.business.model.CylinderEntity
import com.dabb.business.model.CustomerEntity
import com.dabb.business.model.PaymentEntity
import com.dabb.business.model.SaleEntity
import com.dabb.business.model.StationPaymentEntity
import com.dabb.business.model.StationPurchaseEntity

@Database(
    entities = [
        CylinderEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        PaymentEntity::class,
        StationPurchaseEntity::class,
        StationPaymentEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cylinderDao(): CylinderDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun stationDao(): StationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        const val DB_NAME = "gas_db.sqlite"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // قبل مرحلة البيانات الحقيقية — يُعاد بناء القاعدة عند تغيّر المخطط.
                    // لاحقاً يُستبدل بترقية Migration صريحة مع وجود بيانات مستخدمين.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
