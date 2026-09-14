/**
 * منطق الأعمال — إعادة بناء أمينة لـ AppViewModel.kt في نسخة الويب.
 *
 * المبادئ المحفوظة من تطبيق Kotlin (تفكير §49 — الأولوية: يعمل ← آمن ← سريع ← جميل):
 *  1. كل عملية مالية/مخزونية داخل معاملة قاعدة بيانات واحدة (تنجح كاملة أو تُلغى).
 *  2. التحقق من المدخلات قبل أي كتابة، برسائل عربية قابلة للتصرف.
 *  3. الرصيد الموثوق يُشتق من المعاملات (المبيعات + التحصيلات) لا من الكاش وحده.
 *  4. لا فقدان بيانات صامت: الإلغاءات محروسة بمطابقة دقيقة.
 *
 * تحسينات ويب (موثقة في changes.html):
 *  - قفل ذرّي على مستوى الصفوف (FOR UPDATE) بدل حاجز الذاكرة — الخادم قد يتعدد.
 *  - منتصف الليل المحلي محسوب على توقيت عدن (UTC+3 بلا صيفي) بدل توقيت الخادم.
 */
import { createHash, randomBytes } from "node:crypto";
import { asc, desc, eq, inArray, and, gte, sql } from "drizzle-orm";
import { db } from "@/db";
import {
  customers,
  cylinders,
  payments,
  sales,
  settings,
  stationPayments,
  stationPurchases,
} from "@/db/schema";
import { MAX_AMOUNT, MAX_UNITS } from "./money";
import type {
  AppState,
  Customer,
  CustomerDetail,
  ReportPeriod,
  Sale,
  StationData,
} from "./types";

const ADEN_OFFSET_MS = 3 * 3600 * 1000; // اليمن: UTC+3 بلا توقيت صيفي

/** خطأ عمل — رسالته عربية تُعرض للمستخدم مباشرة. */
export class BizError extends Error {}

const biz = (msg: string): never => {
  throw new BizError(msg);
};

/** الطابع الزمني المتزايد الفريد (فحص L9): لا طابعان متطابقان أبداً.
 *  يُحفظ في صف مقفول داخل المعاملة نفسها — ذرّي حتى مع طلبات متزامنة. */
async function freshNowTx(tx: Parameters<Parameters<typeof db.transaction>[0]>[0]): Promise<number> {
  const rows = await tx.execute(
    sql`INSERT INTO settings (key, value) VALUES ('last_ts', '0')
        ON CONFLICT (key) DO UPDATE SET value = settings.value
        RETURNING value`
  );
  const last = Number(rows.rows[0]?.value ?? 0);
  const next = Math.max(Date.now(), last + 1);
  await tx
    .update(settings)
    .set({ value: String(next) })
    .where(eq(settings.key, "last_ts"));
  return next;
}

/** منتصف الليل المحلي (إصلاح الفحص L17): «اليوم» من منتصف الليل لا آخر 24 ساعة. */
export function periodStart(p: ReportPeriod, now = Date.now()): number {
  switch (p) {
    case "ALL":
      return 0;
    case "DAY": {
      const shifted = now + ADEN_OFFSET_MS;
      return Math.floor(shifted / 86_400_000) * 86_400_000 - ADEN_OFFSET_MS;
    }
    case "WEEK":
      return now - 7 * 86_400_000;
    case "MONTH":
      return now - 30 * 86_400_000;
  }
}

async function getSetting(key: string, fallback = ""): Promise<string> {
  const rows = await db.select().from(settings).where(eq(settings.key, key)).limit(1);
  return rows[0]?.value ?? fallback;
}

async function setSetting(key: string, value: string): Promise<void> {
  await db.execute(
    sql`INSERT INTO settings (key, value) VALUES (${key}, ${value})
        ON CONFLICT (key) DO UPDATE SET value = ${value}`
  );
}

