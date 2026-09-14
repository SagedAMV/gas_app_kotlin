"use client";

import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { animateNumber } from "@/lib/client";
import { formatRials } from "@/lib/money";

// ===== أيقونات (خطية موحدة العرض 1.8) =====
export function Icon({ name, className = "h-6 w-6" }: { name: string; className?: string }) {
  const paths: Record<string, ReactNode> = {
    flame: (
      <path d="M12 2.5c1.2 2.8-.6 4.3-1.9 5.9C8.7 10 7.5 11.6 7.5 14a4.5 4.5 0 0 0 9 0c0-1.2-.4-2.2-1-3-.3 1-.8 1.7-1.6 2.1.3-2.6-.4-6.9-1.9-10.6Z" />
    ),
    box: (
      <>
        <path d="M3.3 7.3 12 3l8.7 4.3v9.4L12 21l-8.7-4.3V7.3Z" />
        <path d="m3.3 7.3 8.7 4.4 8.7-4.4M12 11.7V21" />
      </>
    ),
    users: (
      <>
        <circle cx="9" cy="8" r="3.2" />
        <path d="M3.5 20c.6-3.2 2.8-5 5.5-5s4.9 1.8 5.5 5M16 5.6a3.2 3.2 0 0 1 0 5.9M17.8 15.4c1.8.7 3 2.3 3.4 4.6" />
      </>
    ),
    chart: (
      <>
        <path d="M4 20V10M10 20V4M16 20v-8" strokeLinecap="round" />
        <path d="M3 20h18" strokeLinecap="round" />
      </>
    ),
    truck: (
      <>
        <path d="M14 18V6H3v12M14 9h4l3 3.5V18h-2.5" />
        <circle cx="7" cy="18" r="2" />
        <circle cx="16.5" cy="18" r="2" />
      </>
    ),
    gear: (
      <>
        <circle cx="12" cy="12" r="3" />
        <path d="M12 2.8 13 5a7.2 7.2 0 0 1 2.4 1l2.3-.8 1.7 1.7-.8 2.3c.4.7.7 1.5 1 2.4l2.2 1-.0 2.3-2.3 1a7.2 7.2 0 0 1-1 2.4l.8 2.3-1.7 1.7-2.3-.8a7.2 7.2 0 0 1-2.4 1l-1 2.2h-2.3l-1-2.3a7.2 7.2 0 0 1-2.4-1l-2.3.8-1.7-1.7.8-2.3c-.4-.7-.7-1.5-1-2.4L2.9 13l0-2.3 2.3-1c.2-.9.6-1.7 1-2.4L5.4 5l1.7-1.7L9.4 4c.8-.4 1.6-.7 2.4-1Z" />
      </>
    ),
    plus: <path d="M12 5v14M5 12h14" strokeLinecap="round" />,
    minus: <path d="M5 12h14" strokeLinecap="round" />,
    close: <path d="m6 6 12 12M18 6 6 18" strokeLinecap="round" />,
    search: (
      <>
        <circle cx="10.5" cy="10.5" r="6.5" />
        <path d="m15.5 15.5 4.5 4.5" strokeLinecap="round" />
      </>
    ),
    back: <path d="m9 5 7 7-7 7" strokeLinecap="round" strokeLinejoin="round" />,
    trash: (
      <path d="M4 7h16M9.5 7V4.8A.8.8 0 0 1 10.3 4h3.4a.8.8 0 0 1 .8.8V7M6.5 7l1 13h9l1-13M10 11v5M14 11v5" strokeLinecap="round" />
    ),
    check: <path d="m5 12.5 4.5 4.5L19 7.5" strokeLinecap="round" strokeLinejoin="round" />,
    lock: (
      <>
        <rect x="5" y="10.5" width="14" height="9.5" rx="2.5" />
        <path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" />
      </>
    ),
    undo: <path d="M8.5 4.5 4 9l4.5 4.5M4 9h9a6 6 0 0 1 0 12h-3" strokeLinecap="round" strokeLinejoin="round" />,
    phone: <path d="M6 3.5h3l1.5 4.5-2 1.5a12 12 0 0 0 5.5 5.5l1.5-2 4.5 1.5v3a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4 5.7 2 2 0 0 1 6 3.5Z" strokeLinejoin="round" />,
    sparkle: <path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9Z" strokeLinejoin="round" />,
    wallet: (
      <>
        <rect x="3" y="6" width="18" height="14" rx="3" />
        <path d="M3 10h18M16 15h2" strokeLinecap="round" />
      </>
    ),
  };
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      className={className}
      aria-hidden="true"
    >
      {paths[name] ?? null}
    </svg>
  );
}

