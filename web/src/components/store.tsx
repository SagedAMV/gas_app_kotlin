"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { apiGet, mutate } from "@/lib/client";
import type { AppState, ReportPeriod } from "@/lib/types";

export interface Toast {
  id: number;
  msg: string;
  kind: "ok" | "err";
}

interface Store {
  state: AppState | null;
  loading: boolean;
  period: ReportPeriod;
  reducedMotion: boolean;
  setPeriod: (p: ReportPeriod) => void;
  refresh: () => Promise<void>;
  toast: (msg: string, kind?: "ok" | "err") => void;
  /** عملية معدِّلة + توست نجاح/فشل + تحديث الحالة. يعيد هل نجحت. */
  run: (op: string, payload?: Record<string, unknown>, successMsg?: string) => Promise<boolean>;
  confetti: () => void;
}

const Ctx = createContext<Store | null>(null);

export function useStore(): Store {
  const s = useContext(Ctx);
  if (!s) throw new Error("Store مفقود");
  return s;
}

export function StoreProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AppState | null>(null);
  const [loading, setLoading] = useState(true);
  const [period, setPeriodState] = useState<ReportPeriod>("ALL");
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [reducedMotion, setReducedMotion] = useState(false);
  const toastId = useRef(0);
  const busyRef = useRef(false);

  const toast = useCallback((msg: string, kind: "ok" | "err" = "ok") => {
    const id = ++toastId.current;
    setToasts((t) => [...t.slice(-2), { id, msg, kind }]);
    window.setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3200);
  }, []);

  const refresh = useCallback(async (p?: ReportPeriod) => {
    try {
      const data = await apiGet<AppState>(`/api/state?period=${p ?? "ALL"}`);
      setState(data);
    } catch {
      // فشل الجلب لا يمسح ما هو معروض — خطأ يُبلغ عبر توست مرة واحدة.
    } finally {
      setLoading(false);
    }
  }, []);

  const setPeriod = useCallback(
    (p: ReportPeriod) => {
      setPeriodState(p);
      void refresh(p);
    },
    [refresh]
  );

  useEffect(() => {
    void refresh("ALL");
    void apiGet<{ reducedMotion: boolean }>("/api/pin")
      .then((d) => setReducedMotion(d.reducedMotion))
      .catch(() => {});
  }, [refresh]);

  useEffect(() => {
    document.documentElement.classList.toggle("reduce-motion", reducedMotion);
  }, [reducedMotion]);

  // مزامنة فورية عند التبديل من شاشة الإعدادات (بدل انتظار إعادة الجلب).
  useEffect(() => {
    const h = (e: Event) => setReducedMotion((e as CustomEvent<boolean>).detail);
    window.addEventListener("reduced-motion", h);
    return () => window.removeEventListener("reduced-motion", h);
  }, []);

  const confetti = useCallback(() => {
    if (document.documentElement.classList.contains("reduce-motion")) return;
    const colors = ["#e8590c", "#f7b32b", "#2f9e44", "#d6336c", "#1971c2"];
    const root = document.body;
    for (let i = 0; i < 26; i++) {
      const el = document.createElement("span");
      el.className = "confetti-piece";
      el.style.right = `${Math.random() * 100}vw`;
      el.style.background = colors[i % colors.length];
      el.style.animationDuration = `${1.6 + Math.random() * 1.4}s`;
      el.style.animationDelay = `${Math.random() * 0.35}s`;
      root.appendChild(el);
      window.setTimeout(() => el.remove(), 3400);
    }
  }, []);

  const run = useCallback(
    async (op: string, payload: Record<string, unknown> = {}, successMsg?: string) => {
      // حاجز مزدوج في الواجهة (الذري الحقيقي في الخادم): يمنع ضغطتين سريعتين.
      if (busyRef.current) {
        toast("توجد عملية قيد التنفيذ — انتظر لحظة", "err");
        return false;
      }
      busyRef.current = true;
      try {
        const r = await mutate(op, payload);
        if (!r.ok) {
          toast(r.error ?? "حدث خطأ", "err");
          return false;
        }
        if (successMsg) toast(successMsg, "ok");
        await refresh(period);
        return true;
      } finally {
        busyRef.current = false;
      }
    },
    [period, refresh, toast]
  );

  const value = useMemo<Store>(
    () => ({
      state,
      loading,
      period,
      reducedMotion,
      setPeriod,
      refresh: () => refresh(period),
      toast,
      run,
      confetti,
    }),
    [state, loading, period, reducedMotion, setPeriod, refresh, toast, run, confetti]
  );

  return (
    <Ctx.Provider value={value}>
      {children}
      {/* مضيف التوست — أسفل الشاشة فوق شريط التنقل */}
      <div className="pointer-events-none fixed inset-x-0 bottom-24 z-[80] flex flex-col items-center gap-2 px-6">
        {toasts.map((t) => (
          <div
            key={t.id}
            role="status"
            className={`pop-in max-w-full rounded-xl px-4 py-2.5 text-sm font-medium shadow-lift ${
              t.kind === "ok" ? "bg-ink text-paper" : "bg-bad text-white"
            }`}
          >
            {t.msg}
          </div>
        ))}
      </div>
    </Ctx.Provider>
  );
}