/** إعادة حساب كاش الزبون من المصدر الموثوق (المجاميع المشتقة). */
async function recomputeCustomerTx(
  tx: Parameters<Parameters<typeof db.transaction>[0]>[0],
  id: string
): Promise<void> {
  const r = await tx.execute(
    sql`SELECT COALESCE((SELECT SUM(total_amount) FROM sales WHERE customer_id = ${id}), 0) AS debt,
               COALESCE((SELECT SUM(amount_paid)  FROM sales WHERE customer_id = ${id}), 0)
             + COALESCE((SELECT SUM(amount) FROM payments WHERE customer_id = ${id}), 0) AS paid`
  );
  const debt = Number(r.rows[0]?.debt ?? 0);
  const paid = Number(r.rows[0]?.paid ?? 0);
  await tx.update(customers).set({ totalDebt: debt, totalPaid: paid }).where(eq(customers.id, id));
}

function uuid(): string {
  return crypto.randomUUID();
}

// ===== الزبائن =====

export async function createCustomer(name: string, phone: string): Promise<Customer> {
  const n = name.trim();
  if (!n) biz("أدخل اسم الزبون");
  const dup = await db
    .select({ id: customers.id })
    .from(customers)
    .where(sql`lower(${customers.name}) = lower(${n})`)
    .limit(1);
  if (dup.length > 0) biz(`يوجد زبون بنفس الاسم «${n}»`);
  const c: Customer = {
    id: uuid(),
    name: n,
    phone: phone.trim(),
    totalDebt: 0,
    totalPaid: 0,
    createdAt: Date.now(),
  };
  await db.insert(customers).values({ ...c });
  return c;
}

/** تحديث زبون — UPDATE صريح (إصلاح P0) مع بث الاسم المكرر للسجلات (إصلاح الخطأ 9). */
export async function updateCustomer(id: string, name: string, phone: string): Promise<void> {
  const n = name.trim();
  if (!n) biz("الاسم لا يمكن أن يكون فارغاً");
  await db.transaction(async (tx) => {
    const dup = await tx
      .select({ id: customers.id })
      .from(customers)
      .where(sql`lower(${customers.name}) = lower(${n}) AND ${customers.id} <> ${id}`)
      .limit(1);
    if (dup.length > 0) biz(`يوجد زبون مسجّل بنفس الاسم «${n}»`);
    const found = await tx.select().from(customers).where(eq(customers.id, id)).limit(1);
    if (found.length === 0) biz("الزبون غير موجود");
    await tx
      .update(customers)
      .set({ name: n, phone: phone.trim() })
      .where(eq(customers.id, id));
    await tx.update(sales).set({ customerName: n }).where(eq(sales.customerId, id));
    await tx.update(payments).set({ customerName: n }).where(eq(payments.customerId, id));
  });
}

export async function deleteCustomer(id: string): Promise<void> {
  await db.transaction(async (tx) => {
    const s = await tx
      .select({ n: sql<number>`count(*)` })
      .from(sales)
      .where(eq(sales.customerId, id));
    const p = await tx
      .select({ n: sql<number>`count(*)` })
      .from(payments)
      .where(eq(payments.customerId, id));
    if (Number(s[0]?.n ?? 0) > 0 || Number(p[0]?.n ?? 0) > 0)
      biz("لا يمكن حذف زبون له عمليات بيع أو دفعات. ألغِ عملياته أولاً");
    await tx.delete(customers).where(eq(customers.id, id));
  });
}

export async function searchCustomers(q: string): Promise<Customer[]> {
  const t = q.trim();
  if (!t) return [];
  const escaped = t.replace(/\\/g, "\\\\").replace(/%/g, "\\%").replace(/_/g, "\\_");
  return db
    .select()
    .from(customers)
    .where(
      sql`(name ILIKE ${"%" + escaped + "%"} OR phone ILIKE ${"%" + escaped + "%"})`
    )
    .orderBy(asc(customers.name))
    .limit(50);
}

export async function getCustomerDetail(id: string): Promise<CustomerDetail> {
  const c = (await db.select().from(customers).where(eq(customers.id, id)).limit(1))[0] ?? null;
  const s = await db.select().from(sales).where(eq(sales.customerId, id)).orderBy(desc(sales.saleDate));
  const p = await db.select().from(payments).where(eq(payments.customerId, id)).orderBy(desc(payments.paymentDate));
  const totalBought = s.reduce((a, x) => a + Number(x.totalAmount), 0);
  const totalPaid = s.reduce((a, x) => a + Number(x.amountPaid), 0) + p.reduce((a, x) => a + Number(x.amount), 0);
  return { customer: c, sales: s, payments: p, totalBought, totalPaid, balance: totalBought - totalPaid };
}

// ===== البيع =====

