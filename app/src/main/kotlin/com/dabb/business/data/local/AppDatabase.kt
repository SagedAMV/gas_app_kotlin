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
    version = AppDatabase.VERSION,
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

        /** رقم مخطط Room — يُستخدم في @Database وفي فحص النسخ الاحتياطية. */
        const val VERSION = 5

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
         * ترقية 1 → 2 (إصلاح الفحص H2): الإصدار 1 لم يكن فيه سوى 3 جداول
         * (cylinders, customers, sales). في v2 أُضيفت جداول payments و
         * station_purchases وstation_payments، وأُضيف عمود soldDate إلى
         * cylinders (لحساب ربح الفترة). الأعمدة الأخرى بأسماء مطابقة
         * (المبالغ REAL قديماً ← تقرأ كأعداد صحيحة بأمان عبر نوع SQLite).
         * بدون هذه الهجرة كان أي جهاز بـ v1 يسقط عند كل فتح (RoomException)
         * لأن fallbackToDestructiveMigration أُزيل عمداً.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cylinders ADD COLUMN soldDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments` (" +
                        "`id` TEXT NOT NULL, `customerId` TEXT NOT NULL, " +
                        "`customerName` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                        "`paymentDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `station_purchases` (" +
                        "`id` TEXT NOT NULL, `units` INTEGER NOT NULL, " +
                        "`costPerUnit` INTEGER NOT NULL, `totalAmount` INTEGER NOT NULL, " +
                        "`amountPaid` INTEGER NOT NULL, `purchaseDate` INTEGER NOT NULL, " +
                        "`notes` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `station_payments` (" +
                        "`id` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                        "`paymentDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
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

        /**
         * ترقية 4 → 5 (العيب 11 — دفاع في العمق على مستوى المخطط):
         * - payments: مفتاح أجنبي نحو customers (RESTRICT) + فهرس customerId
         *   + CHECK(amount > 0) — كانت الجداول الستة بلا مفتاح أجنبي واحد
         *   (عدا sales) وبلا قيد CHECK واحد، وكل الدفاع في طبقة التطبيق فقط.
         * - station_purchases: CHECK(units بين 1 و10000) + CHECK(المبالغ >= 0).
         * المفاتيح والقيود لا تُضاف بـ ALTER ⇒ إعادة بناء الجدولين (نمط MIGRATION_2_3).
         * ملاحظة سلامة: صف يخالف القيود الجديدة يفشل النسخ بصوت مرتفع —
         * وهذا مقصود (فلسفة المشروع: لا فقدان بيانات صامت). بيانات التطبيق
         * تمر كلها عبر مسارات مُتحقِّقة (insertValidated / require) فلا يحدث عملياً،
         * والاستثناء الوحيد (نسخة تالفة مستوردة) يوقفها فحص probeWithRoom قبل الوصول هنا.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // تحصيلات يتيمة (ممكنة فقط من ملف تالف) — تنظيف قبل بناء FK
                db.execSQL("DELETE FROM payments WHERE customerId NOT IN (SELECT id FROM customers)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments_new` (" +
                        "`id` TEXT NOT NULL, `customerId` TEXT NOT NULL, " +
                        "`customerName` TEXT NOT NULL, `amount` INTEGER NOT NULL CHECK(`amount` > 0), " +
                        "`paymentDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE RESTRICT)"
                )
                db.execSQL(
                    "INSERT INTO payments_new (id, customerId, customerName, amount, paymentDate, notes) " +
                        "SELECT id, customerId, customerName, amount, paymentDate, notes FROM payments"
                )
                db.execSQL("DROP TABLE payments")
                db.execSQL("ALTER TABLE payments_new RENAME TO payments")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_customerId` ON `payments` (`customerId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `station_purchases_new` (" +
                        "`id` TEXT NOT NULL, `units` INTEGER NOT NULL " +
                        "CHECK(`units` > 0 AND `units` <= 10000), " +
                        "`costPerUnit` INTEGER NOT NULL CHECK(`costPerUnit` >= 0), " +
                        "`totalAmount` INTEGER NOT NULL CHECK(`totalAmount` >= 0), " +
                        "`amountPaid` INTEGER NOT NULL CHECK(`amountPaid` >= 0), " +
                        "`purchaseDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT INTO station_purchases_new (id, units, costPerUnit, totalAmount, amountPaid, purchaseDate, notes) " +
                        "SELECT id, units, costPerUnit, totalAmount, amountPaid, purchaseDate, notes FROM station_purchases"
                )
                db.execSQL("DROP TABLE station_purchases")
                db.execSQL("ALTER TABLE station_purchases_new RENAME TO station_purchases")
            }
        }
    }
}
