"use client";

import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { apiGet, mutate } from "@/lib/client";
import { StoreProvider, useStore } from "@/components/store";
import { Icon } from "@/components/ui";
import { DispenseScreen } from "@/components/screens/Dispense";
import { InventoryScreen } from "@/components/screens/Inventory";
import { CustomersScreen } from "@/components/screens/Customers";
import { ReportsScreen } from "@/components/screens/Reports";
import { StationScreen } from "@/components/screens/Station";
import { SettingsScreen } from "@/components/screens/Settings";

type Tab = "dispense" | "inventory" | "customers" | "reports" | "station" | "settings";

const TABS: { id: Tab; label: string; icon: string }[] = [
  { id: "dispense", label: "توزيع", icon: "flame" },
  { id: "inventory", label: "المخزون", icon: "box" },
  { id: "customers", label: "الزبائن", icon: "users" },
  { id: "reports", label: "التقارير", icon: "chart" },
  { id: "station", label: "المحطة", icon: "truck" },
];

export default function Page() {
  return (
    <StoreProvider>
      <Shell />
    </StoreProvider>
  );
}

function Shell() {
  const { state } = useStore();
  const [tab, setTab] = useState<Tab>("dispense");
  const [pinState, setPinState] = useState<"loading" | "locked" | "open">("loading");
  const [pinSet, setPinSet] = useState(false);

  useEffect(() => {
    apiGet<{ isSet: boolean }>("/api/pin")
      .then((d) => {
        setPinSet(d.isSet);
        setPinState(d.isSet ? "locked" : "open");
      })
      .catch(() => setPinState("open"));
  }, []);

  if (pinState === "loading") {
    return (
      <div className="grid min-h-dvh place-items-center">
        <Icon name="flame" className="flame-live h-14 w-14 text-flame" />
      </div>
    );
  }

  if (pinState === "locked") {
    return (
      <PinLock
        onUnlock={() => setPinState("open")}
      />
    );
  }

  const title =
    tab === "settings" ? "الإعدادات" : TABS.find((t) => t.id === tab)?.label ?? "";

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-md flex-col bg-paper">
      {/* الترويسة */}
      <header className="sticky top-0 z-40 border-b border-line bg-paper/90 backdrop-blur">
        <div className="flex items-center justify-between px-5 py-3.5">
          <div className="flex items-center gap-2.5">
            <span className="grid h-10 w-10 place-items-center rounded-2xl bg-flame text-white shadow-[0_4px_14px_rgba(232,89,12,0.4)]">
              <Icon name="flame" className="flame-live h-5.5 w-5.5" />
            </span>
            <div>
              <h1 className="font-display text-lg font-black leading-none">دَبّ للغاز</h1>
              <p className="mt-0.5 text-[11px] font-semibold text-ink-faint">{title}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {state && (
              <span className="hidden rounded-full bg-flame-soft px-3 py-1.5 text-xs font-bold text-flame-deep tnum min-[380px]:inline-block">
                متوفر: {state.stats.availableCount}
              </span>
            )}
            <button
              onClick={() => setTab(tab === "settings" ? "dispense" : "settings")}
              className={`grid h-10 w-10 place-items-center rounded-xl border transition active:scale-90 ${
                tab === "settings" ? "border-flame bg-flame text-white" : "border-line bg-card text-ink-soft"
              }`}
              aria-label="الإعدادات"
            >
              <Icon name={tab === "settings" ? "back" : "gear"} className="h-5 w-5" />
            </button>
          </div>
        </div>
      </header>

      {/* المحتوى بانتقالات متحركة */}
      <main className="flex-1 px-4 pb-28 pt-4">
        <AnimatePresence mode="wait">
          <motion.div
            key={tab}
            initial={{ opacity: 0, x: -18 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 18 }}
            transition={{ duration: 0.22, ease: [0.2, 0.7, 0.2, 1] }}
          >
            {tab === "dispense" && <DispenseScreen />}
            {tab === "inventory" && <InventoryScreen />}
            {tab === "customers" && <CustomersScreen />}
            {tab === "reports" && <ReportsScreen />}
            {tab === "station" && <StationScreen />}
            {tab === "settings" && (
              <SettingsScreen
                pinSet={pinSet}
                onPinChanged={(v) => {
                  setPinSet(v);
                  setTab("dispense");
                }}
              />
            )}
          </motion.div>
        </AnimatePresence>
      </main>

      {/* شريط التنقل السفلي */}
      <nav className="fixed inset-x-0 bottom-0 z-50 mx-auto max-w-md">
        <div className="mx-3 mb-3 flex items-stretch justify-between rounded-3xl bg-night px-2 py-2 shadow-[0_10px_30px_rgba(33,29,25,0.35)]">
          {TABS.map((t) => {
            const active = tab === t.id;
            return (
              <button
                key={t.id}
                onClick={() => setTab(t.id)}
                className="relative flex flex-1 flex-col items-center gap-1 rounded-2xl py-2"
                aria-label={t.label}
              >
                {active && (
                  <motion.span
                    layoutId="nav-pill"
                    className="absolute inset-0 rounded-2xl bg-night-2"
                    transition={{ type: "spring", damping: 26, stiffness: 380 }}
                  />
                )}
                <span className={`relative z-10 transition-colors ${active ? "text-flame" : "text-paper/55"}`}>
                  <Icon name={t.icon} className="h-5.5 w-5.5" />
                </span>
                <span className={`relative z-10 text-[10px] font-bold transition-colors ${active ? "text-paper" : "text-paper/45"}`}>
                  {t.label}
                </span>
                {active && (
                  <motion.span
                    layoutId="nav-dot"
                    className="relative z-10 h-1 w-1 rounded-full bg-flame"
                    transition={{ type: "spring", damping: 26, stiffness: 380 }}
                  />
                )}
              </button>
            );
          })}
        </div>
      </nav>
    </div>
  );
}