export async function recordSale(input: {
  customerName: string;
  phone?: string;
  units: number;
  price: number;
  paidNow: number;
  notes: string;
}): Promise<{ saleId: string; firstSaleToday: boolean }> {
  const { units, price, paidNow } = input;
  if (!Number.isInteger(units) || units <= 0 || units > MAX_UNITS)
    biz(`أدخل عدداً صحيحاً من الأسطوانات (الحد الأقصى ${MAX_UNITS})`);
  // إصلاح الفحص 82: السعر 0 مسموح (منحة/عينة). السالب مرفوض دائماً.
  if (!Number.isInteger(price) || price < 0 || price > MAX_AMOUNT)
    biz(`أدخل سعراً غير سالب (الحد الأقصى ${MAX_AMOUNT} ريال)`);
  const total = units * price;
  // إصلاح الفحص 4: دفع جزئي وقت البيع ضمن [0, الإجمالي].
  if (!Number.isInteger(paidNow) || paidNow < 0 || paidNow > total)
    biz(`المدفوع الآن خارج النطاق (0 إلى ${total} ريال)`);
  const name = input.customerName.trim();
  if (!name) biz("أدخل اسم الزبون");

  return db.transaction(async (tx) => {
    const available = await tx
      .select({ n: sql<number>`count(*)` })
      .from(cylinders)
      .where(eq(cylinders.status, "AVAILABLE"));
    const avail = Number(available[0]?.n ?? 0);
    if (avail < units) biz(`المخزون لا يكفي — المتوفر ${avail} أسطوانة فقط`);

    const existing = (
      await tx.select().from(customers).where(sql`lower(${customers.name}) = lower(${name})`).limit(1)
    )[0];
    const custId = existing?.id ?? uuid();
    const now = await freshNowTx(tx);
    // إصلاح المشكلة 1: الزبون الجديد يُنشأ داخل المعاملة نفسها قبل أي عملية تعتمد عليه.
    if (!existing) {
      await tx.insert(customers).values({
        id: custId,
        name,
        phone: (input.phone ?? "").trim(),
        totalDebt: 0,
        totalPaid: 0,
        createdAt: now,
      });
    }

    // تخصيص ذرّي: قفل الصفوف المختارة ثم تحديثها — لا سباق مع بيع متزامن.
    const marked = await tx.execute(
      sql`UPDATE cylinders SET status = 'SOLD', sold_date = ${now}
          WHERE id IN (
            SELECT id FROM cylinders WHERE status = 'AVAILABLE'
            ORDER BY acquired_date ASC LIMIT ${units} FOR UPDATE
          ) RETURNING id`
    );
    const ids = marked.rows.map((r) => String(r.id));
    if (ids.length !== units) biz("تعذّر تخصيص الأسطوانات — أعد المحاولة");

    // ملاحظة: pg يعيد COUNT كسلسلة — التحويل الصريح يمنع مقارنة "0" === 0 الفاشلة.
    const firstToday =
      Number(
        (
          await tx
            .select({ n: sql<number>`count(*)` })
            .from(sales)
            .where(gte(sales.saleDate, periodStart("DAY", now)))
        )[0]?.n ?? 0
      ) === 0;

    const saleId = uuid();
    await tx.insert(sales).values({
      id: saleId,
      customerId: custId,
      customerName: name,
      cylinderIdsJson: JSON.stringify(ids),
      unitsSold: units,
      pricePerUnit: price,
      totalAmount: total,
      amountPaid: paidNow,
      status: total - paidNow <= 0 ? "PAID" : "CREDIT",
      saleDate: now,
      notes: input.notes.trim(),
    });
    await recomputeCustomerTx(tx, custId);
    return { saleId, firstSaleToday: firstToday };
  });
}

// ===== تحصيل الديون =====

export async function recordCustomerPayment(customerId: string, amount: number, notes: string): Promise<void> {
  if (!Number.isInteger(amount) || amount < 1 || amount > MAX_AMOUNT) biz("أدخل مبلغاً صحيحاً");
  await db.transaction(async (tx) => {
    const c = (await tx.select().from(customers).where(eq(customers.id, customerId)).limit(1))[0];
    if (!c) biz("الزبون غير موجود");
    // إصلاح الفحص 7: السداد الزائد مسموح — الفارق رصيد دائن لصالح الزبون.
    await tx.insert(payments).values({
      id: uuid(),
      customerId,
      customerName: c.name,
      amount,
      paymentDate: await freshNowTx(tx),
      notes: notes.trim(),
    });
    await recomputeCustomerTx(tx, customerId);
  });
}

