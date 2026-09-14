"use client";

import { useState } from "react";
import { parseRials, formatRials } from "@/lib/money";
import { useStore } from "@/components/store";
import { Field, FlameButton, Icon, Sheet } from "@/components/ui";

/** شاشة الإعدادات — السعر الافتراضي، تقليل الحركة، رمز القفل (مرآة SettingsScreen). */
export function SettingsScreen({ pinSet, onPinChanged }: { pinSet: boolean; onPinChanged: (v: boolean) => void }) {
  const { state, run, toast, reducedMotion } = useStore();
  const [priceOpen, setPriceOpen] = useState(false);
  const [pinOpen, setPinOpen] = useState(false);
  const [priceText, setPriceText] = useState("");
  const [pin1, setPin1] = useState("");
  const [pin2, setPin2] = useState("");

  const savePrice = async () => {
    const v = parseRials(priceText);
    if (v <= 0) return toast("أدخل سعراً صحيحاً أكبر من صفر", "err");
    const ok = await run("setDefaultPrice", { price: v }, `السعر الافتراضي الآن ${formatRials(v)} ريال`);
    if (ok) setPriceOpen(false);
  };

  const savePin = async () => {
    if (!/^\d{4}$/.test(pin1)) return toast("رمز القفل 4 أرقام بالضبط", "err");
    if (pin1 !== pin2) return toast("الرمزان غير متطابقين", "err");
    const ok = await run("setPin", { pin: pin1 }, "تم تفعيل رمز القفل");
    if (ok) {
      setPinOpen(false);
      setPin1("");
      setPin2("");
      onPinChanged(true);
    }
  };

  const removePin = async () => {
    const ok = await run("clearPin", {}, "أُزيل رمز القفل");
    if (ok) onPinChanged(false);
  };

  const toggleMotion = async () => {
    const next = !reducedMotion;
    const ok = await run("setReducedMotion", { on: next }, next ? "تم تفعيل تقليل الحركة" : "أُعيد تفعيل الحركات");
    if (!ok) return;
    document.documentElement.classList.toggle("reduce-motion", next);
    // الحالة داخل المزود تُحدَّث عبر إعادة الجلب القادم — طبقها محلياً فوراً:
    window.dispatchEvent(new CustomEvent("reduced-motion", { detail: next }));
  };

  return (
    <div className="space-y-3">
      <Row
        icon="flame"
        title="السعر الافتراضي للأسطوانة"
        subtitle={state ? `${formatRials(state.defaultPrice)} ريال` : "…"}
        onClick={() => {
          setPriceText(String(state?.defaultPrice ?? 25000));
          setPriceOpen(true);
        }}
      />
      <div className="flex items-center justify-between rounded-2xl border border-line bg-card p-4 shadow-lift">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-flame-soft text-flame">
            <Icon name="sparkle" className="h-5 w-5" />
          </span>
          <div>
            <p className="font-bold">تقليل الحركة</p>
            <p className="text-xs text-ink-faint">إيقاف كل التحريكات والتكرارات</p>
          </div>
        </div>
        <button
          onClick={toggleMotion}
          role="switch"
          aria-checked={reducedMotion}
          className={`relative h-7 w-12 rounded-full transition-colors ${reducedMotion ? "bg-flame" : "bg-line"}`}
        >
          <span
            className={`absolute top-1 h-5 w-5 rounded-full bg-white shadow transition-all ${
              reducedMotion ? "right-6" : "right-1"
            }`}
          />
        </button>
      </div>

      <Row
        icon="lock"
        title={pinSet ? "رمز القفل مفعّل" : "تفعيل رمز القفل"}
        subtitle={pinSet ? "اضغط للإزالة" : "4 أرقام تحمي التطبيق عند الفتح"}
        onClick={() => (pinSet ? removePin() : setPinOpen(true))}
      />

      <a
        href="/changes.html"
        className="flex items-center gap-3 rounded-2xl border-2 border-dashed border-flame/40 bg-flame-soft/60 p-4 transition active:scale-[0.98]"
      >
        <span className="grid h-10 w-10 place-items-center rounded-xl bg-flame text-white">
          <Icon name="chart" className="h-5 w-5" />
        </span>
        <div className="flex-1">
          <p className="font-bold text-flame-deep">ماذا تغيّر في هذه النسخة؟</p>
          <p className="text-xs text-ink-soft">عرض متحرك «قبل/بعد» للإضافات والتحسينات</p>
        </div>
        <Icon name="back" className="h-5 w-5 rotate-180 text-flame" />
      </a>

      <div className="rounded-2xl border border-line bg-card p-4 text-xs leading-relaxed text-ink-faint shadow-lift">
        <p className="mb-1 font-bold text-ink-soft">عن النظام</p>
        إعادة بناء ويب لتطبيق «دَبّ لتجارة الغاز» (Kotlin/Compose) — نفس جوهر المنطق: كل عملية
        مالية داخل معاملة ذرّية، الأرصدة تُشتق من المعاملات لا من الكاش، والإلغاءات محروسة
        بمطابقة دقيقة. كل المبالغ بالريال اليمني الصحيح.
      </div>

      {/* السعر الافتراضي */}
      <Sheet open={priceOpen} onClose={() => setPriceOpen(false)} title="السعر الافتراضي">
        <div className="space-y-4">
          <Field label="السعر بالريال">
            <input className="field tnum" inputMode="numeric" value={priceText} onChange={(e) => setPriceText(e.target.value)} />
          </Field>
          <FlameButton onClick={savePrice}>حفظ</FlameButton>
        </div>
      </Sheet>

      {/* ضبط رمز القفل */}
      <Sheet open={pinOpen} onClose={() => setPinOpen(false)} title="رمز قفل جديد">
        <div className="space-y-4">
          <p className="rounded-xl bg-credit-soft px-3 py-2 text-xs font-semibold text-credit">
            يُخزَّن الرمز كبصمة مملّحة (ملح عشوائي + SHA-256) مع قفل متصاعد بعد 4 محاولات خاطئة:
            60 ثانية ← 5 دقائق ← 30 دقيقة ← ساعتان
          </p>
          <Field label="الرمز (4 أرقام)">
            <input className="field tnum text-center tracking-[0.5em]" inputMode="numeric" maxLength={4} value={pin1} onChange={(e) => setPin1(e.target.value.replace(/\D/g, ""))} />
          </Field>
          <Field label="تأكيد الرمز">
            <input className="field tnum text-center tracking-[0.5em]" inputMode="numeric" maxLength={4} value={pin2} onChange={(e) => setPin2(e.target.value.replace(/\D/g, ""))} />
          </Field>
          <FlameButton onClick={savePin} disabled={pin1.length !== 4 || pin2.length !== 4}>
            تفعيل القفل
          </FlameButton>
        </div>
      </Sheet>
    </div>
  );
}

function Row({ icon, title, subtitle, onClick }: { icon: string; title: string; subtitle: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="flex w-full items-center gap-3 rounded-2xl border border-line bg-card p-4 text-right shadow-lift transition active:scale-[0.98]"
    >
      <span className="grid h-10 w-10 place-items-center rounded-xl bg-flame-soft text-flame">
        <Icon name={icon} className="h-5 w-5" />
      </span>
      <span className="flex-1">
        <span className="block font-bold">{title}</span>
        <span className="block text-xs text-ink-faint">{subtitle}</span>
      </span>
      <Icon name="back" className="h-5 w-5 rotate-180 text-ink-faint" />
    </button>
  );
}
