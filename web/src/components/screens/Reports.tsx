"use client";

import { useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import { apiGet } from "@/lib/client";
import { formatDateTime, formatRials } from "@/lib/money";
import type { ReportPeriod, Sale } from "@/lib/types";
import { useStore } from "@/components/store";
import { Donut, EmptyState, Icon, MoneyText, Rial, Segmented, StatusPill } from "@/components/ui";

const PERIODS: { v: ReportPeriod; label: string }[] = [
  { v: "ALL", label: "الكل" },
  { v: "MONTH", label: "الشهر" },
  { v: "WEEK", label: "الأسبوع" },
  { v: "DAY", label: "اليوم" },
];

/** شاشة التقارير — إحصاءات الفترة + الدونات + السجل الكامل (مرآة Reports+SalesHistory). */
export function ReportsScreen() {
  const { state, loading, period, setPeriod, run } = useStore();
  const [historyOpen, setHistoryOpen] = useState(false);

  if (loading || !state)
    return (
      <div className="space-y-4">
        <div className="skeleton h-12 rounded-2xl" />
        <div className="grid grid-cols-2 gap-3">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skeleton h-24 rounded-2xl" />
          ))}
        </div>
      </div>
    );

  const s = state.stats;
  const paidInPeriod = Math.min(s.totalPaid, s.totalSales);
  const creditInPeriod = Math.max(s.totalSales - paidInPeriod, 0);

  return (
    <div className="space-y-4">
      <Segmented options={PERIODS} value={period} onChange={setPeriod} />

      {/* البطاقات الرئيسية */}
      <div className="grid grid-cols-2 gap-3">
        <StatCard title="المبيعات" value={s.totalSales} tone="flame" icon="chart" />
        <StatCard title="الربح" value={s.profit} tone={s.profit >= 0 ? "good" : "bad"} icon="sparkle" />
        <StatCard title="التكلفة" value={s.totalCost} tone="muted" icon="wallet" />
        <StatCard title="الدين المتبقي (الكل)" value={s.totalCredit} tone={s.totalCredit > 0 ? "credit" : "good"} icon="users" />
      </div>

      {/* المقبوض مقابل الآجل */}
      <motion.div layout className="flex items-center gap-4 rounded-3xl border border-line bg-card p-4 shadow-lift">
        <Donut paid={paidInPeriod} credit={creditInPeriod} />
        <div className="flex-1 space-y-2 text-sm">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 font-semibold text-ink-soft">
              <span className="h-2.5 w-2.5 rounded-full bg-good" /> مقبوض
            </span>
            <MoneyText value={paidInPeriod} className="font-bold text-good" />
          </div>
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 font-semibold text-ink-soft">
              <span className="h-2.5 w-2.5 rounded-full bg-credit" /> آجل
            </span>
            <MoneyText value={creditInPeriod} className="font-bold text-credit" />
          </div>
          <p className="border-t border-line pt-2 text-xs text-ink-faint">
            التحصيلات المستقلة تدخل في «المقبوض» — الدين المتبقي أعلاه حالة حالية بلا فلترة فترة
          </p>
        </div>
      </motion.div>

      {/* أعلى المدينين */}
      {state.topDebtors.length > 0 && (
        <div>
          <p className="mb-2 text-sm font-bold text-ink-soft">أعلى المدينين</p>
          <div className="space-y-2">
            {state.topDebtors.map((c, i) => (
              <motion.div
                key={c.id}
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.08 }}
                className="flex items-center justify-between rounded-2xl border border-line bg-card px-4 py-3 shadow-lift"
              >
                <span className="font-bold">{c.name}</span>
                <span className="font-black text-bad tnum">{formatRials(c.balance)} ريال</span>
              </motion.div>
            ))}
          </div>
        </div>
      )}

      {/* أحدث المبيعات */}
      <div>
        <div className="mb-2 flex items-center justify-between">
          <p className="text-sm font-bold text-ink-soft">أحدث المبيعات</p>
          <button
            onClick={() => setHistoryOpen((v) => !v)}
            className="text-sm font-bold text-flame transition active:scale-95"
          >
            {historyOpen ? "إخفاء السجل الكامل" : "السجل الكامل ←"}
          </button>
        </div>
        {historyOpen ? (
          <FullHistory />
        ) : state.recentSales.length === 0 ? (
          <EmptyState icon="chart" title="لا مبيعات في هذه الفترة" hint="أول عملية بيع ستظهر هنا فوراً" />
        ) : (
          <div className="space-y-2">
            {state.recentSales.slice(0, 6).map((sale) => (
              <SaleRow key={sale.id} sale={sale} onCancel={() => run("cancelSale", { saleId: sale.id })} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function StatCard({ title, value, tone, icon }: { title: string; value: number; tone: "flame" | "good" | "bad" | "credit" | "muted"; icon: string }) {
  const text = { flame: "text-flame", good: "text-good", bad: "text-bad", credit: "text-credit", muted: "text-ink" }[tone];
  return (
    <motion.div layout className="rounded-2xl border border-line bg-card p-4 shadow-lift">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold text-ink-faint">{title}</p>
        <Icon name={icon} className={`h-4.5 w-4.5 ${text}`} />
      </div>
      <MoneyText value={value} className={`mt-1 block font-display text-xl font-black ${text}`} />
    </motion.div>
  );
}

export function SaleRow({ sale, onCancel }: { sale: Sale; onCancel?: () => void }) {
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-2xl border border-line bg-card p-3.5 shadow-lift"
    >
      <div className="flex items-center justify-between gap-2">
        <div className="min-w-0">
          <p className="truncate font-bold">{sale.customerName}</p>
          <p className="text-xs text-ink-faint tnum">
            {sale.unitsSold} أسطوانة × <Rial v={sale.pricePerUnit} /> — {formatDateTime(sale.saleDate)}
          </p>
        </div>
        <div className="text-left">
          <p className="font-black tnum">{formatRials(sale.totalAmount)} ريال</p>
          <StatusPill kind={sale.status === "PAID" ? "paid" : "credit"}>
            {sale.status === "PAID" ? "مدفوع" : `متبقٍ ${formatRials(sale.totalAmount - sale.amountPaid)}`}
          </StatusPill>
        </div>
      </div>
      {sale.notes && <p className="mt-1.5 rounded-lg bg-paper px-2 py-1 text-xs text-ink-soft">📝 {sale.notes}</p>}
      {onCancel && (
        <div className="mt-2 flex justify-end">
          <button
            onClick={onCancel}
            className="flex items-center gap-1 rounded-lg bg-bad-soft px-2.5 py-1.5 text-xs font-bold text-bad transition active:scale-95"
          >
            <Icon name="undo" className="h-3.5 w-3.5" /> إلغاء
          </button>
        </div>
      )}
    </motion.div>
  );
}

/** السجل الكامل — ترقيم صفحات وبحث وفلترة تاريخ (مرآة SalesHistoryScreen.kt). */
function FullHistory() {
  const { run } = useStore();
  const [q, setQ] = useState("");
  const [rows, setRows] = useState<Sale[]>([]);
  const [count, setCount] = useState(0);
  const [page, setPage] = useState(0);
  const [loadingRows, setLoadingRows] = useState(true);
  const PAGE = 20;

  useEffect(() => {
    let alive = true;
    setLoadingRows(true);
    const url = q.trim()
      ? `/api/sales?q=${encodeURIComponent(q.trim())}`
      : `/api/sales?limit=${PAGE}&offset=${page * PAGE}`;
    apiGet<{ sales: Sale[]; count: number }>(url)
      .then((d) => {
        if (!alive) return;
        setRows(d.sales);
        setCount(d.count);
      })
      .catch(() => {})
      .finally(() => alive && setLoadingRows(false));
    return () => {
      alive = false;
    };
  }, [q, page]);

  const pages = useMemo(() => Math.max(Math.ceil(count / PAGE), 1), [count]);

  return (
    <div className="rise-in space-y-3">
      <div className="relative">
        <Icon name="search" className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-faint" />
        <input className="field pr-12" placeholder="ابحث باسم الزبون أو الملاحظات…" value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} />
      </div>
      {loadingRows ? (
        <div className="space-y-2">
          {[0, 1, 2].map((i) => (
            <div key={i} className="skeleton h-20 rounded-2xl" />
          ))}
        </div>
      ) : rows.length === 0 ? (
        <EmptyState icon="search" title="لا نتائج" hint="جرّب بحثاً آخر" />
      ) : (
        <div className="space-y-2">
          {rows.map((sale) => (
            <SaleRow key={sale.id} sale={sale} onCancel={() => run("cancelSale", { saleId: sale.id })} />
          ))}
        </div>
      )}
      {!q.trim() && pages > 1 && (
        <div className="flex items-center justify-center gap-4 pt-1 text-sm font-bold">
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            className="rounded-xl border border-line bg-card px-4 py-2 disabled:opacity-40"
          >
            الأحدث
          </button>
          <span className="text-ink-faint tnum">صفحة {page + 1} / {pages}</span>
          <button
            disabled={page >= pages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-xl border border-line bg-card px-4 py-2 disabled:opacity-40"
          >
            الأقدم
          </button>
        </div>
      )}
    </div>
  );
}