/** عكس دفعة تحصيل خاطئة — يُعيد المبلغ ديناً على الزبون. */
export async function reverseCustomerPayment(paymentId: string): Promise<void> {
  await db.transaction(async (tx) => {
    const p = (await tx.select().from(payments).where(eq(payments.id, paymentId)).limit(1))[0];
    if (!p) biz("الدفعة غير موجودة");
    await tx.delete(payments).where(eq(payments.id, paymentId));
    await recomputeCustomerTx(tx, p.customerId);
  });
}

// ===== إلغاء بيع =====

export async function cancelSale(saleId: string): Promise<void> {
  await db.transaction(async (tx) => {
    const sale = (await tx.select().from(sales).where(eq(sales.id, saleId)).limit(1))[0];
    if (!sale) biz("البيع غير موجود");

    // إصلاح المشكلة 3: منع الإلغاء مع تحصيلات لاحقة + رسالة قابلة للتصرف (العيب 9).
    const later = await tx
      .select()
      .from(payments)
      .where(and(eq(payments.customerId, sale.customerId), sql`payment_date > ${sale.saleDate}`));
    if (later.length > 0) {
      const sum = later.reduce((a, x) => a + Number(x.amount), 0);
      const oldest = new Date(Math.min(...later.map((x) => Number(x.paymentDate))));
      biz(
        `لا يمكن الإلغاء: توجد ${later.length} دفعة لاحقة على هذا الزبون بمجموع ${sum} ريال ` +
          `(أقدمها ${oldest.toLocaleString("ar")}). ألغِ تلك الدفعات أولاً إن كانت سُجّلت بالخطأ — ` +
          `وإلا فراجع حساب الزبون يدوياً.`
      );
    }

    // إصلاح الفحص 33: قراءة JSON حقيقية مع تسامح مع CSV القديم.
    const ids = parseCylinderIds(sale.cylinderIdsJson);
    // إصلاح المشكلة 2 + العيب 10: الاسترجاع محروس بالمطابقة حتى لو كانت القائمة فارغة.
    let restored = 0;
    if (ids.length > 0) {
      const r = await tx
        .update(cylinders)
        .set({ status: "AVAILABLE", soldDate: 0 })
        .where(
          and(inArray(cylinders.id, ids), eq(cylinders.status, "SOLD"), eq(cylinders.soldDate, Number(sale.saleDate)))
        )
        .returning({ id: cylinders.id });
      restored = r.length;
    }
    if (ids.length !== sale.unitsSold || restored !== ids.length) {
      biz(
        `بيانات أسطوانات هذا البيع غير مكتملة (المسجَّل ${sale.unitsSold} — الموجود ${ids.length} — ` +
          `المُسترجَع ${restored}). أُلغي الإلغاء حفاظاً على المخزون.`
      );
    }
    await tx.delete(sales).where(eq(sales.id, saleId));
    await recomputeCustomerTx(tx, sale.customerId);
  });
}

function parseCylinderIds(stored: string): string[] {
  const t = stored.trim();
  if (!t) return [];
  if (t.startsWith("[")) {
    return t
      .replace(/^\[/, "")
      .replace(/\]$/, "")
      .split(",")
      .map((s) => s.trim().replace(/^"|"$/g, ""))
      .filter(Boolean);
  }
  return t.split(",").map((s) => s.trim()).filter(Boolean);
}

// ===== المحطة (المورد) =====

