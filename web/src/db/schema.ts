import {
  bigint,
  check,
  index,
  integer,
  pgTable,
  text,
} from "drizzle-orm/pg-core";
import { sql } from "drizzle-orm";
import type { CylinderStatus, SaleStatus } from "@/lib/types";

/**
 * مخطط قاعدة البيانات — نسخة ويب من تطبيق الغاز.
 *
 * منقول بدقة عن مخطط Room الإصدار 5 في تطبيق Kotlin (تفكير §31 — المبادئ الأولى):
 *  - كل المبالغ أعداد صحيحة بالريال اليمني (لا كسور في السوق).
 *  - جداول: الزبائن، الأسطوانات، المبيعات، التحصيلات، سحوبات المحطة، تسديدات المحطة.
 *  - مفاتيح أجنبية RESTRICT على sales/payments (العيب 8 والعيب 11): لا مبيعات
 *    لزبائن وهمية ولا تحصيلات يتيمة، ولا حذف لزبون له عمليات.
 *  - قيود CHECK (MIGRATION_4_5): amount > 0، units بين 1 و10000، المبالغ >= 0.
 *  - أسماء الأعمدة بالسجل تبقى مطابقة لتطبيق Kotlin (قابلة للنسخ الاحتياطي ذهنياً).
 */

export const customers = pgTable("customers", {
  id: text("id").primaryKey(),
  name: text("name").notNull().default(""),
  phone: text("phone").notNull().default(""),
  // كاش يُعاد حسابه من المعاملات داخل معاملة واحدة (كما في تطبيق Kotlin) —
  // المصدر الموثوق للرصيد هو المجاميع المشتقة من المبيعات والتحصيلات.
  totalDebt: bigint("total_debt", { mode: "number" }).notNull().default(0),
  totalPaid: bigint("total_paid", { mode: "number" }).notNull().default(0),
  createdAt: bigint("created_at", { mode: "number" }).notNull().default(0),
});

export const cylinders = pgTable(
  "cylinders",
  {
    id: text("id").primaryKey(),
    sizeLiters: integer("size_liters").notNull().default(20),
    /** AVAILABLE أو SOLD — نص صريح كما في Room. */
    status: text("status").$type<CylinderStatus>().notNull().default("AVAILABLE"),
    acquiredFromStation: text("acquired_from_station").notNull().default(""),
    acquisitionCost: bigint("acquisition_cost", { mode: "number" })
      .notNull()
      .default(0),
    acquiredDate: bigint("acquired_date", { mode: "number" })
      .notNull()
      .default(0),
    soldDate: bigint("sold_date", { mode: "number" }).notNull().default(0),
    /** ربط صريح بسجل سحب المحطة — أساس إلغاء السحب بأمان (إصلاح الخطأ 3). */
    purchaseId: text("purchase_id").notNull().default(""),
  },
  (t) => [
    index("cylinders_status_idx").on(t.status),
    index("cylinders_purchase_idx").on(t.purchaseId),
    check("cylinders_status_valid", sql`status IN ('AVAILABLE','SOLD')`),
    check("cylinders_cost_nonneg", sql`acquisition_cost >= 0`),
  ]
);

export const sales = pgTable(
  "sales",
  {
    id: text("id").primaryKey(),
    customerId: text("customer_id")
      .notNull()
      .references(() => customers.id, { onDelete: "restrict" }),
    customerName: text("customer_name").notNull().default(""),
    /** JSON حقيقي ["id","id",...] مع دعم قراءة CSV القديم (إصلاح الفحص 33). */
    cylinderIdsJson: text("cylinder_ids_json").notNull().default(""),
    unitsSold: integer("units_sold").notNull().default(0),
    pricePerUnit: bigint("price_per_unit", { mode: "number" })
      .notNull()
      .default(0),
    totalAmount: bigint("total_amount", { mode: "number" })
      .notNull()
      .default(0),
    amountPaid: bigint("amount_paid", { mode: "number" }).notNull().default(0),
    /** PAID أو CREDIT. */
    status: text("status").$type<SaleStatus>().notNull().default("CREDIT"),
    saleDate: bigint("sale_date", { mode: "number" }).notNull().default(0),
    notes: text("notes").notNull().default(""),
  },
  (t) => [
    index("sales_customer_idx").on(t.customerId),
    index("sales_date_idx").on(t.saleDate),
    check("sales_units_positive", sql`units_sold > 0`),
    check("sales_amounts_nonneg", sql`total_amount >= 0 AND amount_paid >= 0`),
    check("sales_status_valid", sql`status IN ('PAID','CREDIT')`),
  ]
);

export const payments = pgTable(
  "payments",
  {
    id: text("id").primaryKey(),
    customerId: text("customer_id")
      .notNull()
      .references(() => customers.id, { onDelete: "restrict" }),
    customerName: text("customer_name").notNull().default(""),
    amount: bigint("amount", { mode: "number" }).notNull(),
    paymentDate: bigint("payment_date", { mode: "number" }).notNull().default(0),
    notes: text("notes").notNull().default(""),
  },
  (t) => [
    index("payments_customer_idx").on(t.customerId),
    check("payments_amount_positive", sql`amount > 0`),
  ]
);

export const stationPurchases = pgTable(
  "station_purchases",
  {
    id: text("id").primaryKey(),
    units: integer("units").notNull().default(0),
    costPerUnit: bigint("cost_per_unit", { mode: "number" }).notNull().default(0),
    totalAmount: bigint("total_amount", { mode: "number" }).notNull().default(0),
    amountPaid: bigint("amount_paid", { mode: "number" }).notNull().default(0),
    purchaseDate: bigint("purchase_date", { mode: "number" }).notNull().default(0),
    notes: text("notes").notNull().default(""),
  },
  (t) => [
    check("sp_units_range", sql`units > 0 AND units <= 10000`),
    check("sp_amounts_nonneg", sql`cost_per_unit >= 0 AND total_amount >= 0 AND amount_paid >= 0`),
  ]
);

export const stationPayments = pgTable(
  "station_payments",
  {
    id: text("id").primaryKey(),
    amount: bigint("amount", { mode: "number" }).notNull(),
    paymentDate: bigint("payment_date", { mode: "number" }).notNull().default(0),
    notes: text("notes").notNull().default(""),
  },
  (t) => [check("st_amount_positive", sql`amount > 0`)]
);

/**
 * إعدادات عامة (بدل SharedPreferences في Kotlin): السعر الافتراضي، رمز القفل
 * (ملح + بصمة)، عدّاد محاولات القفل، والطابع الزمني الأخير للتسلسل المتزايد.
 */
export const settings = pgTable("settings", {
  key: text("key").primaryKey(),
  value: text("value").notNull().default(""),
});
