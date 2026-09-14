"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { parseRials, formatRials } from "@/lib/money";
import { useStore } from "@/components/store";
import { Field, FlameButton, Icon, LiquidGauge, Rial, Sheet, Stepper } from "@/components/ui";

/** شاشة المخزون — مقياس سائل حي + سحب من المحطة (مرآة InventoryScreen.kt). */
export function InventoryScreen() {
  const { state, loading, run } = useStore();
  const [open, setOpen] = useState(false);

  const [units, setUnits] = useState(10);
  const [costText, setCostText] = useState("22000");
  const [paidText, setPaidText] = useState("0");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);

  const cost = Math.max(parseRials(costText), 0);
  const paid = Math.max(parseRials(paidText), 0);
  const total = units * cost;
  const paidTooHigh = paid > total;

  const submit = async () => {
    setBusy(true);
    const ok = await run(
      "purchaseFromStation",
      { units, cost, paidNow: Math.min(paid, total), notes },
      `تم سحب ${units} أسطوانة إلى المخزون`
    );
    setBusy(false);
    if (ok) {
      setOpen(false);
      setNotes("");
    }
  };

  if (loading || !state)
    return (
      <div className="space-y-4">
        <div className="skeleton h-44 rounded-3xl" />
        <div className="skeleton h-24 rounded-3xl" />
      </div>
    );

  const { availableCount, soldCount } = state.stats;

  return (
    <div className="space-y-4">
      <LiquidGauge available={availableCount} sold={soldCount} />

      <div className="grid grid-cols-2 gap-3">
        <motion.div layout className="rounded-2xl border border-line bg-card p-4 shadow-lift">
          <p className="text-xs font-semibold text-ink-faint">مباعة إجمالاً</p>
          <p className="mt-1 font-display text-2xl font-black tnum">{soldCount}</p>
        </motion.div>
        <motion.div layout className="rounded-2xl border border-line bg-card p-4 shadow-lift">
          <p className="text-xs font-semibold text-ink-faint">سعة المخزون الكلية</p>
          <p className="mt-1 font-display text-2xl font-black tnum">{availableCount + soldCount}</p>
        </motion.div>
      </div>

      <FlameButton onClick={() => setOpen(true)}>
        <Icon name="truck" className="h-5 w-5" />
        سحب أسطوانات من المحطة
      </FlameButton>

      <Sheet open={open} onClose={() => setOpen(false)} title="سحب جديد من محطة المورد">
        <div className="space-y-4">
          <Field label="عدد الأسطوانات">
            <Stepper value={units} onChange={setUnits} min={1} max={500} />
          </Field>
          <Field label="تكلفة الأسطوانة (ريال)">
            <input className="field tnum" inputMode="numeric" value={costText} onChange={(e) => setCostText(e.target.value)} />
          </Field>
          <Field label="المدفوع للمحطة الآن">
            <input className="field tnum" inputMode="numeric" value={paidText} onChange={(e) => setPaidText(e.target.value)} />
          </Field>
          {paidTooHigh && (
            <p className="rise-in rounded-xl bg-bad-soft px-3 py-2 text-xs font-bold text-bad">
              المدفوع أكبر من الإجمالي — الأقصى <Rial v={total} /> ريال
            </p>
          )}
          <div className="rounded-2xl bg-flame-soft px-4 py-3 text-sm font-semibold text-flame-deep">
            إجمالي السحب: <span className="font-black tnum">{formatRials(total)}</span> ريال
            {total - Math.min(paid, total) > 0 && (
              <> — المتبقي ديناً للمحطة: <span className="font-black tnum">{formatRials(total - Math.min(paid, total))}</span></>
            )}
          </div>
          <Field label="ملاحظات (اختياري)">
            <input className="field" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </Field>
          <FlameButton onClick={submit} disabled={busy || paidTooHigh || cost <= 0}>
            {busy ? "جارٍ التسجيل…" : "تأكيد السحب"}
          </FlameButton>
        </div>
      </Sheet>
    </div>
  );
}