export async function purchaseFromStation(input: {
  units: number;
  cost: number;
  paidNow: number;
  notes: string;
}): Promise<void> {
  const { units, cost, paidNow } = input;
  if (!Number.isInteger(units) || units < 1 || units > MAX_UNITS)
    biz(`أدخل عدداً صحيحاً (الحد الأقصى ${MAX_UNITS})`);
  if (!Number.isInteger(cost) || cost < 1 || cost > MAX_AMOUNT)
    biz(`التكلفة خارج النطاق المسموح (الحد الأقصى ${MAX_AMOUNT} ريال)`);
  const total = units * cost;
  // إصلاح الفحص H3: النطاق [0, total] — السالبة كانت تمر قديماً فتضخّم الدين صامتاً.
  if (!Number.isInteger(paidNow) || paidNow < 0 || paidNow > total) biz("المدفوع خارج النطاق المسموح");

  await db.transaction(async (tx) => {
    const now = await freshNowTx(tx);
    // إصلاح الخطأ 3: معرّف السحب يُولَّد أولاً ويُختم على كل أسطوانة.
    const purchaseId = uuid();
    await tx.insert(cylinders).values(
      Array.from({ length: units }, () => ({
        id: uuid(),
        sizeLiters: 20,
        status: "AVAILABLE" as const,
        acquiredFromStation: "محطة المورد",
        acquisitionCost: cost,
        acquiredDate: now,
        soldDate: 0,
        purchaseId,
      }))
    );
    await tx.insert(stationPurchases).values({
      id: purchaseId,
      units,
      costPerUnit: cost,
      totalAmount: total,
      amountPaid: paidNow,
      purchaseDate: now,
      notes: input.notes.trim(),
    });
  });
}

export async function payStation(amount: number, notes: string): Promise<void> {
  if (!Number.isInteger(amount) || amount < 1 || amount > MAX_AMOUNT) biz("أدخل مبلغاً صحيحاً");
  const balance = await getStationBalance();
  if (balance <= 0) biz("لا يوجد دَين للمحطة");
  if (amount > balance) biz("المبلغ أكبر من دَين المحطة");
  await db.insert(stationPayments).values({
    id: uuid(),
    amount,
    paymentDate: Date.now(),
    notes: notes.trim(),
  });
}

/** إلغاء سحب من المحطة — فقط إذا كانت أسطواناته ما زالت في المخزون (المشكلة 11). */
export async function cancelStationPurchase(purchaseId: string): Promise<void> {
  await db.transaction(async (tx) => {
    const p = (await tx.select().from(stationPurchases).where(eq(stationPurchases.id, purchaseId)).limit(1))[0];
    if (!p) biz("السحب غير موجود");
    const rows = await tx
      .select({ id: cylinders.id })
      .from(cylinders)
      .where(and(eq(cylinders.purchaseId, purchaseId), eq(cylinders.status, "AVAILABLE")));
    if (rows.length < p.units)
      biz("لا يمكن الإلغاء — بعض أسطوانات هذا السحب بِيعت بالفعل. ألغِ تلك المبيعات أولاً.");
    await tx.delete(cylinders).where(inArray(cylinders.id, rows.map((r) => r.id)));
    await tx.delete(stationPurchases).where(eq(stationPurchases.id, purchaseId));
  });
}

export async function cancelStationPayment(paymentId: string): Promise<void> {
  const rows = await db.select().from(stationPayments).where(eq(stationPayments.id, paymentId)).limit(1);
  if (rows.length === 0) biz("التسديد غير موجود");
  await db.delete(stationPayments).where(eq(stationPayments.id, paymentId));
}

async function getStationBalance(): Promise<number> {
  const r = await db.execute(
    sql`SELECT COALESCE((SELECT SUM(total_amount) FROM station_purchases),0)
             - COALESCE((SELECT SUM(amount_paid) FROM station_purchases),0)
             - COALESCE((SELECT SUM(amount) FROM station_payments),0) AS bal`
  );
  return Number(r.rows[0]?.bal ?? 0);
}

export async function getStationData(): Promise<StationData> {
  const [purchases, pays, totals] = await Promise.all([
    db.select().from(stationPurchases).orderBy(desc(stationPurchases.purchaseDate)).limit(200),
    db.select().from(stationPayments).orderBy(desc(stationPayments.paymentDate)).limit(200),
    db.execute(
      sql`SELECT COALESCE(SUM(total_amount),0) AS total,
                 COALESCE(SUM(total_amount),0) - COALESCE(SUM(amount_paid),0)
                   - COALESCE((SELECT SUM(amount) FROM station_payments),0) AS bal
          FROM station_purchases`
    ),
  ]);
  return {
    purchases,
    payments: pays,
    totalPurchases: Number(totals.rows[0]?.total ?? 0),
    balance: Number(totals.rows[0]?.bal ?? 0),
  };
}

// ===== سجل المبيعات =====