// ===== رقم مالي متحرك =====
export function MoneyText({ value, suffix = " ر.ي", className = "" }: { value: number; suffix?: string; className?: string }) {
  const [shown, setShown] = useState(value);
  const prev = useRef(value);
  useEffect(() => {
    const stop = animateNumber(prev.current, value, setShown);
    prev.current = value;
    return stop;
  }, [value]);
  return (
    <span className={`tnum ${className}`} dir="ltr">
      {formatRials(shown)}
      {suffix}
    </span>
  );
}

export function Rial({ v }: { v: number }) {
  return (
    <span className="tnum" dir="ltr">
      {formatRials(v)}
    </span>
  );
}

// ===== شريط حالة =====
export function StatusPill({ kind, children }: { kind: "paid" | "credit" | "avail" | "sold"; children: ReactNode }) {
  const cls = {
    paid: "bg-good-soft text-good",
    credit: "bg-credit-soft text-credit",
    avail: "bg-good-soft text-good",
    sold: "bg-line/60 text-ink-soft",
  }[kind];
  const dot = { paid: "bg-good", credit: "bg-credit", avail: "bg-good", sold: "bg-ink-faint" }[kind];
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${cls}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${dot}`} />
      {children}
    </span>
  );
}

// ===== شيت سفلي =====
export function Sheet({
  open,
  onClose,
  title,
  children,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
}) {
  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            className="fixed inset-0 z-[60] bg-ink/45 backdrop-blur-[2px]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.div
            className="fixed inset-x-0 bottom-0 z-[70] mx-auto max-h-[88dvh] w-full max-w-md overflow-y-auto rounded-t-3xl bg-paper shadow-sheet"
            initial={{ y: "100%" }}
            animate={{ y: 0 }}
            exit={{ y: "100%" }}
            transition={{ type: "spring", damping: 30, stiffness: 320 }}
          >
            <div className="sticky top-0 z-10 flex items-center justify-between border-b border-line bg-paper/95 px-5 py-4 backdrop-blur">
              <h3 className="font-display text-lg font-bold">{title}</h3>
              <button
                onClick={onClose}
                className="grid h-9 w-9 place-items-center rounded-full bg-line/60 text-ink-soft transition active:scale-90"
                aria-label="إغلاق"
              >
                <Icon name="close" className="h-4.5 w-4.5" />
              </button>
            </div>
            <div className="px-5 pb-8 pt-4">{children}</div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}

// ===== حالة فارغة =====
export function EmptyState({ icon, title, hint }: { icon: string; title: string; hint?: string }) {
  return (
    <div className="rise-in flex flex-col items-center gap-2 rounded-2xl border border-dashed border-line bg-card/60 px-6 py-10 text-center">
      <div className="grid h-14 w-14 place-items-center rounded-2xl bg-flame-soft text-flame">
        <Icon name={icon} className="h-7 w-7" />
      </div>
      <p className="mt-1 font-semibold text-ink">{title}</p>
      {hint && <p className="text-sm text-ink-faint">{hint}</p>}
    </div>
  );
}

// ===== عدّاد كمية =====
export function Stepper({
  value,
  onChange,
  min = 1,
  max = 100,
}: {
  value: number;
  onChange: (v: number) => void;
  min?: number;
  max?: number;
}) {
  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={() => onChange(Math.max(min, value - 1))}
        className="grid h-11 w-11 place-items-center rounded-xl border-1.5 border-line bg-card text-ink transition active:scale-90 disabled:opacity-40"
        disabled={value <= min}
        aria-label="إنقاص"
      >
        <Icon name="minus" className="h-5 w-5" />
      </button>
      <motion.div
        key={value}
        initial={{ scale: 1.25, color: "#e8590c" }}
        animate={{ scale: 1, color: "#211d19" }}
        className="min-w-12 text-center font-display text-2xl font-bold tnum"
      >
        {value}
      </motion.div>
      <button
        type="button"
        onClick={() => onChange(Math.min(max, value + 1))}
        className="grid h-11 w-11 place-items-center rounded-xl border-1.5 border-line bg-card text-ink transition active:scale-90 disabled:opacity-40"
        disabled={value >= max}
        aria-label="زيادة"
      >
        <Icon name="plus" className="h-5 w-5" />
      </button>
    </div>
  );
}

// ===== مبدّل مقسّم (فترات التقرير) =====
export function Segmented<T extends string>({
  options,
  value,
  onChange,
}: {
  options: { v: T; label: string }[];
  value: T;
  onChange: (v: T) => void;
}) {
  return (
    <div className="flex rounded-2xl border border-line bg-card p-1">
      {options.map((o) => {
        const active = o.v === value;
        return (
          <button
            key={o.v}
            onClick={() => onChange(o.v)}
            className={`relative flex-1 rounded-xl px-2 py-2 text-sm font-semibold transition-colors ${
              active ? "text-white" : "text-ink-soft active:text-ink"
            }`}
          >
            {active && (
              <motion.span
                layoutId="seg-pill"
                className="absolute inset-0 rounded-xl bg-flame"
                transition={{ type: "spring", damping: 28, stiffness: 400 }}
              />
            )}
            <span className="relative z-10">{o.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ===== مقياس سائل للمخزون =====
export function LiquidGauge({ available, sold }: { available: number; sold: number }) {
  const total = available + sold;
  const pct = total === 0 ? 0 : Math.round((available / total) * 100);
  const fill = total === 0 ? 0 : Math.max(available / total, 0.04);
  const low = total > 0 && available <= Math.max(2, Math.round(total * 0.15));
  return (
    <div className="relative h-44 overflow-hidden rounded-3xl border border-line bg-card">
      {/* التعبئة */}
      <motion.div
        className={`absolute inset-x-0 bottom-0 ${low ? "bg-bad/85" : "bg-flame/90"}`}
        initial={{ height: "0%" }}
        animate={{ height: `${fill * 100}%` }}
        transition={{ type: "spring", damping: 20, stiffness: 90 }}
      >
        {/* موجتان */}
        <div className="absolute -top-3 left-0 h-4 w-[200%] wave-band opacity-90">
          <svg viewBox="0 0 400 16" preserveAspectRatio="none" className="h-4 w-full">
            <path d="M0 8 Q 25 0 50 8 T 100 8 T 150 8 T 200 8 T 250 8 T 300 8 T 350 8 T 400 8 V 16 H 0 Z" fill={low ? "#d6336c" : "#e8590c"} opacity="0.9" />
          </svg>
        </div>
      </motion.div>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <motion.p
          key={available}
          initial={{ scale: 1.3 }}
          animate={{ scale: 1 }}
          className="font-display text-5xl font-black text-ink drop-shadow-[0_1px_0_rgba(255,255,255,0.65)] tnum"
        >
          {available}
        </motion.p>
        <p className="text-sm font-semibold text-ink/80">أسطوانة متوفرة</p>
        <p className="mt-0.5 text-xs text-ink/60 tnum">{pct}% من السعة الكلية ({total})</p>
      </div>
    </div>
  );
}

// ===== دونات متحرك (مقبوض/آجل) =====
export function Donut({ paid, credit }: { paid: number; credit: number }) {
  const total = Math.max(paid + credit, 1);
  const paidPct = paid / total;
  const R = 54;
  const C = 2 * Math.PI * R;
  return (
    <div className="relative h-36 w-36">
      <svg viewBox="0 0 140 140" className="h-full w-full -rotate-90">
        <circle cx="70" cy="70" r={R} fill="none" stroke="#e5ddce" strokeWidth="16" />
        <motion.circle
          cx="70"
          cy="70"
          r={R}
          fill="none"
          stroke="#e8890c"
          strokeWidth="16"
          strokeLinecap="round"
          strokeDasharray={C}
          initial={{ strokeDashoffset: 0 }}
          animate={{ strokeDashoffset: C * (1 - 1) }}
          style={{ strokeDashoffset: 0 }}
        />
        <motion.circle
          cx="70"
          cy="70"
          r={R}
          fill="none"
          stroke="#2f9e44"
          strokeWidth="16"
          strokeLinecap="round"
          strokeDasharray={C}
          initial={{ strokeDashoffset: C }}
          animate={{ strokeDashoffset: C * (1 - paidPct) }}
          transition={{ duration: 0.9, ease: "easeOut" }}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <p className="font-display text-lg font-black tnum">{Math.round(paidPct * 100)}%</p>
        <p className="text-[11px] font-semibold text-ink-faint">مقبوض</p>
      </div>
    </div>
  );
}

// ===== حقل بعنوان =====
export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-semibold text-ink-soft">{label}</span>
      {children}
    </label>
  );
}

// ===== زر رئيسي بلهب =====
export function FlameButton({
  children,
  onClick,
  disabled,
  tone = "flame",
  type = "button",
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  tone?: "flame" | "ghost" | "danger";
  type?: "button" | "submit";
}) {
  const cls =
    tone === "flame"
      ? "bg-flame text-white shadow-[0_6px_18px_rgba(232,89,12,0.35)] hover:bg-flame-deep"
      : tone === "danger"
        ? "bg-bad-soft text-bad border border-bad/30"
        : "border-1.5 border-line bg-card text-ink";
  return (
    <motion.button
      type={type}
      whileTap={{ scale: 0.96 }}
      onClick={onClick}
      disabled={disabled}
      className={`flex w-full items-center justify-center gap-2 rounded-2xl px-5 py-3.5 font-display text-base font-bold transition disabled:opacity-50 ${cls}`}
    >
      {children}
    </motion.button>
  );
}
