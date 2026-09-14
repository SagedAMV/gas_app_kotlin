"use client";

import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { mutate } from "@/lib/client";
import { parseRials, formatRials } from "@/lib/money";
import { useStore } from "@/components/store";
import { Field, FlameButton, Icon, MoneyText, Stepper } from "@/components/ui";

/** شاشة الصرف/البيع — مرآة DispenseScreen.kt مع تدقيق إدخال لحظي. */
export function DispenseScreen() {
  const { state, loading, toast, refresh, confetti } = useStore();
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [units, setUnits] = useState(1);
  const [priceText, setPriceText] = useState<string | null>(null);
  const [paidText, setPaidText] = useState("0");
  const [paidMode, setPaidMode] = useState<"none" | "part" | "full">("none");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);

  const available = state?.stats.availableCount ?? 0;
  const defaultPrice = state?.defaultPrice ?? 25000;
  const price = priceText === null ? defaultPrice : Math.max(parseRials(priceText), 0);
  const total = units * price;

  const paidNow = useMemo(() => {
    if (paidMode === "full") return total;
    if (paidMode === "none") return 0;
    const v = parseRials(paidText);
    return v < 0 ? 0 : Math.min(v, total);
  }, [paidMode, paidText, total]);

  const remaining = total - paidNow;

  const suggestions = useMemo(() => {
    const t = name.trim();
    const list = state?.customers ?? [];
    if (!t) return list.slice(0, 4);
    return list.filter((c) => c.name.includes(t) && c.name !== t).slice(0, 5);
  }, [name, state?.customers]);

  const submit = async () => {
    if (busy) return;
    if (!name.trim()) return toast("أدخل اسم الزبون أولاً", "err");
    if (units > available) return toast(`المخزون لا يكفي — المتوفر ${available} أسطوانة فقط`, "err");
    setBusy(true);
    try {
      const r = await mutate<{ saleId: string; firstSaleToday: boolean }>("recordSale", {
        customerName: name,
        phone,
        units,
        price,
        paidNow,
        notes,
      });
      if (!r.ok) return toast(r.error ?? "حدث خطأ", "err");
      toast(
        remaining > 0
          ? `تم البيع — المتبقي على الزبون ${formatRials(remaining)} ريال`
          : "تم البيع بالكامل بنجاح",
        "ok"
      );
      if (r.data?.firstSaleToday) {
        confetti();
        window.setTimeout(() => toast("أول بيع اليوم — يوم موفق", "ok"), 600);
      }
      setName("");
      setPhone("");
      setUnits(1);
      setNotes("");
      setPaidMode("none");
      setPaidText("0");
      await refresh();
    } finally {
      setBusy(false);
    }
  };

  if (loading || !state) return <DispenseSkeleton />;

  return (
    <div className="space-y-4">
      {/* بطاقة تنبيه المخزون */}
      {available === 0 && (
        <div className="rise-in flex items-center gap-3 rounded-2xl border border-bad/30 bg-bad-soft px-4 py-3 text-sm font-semibold text-bad">
          <Icon name="flame" className="h-5 w-5 shrink-0" />
          المخزون فارغ — اسحب أسطوانات من المحطة أولاً من تبويب «المخزون»
        </div>
      )}

      <motion.div layout className="space-y-4 rounded-3xl border border-line bg-card p-5 shadow-lift">
        {/* الزبون */}
        <Field label="اسم الزبون">
          <input
            className="field"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="مثال: أحمد صالح"
            autoComplete="off"
          />
        </Field>
        {suggestions.length > 0 && name.trim() !== "" && (
          <div className="no-scrollbar -mt-1 flex gap-2 overflow-x-auto pb-1">
            {suggestions.map((c) => (
              <button
                key={c.id}
                onClick={() => {
                  setName(c.name);
                  setPhone(c.phone);
                }}
                className="pop-in shrink-0 rounded-full border border-line bg-paper px-3 py-1.5 text-xs font-semibold text-ink-soft transition active:scale-95"
              >
                {c.name}
              </button>
            ))}
          </div>
        )}
        <Field label="رقم الهاتف (اختياري)">
          <input
            className="field tnum"
            dir="ltr"
            inputMode="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="7xxxxxxxx"
          />
        </Field>

        {/* الكمية والسعر */}
        <div className="grid grid-cols-2 gap-4">
          <Field label={`الكمية (متوفر ${available})`}>
            <Stepper value={units} onChange={setUnits} min={1} max={Math.max(available, 1)} />
          </Field>
          <Field label="سعر الأسطوانة">
            <input
              className="field tnum"
              inputMode="numeric"
              value={priceText ?? String(defaultPrice)}
              onChange={(e) => setPriceText(e.target.value)}
              onFocus={() => setPriceText(priceText ?? String(defaultPrice))}
            />
          </Field>
        </div>

        {/* طريقة السداد */}
        <Field label="السداد الآن">
          <div className="grid grid-cols-3 gap-2">
            {(
              [
                { v: "none", label: "آجل بالكامل" },
                { v: "part", label: "دفعة جزئية" },
                { v: "full", label: "كامل المبلغ" },
              ] as const
            ).map((o) => (
              <button
                key={o.v}
                onClick={() => setPaidMode(o.v)}
                className={`rounded-xl border px-2 py-2.5 text-xs font-bold transition active:scale-95 ${
                  paidMode === o.v
                    ? "border-flame bg-flame-soft text-flame"
                    : "border-line bg-paper text-ink-soft"
                }`}
              >
                {o.label}
              </button>
            ))}
          </div>
        </Field>
        {paidMode === "part" && (
          <div className="rise-in">
            <Field label="المبلغ المدفوع الآن">
              <input
                className="field tnum"
                inputMode="numeric"
                value={paidText}
                onChange={(e) => setPaidText(e.target.value)}
                placeholder="0"
              />
            </Field>
          </div>
        )}

        <Field label="ملاحظات (اختياري)">
          <input className="field" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="مثال: تسليم مساءً" />
        </Field>
      </motion.div>

      {/* ملخص حي */}
      <div className="rounded-3xl bg-ink p-5 text-paper shadow-lift">
        <div className="flex items-center justify-between text-sm text-paper/70">
          <span>الإجمالي</span>
          <MoneyText value={total} className="font-display text-xl font-black text-paper" />
        </div>
        <div className="mt-2 flex items-center justify-between text-sm">
          <span className="text-paper/70">المدفوع الآن</span>
          <MoneyText value={paidNow} className="font-bold text-good" suffix=" ر.ي" />
        </div>
        <div className="mt-2 flex items-center justify-between border-t border-paper/15 pt-2 text-sm">
          <span className="text-paper/70">يبقى ديناً</span>
          <MoneyText value={remaining} className={`font-display text-lg font-black ${remaining > 0 ? "text-credit" : "text-good"}`} />
        </div>
      </div>

      <FlameButton onClick={submit} disabled={busy || available === 0 || !name.trim()}>
        <Icon name="flame" className="h-5 w-5" />
        {busy ? "جارٍ التسجيل…" : `تسجيل بيع ${units} أسطوانة`}
      </FlameButton>
    </div>
  );
}

function DispenseSkeleton() {
  return (
    <div className="space-y-4">
      <div className="skeleton h-64 rounded-3xl" />
      <div className="skeleton h-28 rounded-3xl" />
      <div className="skeleton h-14 rounded-2xl" />
    </div>
  );
}