export async function getSalesPaged(limit: number, offset: number): Promise<Sale[]> {
  return db.select().from(sales).orderBy(desc(sales.saleDate)).limit(Math.min(limit, 200)).offset(offset);
}

export async function getSalesCount(): Promise<number> {
  const r = await db.select({ n: sql<number>`count(*)` }).from(sales);
  return Number(r[0]?.n ?? 0);
}

/** بحث على مستوى القاعدة — الاسم أو الملاحظات مع تهريب محارف النمط (العيب 7). */
export async function searchSales(q: string): Promise<Sale[]> {
  const t = q.trim();
  if (!t) return [];
  const escaped = t.replace(/\\/g, "\\\\").replace(/%/g, "\\%").replace(/_/g, "\\_");
  const pat = `%${escaped}%`;
  return db
    .select()
    .from(sales)
    .where(sql`(customer_name ILIKE ${pat} OR notes ILIKE ${pat})`)
    .orderBy(desc(sales.saleDate))
    .limit(200);
}

export async function getSalesBetween(from: number, to: number): Promise<Sale[]> {
  return db
    .select()
    .from(sales)
    .where(and(gte(sales.saleDate, from), sql`sale_date <= ${to}`))
    .orderBy(desc(sales.saleDate));
}

// ===== الحالة المجمّعة =====

export async function getAppState(period: ReportPeriod): Promise<AppState> {
  const from = periodStart(period);
  const since = from > 0;

  const [inv, sums, stationBal, allSums, recent, debtors, custs, price] = await Promise.all([
    db.execute(
      sql`SELECT COUNT(*) FILTER (WHERE status = 'AVAILABLE') AS avail,
                 COUNT(*) FILTER (WHERE status = 'SOLD') AS sold FROM cylinders`
    ),
    db.execute(
      sql`SELECT COALESCE(SUM(total_amount),0) AS sales_total,
                 COALESCE(SUM(amount_paid),0) AS sale_paid
          FROM sales ${since ? sql`WHERE sale_date >= ${from}` : sql``}`,
      // ملاحظة: لا تُدمج هذه مع التالية — لكل جدول نافذته الخاصة.
    ),
    getStationBalance(),
    db.execute(
      sql`SELECT COALESCE((SELECT SUM(total_amount) FROM sales),0) AS all_sales,
                 COALESCE((SELECT SUM(amount_paid) FROM sales),0)
               + COALESCE((SELECT SUM(amount) FROM payments),0) AS all_paid`
    ),
    db.select().from(sales).orderBy(desc(sales.saleDate)).limit(50),
    db.select().from(customers).where(sql`total_debt > total_paid`).orderBy(desc(sql`total_debt - total_paid`)).limit(3),
    db.select().from(customers).orderBy(asc(sql`lower(name)`)).limit(2000),
    getSetting("default_price", "25000"),
  ]);

  const collections = await db.execute(
    sql`SELECT COALESCE(SUM(amount),0) AS c FROM payments ${since ? sql`WHERE payment_date >= ${from}` : sql``}`
  );
  const cost = await db.execute(
    sql`SELECT COALESCE(SUM(acquisition_cost),0) AS c FROM cylinders
        WHERE status = 'SOLD' ${since ? sql`AND sold_date >= ${from}` : sql``}`
  );

  const salesTotal = Number(sums.rows[0]?.sales_total ?? 0);
  const salePaid = Number(sums.rows[0]?.sale_paid ?? 0);
  const cols = Number(collections.rows[0]?.c ?? 0);
  const totalCost = Number(cost.rows[0]?.c ?? 0);
  const allSalesV = Number(allSums.rows[0]?.all_sales ?? 0);
  const allPaidV = Number(allSums.rows[0]?.all_paid ?? 0);

  const periodRecent = since ? recent.filter((s) => Number(s.saleDate) >= from) : recent;

  return {
    stats: {
      availableCount: Number(inv.rows[0]?.avail ?? 0),
      soldCount: Number(inv.rows[0]?.sold ?? 0),
      totalSales: salesTotal,
      totalPaid: salePaid + cols,
      totalCredit: Math.max(allSalesV - allPaidV, 0),
      totalCost,
      profit: salesTotal - totalCost,
      stationBalance: Number(stationBal),
      allTimeSales: allSalesV,
      allTimePaid: allPaidV,
    },
    recentSales: periodRecent,
    topDebtors: debtors.map((c) => ({ ...c, balance: c.totalDebt - c.totalPaid })),
    customers: custs,
    defaultPrice: Number(price) > 0 ? Number(price) : 25000,
    period,
  };
}