// ===== قفل رمز الدخول =====
function PinLock({ onUnlock }: { onUnlock: () => void }) {
  const [pin, setPin] = useState("");
  const [error, setError] = useState("");
  const [lockMs, setLockMs] = useState(0);
  const [shakeKey, setShakeKey] = useState(0);

  useEffect(() => {
    if (lockMs <= 0) return;
    const t = window.setInterval(() => setLockMs((m) => Math.max(m - 1000, 0)), 1000);
    return () => window.clearInterval(t);
  }, [lockMs]);

  const press = async (d: string) => {
    if (lockMs > 0) return;
    if (d === "del") return setPin((p) => p.slice(0, -1));
    if (pin.length >= 4) return;
    const next = pin + d;
    setPin(next);
    if (next.length === 4) {
      const r = await mutate<{ lockRemainingMs?: number }>("tryPin", { pin: next });
      if (r.ok) return onUnlock();
      setError(r.error ?? "رمز خاطئ");
      setLockMs(r.data?.lockRemainingMs ?? 0);
      setShakeKey((k) => k + 1);
      setPin("");
    }
  };

  const secs = Math.ceil(lockMs / 1000);
  const lockText =
    secs >= 3600
      ? `${Math.floor(secs / 3600)} س ${Math.floor((secs % 3600) / 60)} د`
      : secs >= 60
        ? `${Math.floor(secs / 60)} د ${secs % 60} ث`
        : `${secs} ث`;

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-md flex-col items-center justify-center gap-6 px-8">
      <motion.div initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className="text-center">
        <span className="mx-auto grid h-16 w-16 place-items-center rounded-3xl bg-flame text-white shadow-[0_8px_24px_rgba(232,89,12,0.45)]">
          <Icon name="lock" className="h-8 w-8" />
        </span>
        <h2 className="mt-4 font-display text-xl font-black">التطبيق مقفل</h2>
        <p className="mt-1 text-sm text-ink-faint">أدخل رمز الدخول المكوّن من 4 أرقام</p>
      </motion.div>

      {/* النقاط */}
      <motion.div key={shakeKey} className={error && pin === "" ? "shake flex gap-3" : "flex gap-3"}>
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className={`h-3.5 w-3.5 rounded-full border-2 transition-all ${
              i < pin.length ? "scale-110 border-flame bg-flame" : "border-ink-faint bg-transparent"
            }`}
          />
        ))}
      </motion.div>

      <AnimatePresence>
        {(error || lockMs > 0) && (
          <motion.p
            initial={{ opacity: 0, y: -6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="rounded-xl bg-bad-soft px-4 py-2 text-center text-xs font-bold text-bad"
          >
            {lockMs > 0 ? `القفل مفعّل — أعد المحاولة بعد ${lockText}` : error}
          </motion.p>
        )}
      </AnimatePresence>

      {/* لوحة الأرقام */}
      <div className="grid w-full max-w-70 grid-cols-3 gap-3">
        {["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del"].map((k, i) =>
          k === "" ? (
            <span key={i} />
          ) : (
            <motion.button
              key={k}
              whileTap={{ scale: 0.9 }}
              onClick={() => press(k)}
              disabled={lockMs > 0}
              className="grid h-16 place-items-center rounded-2xl border border-line bg-card font-display text-xl font-bold shadow-lift transition disabled:opacity-40"
            >
              {k === "del" ? <Icon name="back" className="h-5 w-5 rotate-180" /> : k}
            </motion.button>
          )
        )}
      </div>
    </div>
  );
}
