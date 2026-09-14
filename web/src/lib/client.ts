"use client";

/** مساعدات العميل: استدراء الخادم + حالة عامة خفيفة. */

export interface ApiOk<T> {
  ok: true;
  data: T;
}
export interface ApiErr {
  ok: false;
  error: string;
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(path, { cache: "no-store" });
  const j = (await res.json()) as ApiOk<T> | ApiErr;
  if (!j.ok) throw new Error(j.error);
  return j.data;
}

/** تنفيذ عملية معدِّلة — يعيد رسالة الخطأ إن فشلت (بدل رميها) لعرضها كتوست. */
export async function mutate<T = unknown>(
  op: string,
  payload: Record<string, unknown> = {}
): Promise<{ ok: boolean; error?: string; data?: T }> {
  try {
    const res = await fetch("/api/mutate", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ op, ...payload }),
    });
    const j = (await res.json()) as { ok: boolean; error?: string; data?: T };
    return j;
  } catch {
    return { ok: false, error: "تعذّر الاتصال بالخادم" };
  }
}

/** عدّاد متحرك للأرقام المالية — يصعد للقيمة الجديدة بدل القفز. */
export function animateNumber(
  from: number,
  to: number,
  cb: (v: number) => void,
  duration = 650
): () => void {
  if (typeof window === "undefined" || from === to) {
    cb(to);
    return () => {};
  }
  const t0 = performance.now();
  let raf = 0;
  const step = (t: number) => {
    const p = Math.min((t - t0) / duration, 1);
    const eased = 1 - Math.pow(1 - p, 3);
    cb(Math.round(from + (to - from) * eased));
    if (p < 1) raf = requestAnimationFrame(step);
  };
  raf = requestAnimationFrame(step);
  return () => cancelAnimationFrame(raf);
}