// ===== الإعدادات ورمز القفل =====
// مرآة SettingsStore.kt: ملح عشوائي + بصمة، وقفل تدرّجي متصاعد 60ث←5د←30د←ساعتان.

const MAX_ATTEMPTS = 4;
const LOCK_LADDER = [60_000, 5 * 60_000, 30 * 60_000, 2 * 3_600_000];

function sha256(s: string): string {
  return createHash("sha256").update(s).digest("hex");
}

export async function setDefaultPrice(v: number): Promise<void> {
  if (!Number.isInteger(v) || v <= 0 || v > MAX_AMOUNT) biz("أدخل سعراً صحيحاً أكبر من صفر");
  await setSetting("default_price", String(v));
}

export async function setReducedMotion(on: boolean): Promise<void> {
  await setSetting("reduced_motion", on ? "1" : "0");
}

export async function getReducedMotion(): Promise<boolean> {
  return (await getSetting("reduced_motion", "0")) === "1";
}

export async function getPinStatus(): Promise<{ isSet: boolean; lockRemainingMs: number }> {
  const [salt, fails, lastFail] = await Promise.all([
    getSetting("pin_salt"),
    getSetting("pin_fails", "0"),
    getSetting("pin_last_fail", "0"),
  ]);
  const f = Number(fails);
  if (!salt) return { isSet: false, lockRemainingMs: 0 };
  if (f < MAX_ATTEMPTS) return { isSet: true, lockRemainingMs: 0 };
  const tier = Math.min(Math.floor((f - MAX_ATTEMPTS) / MAX_ATTEMPTS), LOCK_LADDER.length - 1);
  const lockMs = LOCK_LADDER[tier];
  const elapsed = Date.now() - Number(lastFail);
  return { isSet: true, lockRemainingMs: Math.max(lockMs - elapsed, 0) };
}

export async function setPin(pin: string): Promise<void> {
  if (!/^\d{4}$/.test(pin)) biz("رمز القفل 4 أرقام بالضبط");
  const salt = randomBytes(16).toString("hex");
  await db.transaction(async (tx) => {
    const up = (k: string, v: string) =>
      tx.execute(
        sql`INSERT INTO settings (key, value) VALUES (${k}, ${v})
            ON CONFLICT (key) DO UPDATE SET value = ${v}`
      );
    await up("pin_salt", salt);
    await up("pin_hash", sha256(pin + salt));
    await up("pin_fails", "0");
    await up("pin_last_fail", "0");
  });
}

export async function clearPin(): Promise<void> {
  await db.delete(settings).where(inArray(settings.key, ["pin_salt", "pin_hash", "pin_fails", "pin_last_fail"]));
}

/** محاولة فتح — يعيد {ok} أو {ok:false, error, lockRemainingMs}. */
export async function tryUnlockPin(pin: string): Promise<{ ok: boolean; error?: string; lockRemainingMs?: number }> {
  const status = await getPinStatus();
  if (!status.isSet) return { ok: true };
  if (status.lockRemainingMs > 0) {
    return {
      ok: false,
      error: "القفل مفعّل مؤقتاً لكثرة المحاولات الخاطئة",
      lockRemainingMs: status.lockRemainingMs,
    };
  }
  const salt = await getSetting("pin_salt");
  const hash = await getSetting("pin_hash");
  if (salt && hash === sha256(pin + salt)) {
    await setSetting("pin_fails", "0");
    return { ok: true };
  }
  const fails = Number(await getSetting("pin_fails", "0")) + 1;
  await setSetting("pin_fails", String(fails));
  await setSetting("pin_last_fail", String(Date.now()));
  const after = await getPinStatus();
  const msg = after.lockRemainingMs > 0
    ? `رمز خاطئ — تم قفل الإدخال ${Math.ceil(after.lockRemainingMs / 1000)} ثانية`
    : `رمز خاطئ (المحاولة ${fails} من ${MAX_ATTEMPTS} قبل القفل المؤقت)`;
  return { ok: false, error: msg, lockRemainingMs: after.lockRemainingMs };
}
