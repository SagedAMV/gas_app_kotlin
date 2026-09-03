package com.dabb.business.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
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

        /**
         * إصلاح المشكلة 5: أُزيل fallbackToDestructiveMigration() تماماً —
         * أي تغيير مخطط مستقبلي بلا Migration صريحة سيفشل بصوت مرتفع
         * بدل أن يمحو ديون الزبائن والمخزون بصمت.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * إغلاق القاعدة وتصفير الـ Singleton — يُستدعى قبل استبدال ملف القاعدة
         * عند استيراد نسخة احتياطية (إصلاح المشكلة 4: الكتابة فوق قاعدة مفتوحة تفسدها).
         */
        fun shutdown() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /**
         * ترقية 2 → 3 (إصلاح المشكلة 8): إعادة بناء جدول sales بمفتاح أجنبي
         * نحو customers.id — حماية على مستوى القاعدة من مبيعات لزبائن وهمية.
         * ملاحظة: مفاتيح SQLite الأجنبية لا تُضاف بـ ALTER، لذا يُعاد بناء الجدول
         * (إنشاء جديد ← نسخ البيانات ← حذف القديم ← إعادة تسمية).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sales_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`customerId` TEXT NOT NULL, " +
                        "`customerName` TEXT NOT NULL, " +
                        "`cylinderIdsJson` TEXT NOT NULL, " +
                        "`unitsSold` INTEGER NOT NULL, " +
                        "`pricePerUnit` INTEGER NOT NULL, " +
                        "`totalAmount` INTEGER NOT NULL, " +
                        "`amountPaid` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`saleDate` INTEGER NOT NULL, " +
                        "`notes` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE RESTRICT)"
                )
                db.execSQL(
                    "INSERT INTO sales_new (id, customerId, customerName, cylinderIdsJson, " +
                        "unitsSold, pricePerUnit, totalAmount, amountPaid, status, saleDate, notes) " +
                        "SELECT id, customerId, customerName, cylinderIdsJson, " +
                        "unitsSold, pricePerUnit, totalAmount, amountPaid, status, saleDate, notes FROM sales"
                )
                db.execSQL("DROP TABLE sales")
                db.execSQL("ALTER TABLE sales_new RENAME TO sales")
                // فهرس المفتاح الأجنبي — يمنع فحص الجدول كاملاً (تحذير Room)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)")
            }
        }

        /**
         * ترقية 3 → 4 (إصلاح الخطأ 3): عمود purchaseId في cylinders يربط كل
         * أسطوانة بسجل السحب من المحطة — إلغاء سحب لم يعد يعتمد على acquiredDate
         * الذي قد يتطابق بين سحبتين. ملاحظة صدق: إضافة حقل لكيان Room تغيّر
         * المخطط حكماً، لذا هذه الهجرة ضرورية رغم أن القيمة الافتراضية في Kotlin.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cylinders ADD COLUMN purchaseId TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
