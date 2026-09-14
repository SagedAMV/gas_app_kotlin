"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { apiGet } from "@/lib/client";
import { formatDateTime, formatRials, parseRials } from "@/lib/money";
import type { StationData } from "@/lib/types";
import { useStore } from "@/components/store";
import { EmptyState, Field, FlameButton, Icon, MoneyText, Sheet } from "@/components/ui";

/** شاشة المحطة (المورد) — السحوبات والتسديدات مع الإلغاء الآمن (مرآة StationScreen). */
export function StationScreen() {
  const { run, toast } = useStore();
  const [data, setData] = useState<StationData | null>(null);
  const [payOpen, setPayOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [notes, setNotes] = useState("");

  const load = () => apiGet<StationData>("/api/station").then(setData).catch(() => {});
  useEffect(() => {
    void load();
  }, []);

  const submitPay = async () => {
    const v = parseRials(amount);
    if (v <= 0) return toast("أدخل مبلغاً صحيحاً", "err");
    const ok = await run("payStation", { amount: v, notes }, "تم تسجيل التسديد للمحطة");
    if (ok) {
      setPayOpen(false);
      setAmount("");
      setNotes("");
      await load();
    }
  };

  if (!data)
    return (
      <div className="space-y-3">
        <div className="skeleton h-32 rounded-3xl" />
        <div className="skeleton h-40 rounded-3xl" />
      </div>
    );

  return (
    <div className="space-y-4">
      {/* بطاقة دين المحطة */}
      <div className="rounded-3xl bg-night p-5 text-paper shadow-lift">
        <div className="flex items-center gap-2 text-sm text-paper/70">
          <Icon name="truck" className="h-5 w-5" />
          دَين المحطة المتبقي
        </div>
        <MoneyText
          value={data.balance}
          className={`mt-1 block font-display text-3xl font-black ${data.balance > 0 ? "text-credit" : "text-good"}`}
        />
        <div className="mt-3 flex justify-between border-t border-paper/15 pt-3 text-xs text-paper/70">
          <span>إجمالي السحوبات: <b className="tnum text-paper">{formatRials(data.totalPurchases)}</b></span>
        </div>
        <div className="mt-3">
          <FlameButton onClick={() => setPayOpen(true)} disabled={data.balance <= 0}>
            <Icon name="wallet" className="h-5 w-5" /> تسديد دفعة للمحطة
          </FlameButton>
        </div>
      </div>

      {/* السحوبات */}
      <section>
        <p className="mb-2 text-sm font-bold text-ink-soft">سحوبات الأسطوانات</p>
        {data.purchases.length === 0 ? (
          <EmptyState icon="truck" title="لا سحوبات بعد" hint="اسحب من المحطة عبر تبويب المخزون" />
        ) : (
          <div className="space-y-2">
            {data.purchases.slice(0, 30).map((p, i) => (
              <motion.div
                key={p.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: Math.min(i * 0.04, 0.3) }}
                className="rounded-2xl border border-line bg-card p-3.5 shadow-lift"
              >
                <div className="flex items-center justify-between">
                  <p className="font-bold">
                    {p.units} أسطوانة × <span className="tnum">{formatRials(p.costPerUnit)}</span>
                  </p>
                  <span className="font-black tnum">{formatRials(p.totalAmount)} ريال</span>
                </div>
                <div className="mt-1 flex items-center justify-between text-xs text-ink-faint">
                  <span className="tnum">{formatDateTime(p.purchaseDate)}</span>
                  <span className={p.totalAmount - p.amountPaid > 0 ? "font-bold text-credit" : "font-bold text-good"}>
                    {p.totalAmount - p.amountPaid > 0 ? `متبقٍ ${formatRials(p.totalAmount - p.amountPaid)}` : "مسدد بالكامل"}
                  </span>
                </div>
                <div className="mt-2 flex justify-end">
                  <button
                    onClick={async () => {
                      const ok = await run("cancelStationPurchase", { purchaseId: p.id });
                      if (ok) { toast("أُلغي السحب وأُعيدت الأسطوانات"); await load(); }
                    }}
                    className="flex items-center gap-1 rounded-lg bg-bad-soft px-2.5 py-1.5 text-xs font-bold text-bad transition active:scale-95"
                  >
                    <Icon name="undo" className="h-3.5 w-3.5" /> إلغاء السحب
                  </button>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </section>

      {/* التسديدات */}
      <section>
        <p className="mb-2 text-sm font-bold text-ink-soft">تسديدات المحطة</p>
        {data.payments.length === 0 ? (
          <EmptyState icon="wallet" title="لا تسديدات بعد" />
        ) : (
          <div className="space-y-2">
            {data.payments.slice(0, 30).map((p) => (
              <div key={p.id} className="flex items-center justify-between rounded-2xl border border-line bg-card px-4 py-3 shadow-lift">
                <div>
                  <p className="font-black text-good tnum">−{formatRials(p.amount)} ريال</p>
                  <p className="text-xs text-ink-faint tnum">{formatDateTime(p.paymentDate)}</p>
                </div>
                <button
                  onClick={async () => {
                    const ok = await run("cancelStationPayment", { paymentId: p.id });
                    if (ok) { toast("أُلغي التسديد وعاد المبلغ ديناً"); await load(); }
                  }}
                  className="rounded-lg bg-bad-soft px-2.5 py-1.5 text-xs font-bold text-bad active:scale-95"
                >
                  عكس
                </button>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* تسديد */}
      <Sheet open={payOpen} onClose={() => setPayOpen(false)} title="تسديد دفعة للمحطة">
        <div className="space-y-4">
          <div className="rounded-2xl bg-credit-soft px-4 py-3 text-sm font-semibold text-credit">
            الدين الحالي: {formatRials(data.balance)} ريال — لا يُقبل مبلغ أكبر منه
          </div>
          <Field label="المبلغ">
            <input className="field tnum" inputMode="numeric" value={amount} onChange={(e) => setAmount(e.target.value)} />
          </Field>
          <Field label="ملاحظات">
            <input className="field" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </Field>
          <FlameButton onClick={submitPay} disabled={parseRials(amount) <= 0 || parseRials(amount) > data.balance}>
            تأكيد التسديد
          </FlameButton>
        </div>
      </Sheet>
    </div>
  );
}
