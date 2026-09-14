/**
 * وحدة المال — منقولة من util/Money.kt في تطبيق Kotlin مع نفس الأسقف.
 * كل المبالغ أعداد صحيحة بالريال اليمني (لا كسور عملياً في السوق).
 */

/** سقف المبالغ: مليار ريال — يمنع التشبع والـ overflow في `units * price`. */
export const MAX_AMOUNT = 1_000_000_000;

/** سقف كمية الأسطوانات في عملية واحدة — يمنع القوائم الضخمة (OOM). */
export const MAX_UNITS = 10_000;

/** إدخال نصي بالريال مثل "25000" أو "25,000" → عدد صحيح. غير صالح = -1 (للتمييز). */
export function parseRials(text: string): number {
  const t = text.trim().replace(/,/g, "").replace(/[٠-٩]/g, (d) =>
    String("٠١٢٣٤٥٦٧٨٩".indexOf(d))
  );
  if (t === "") return -1;
  const v = Number(t);
  if (!Number.isFinite(v)) return -1;
  return Math.round(v);
}

/** حبس القيمة ضمن [0, MAX_AMOUNT]. */
export function clampAmount(v: number): number {
  return Math.min(Math.max(v, 0), MAX_AMOUNT);
}

/** عرض بشري بفواصل الآلاف: 25000 → "25,000". */
export function formatRials(v: number): string {
  const sign = v < 0 ? "-" : "";
  return sign + Math.abs(Math.round(v)).toLocaleString("en-US");
}

/** تنسيق طابع زمني (ملي ثانية) بتاريخ/وقت قصير عربي. */
export function formatDateTime(ms: number): string {
  return new Intl.DateTimeFormat("ar", {
    dateStyle: "short",
    timeStyle: "short",
    numberingSystem: "latn",
  }).format(new Date(ms));
}

export function formatDate(ms: number): string {
  return new Intl.DateTimeFormat("ar", {
    dateStyle: "medium",
    numberingSystem: "latn",
  }).format(new Date(ms));
}
