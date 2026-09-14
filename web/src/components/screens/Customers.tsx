"use client";

import { useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { apiGet } from "@/lib/client";
import { formatDateTime, formatRials } from "@/lib/money";
import { balanceOf, type Customer, type CustomerDetail } from "@/lib/types";
import { useStore } from "@/components/store";
import { EmptyState, Field, FlameButton, Icon, MoneyText, Rial, Sheet, StatusPill } from "@/components/ui";

/** شاشة الزبائن — قائمة وبحث وإنشاء + ملف زبون تفصيلي (مرآة Customers/CustomerDetail). */
export function CustomersScreen() {
  const { state, loading, run, toast } = useStore();
  const [q, setQ] = useState("");
  const [selected, setSelected] = useState<Customer | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [newName, setNewName] = useState("");
  const [newPhone, setNewPhone] = useState("");

  const customers = useMemo(() => {
    const list = state?.customers ?? [];
    const t = q.trim();
    if (!t) return list;
    return list.filter((c) => c.name.includes(t) || c.phone.includes(t));
  }, [state?.customers, q]);

  const submitAdd = async () => {
    const ok = await run("createCustomer", { name: newName, phone: newPhone }, `تمت إضافة الزبون «${newName.trim()}»`);
    if (ok) {
      setAddOpen(false);
      setNewName("");
      setNewPhone("");
    }
  };

  if (loading || !state)
    return (
      <div className="space-y-3">
        <div className="skeleton h-12 rounded-2xl" />
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="skeleton h-20 rounded-2xl" />
        ))}
      </div>
    );

  return (
    <div className="space-y-3">
      <div className="relative">
        <Icon name="search" className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-ink-faint" />
        <input
          className="field pr-12"
          placeholder="ابحث بالاسم أو الهاتف…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      <button
        onClick={() => setAddOpen(true)}
        className="flex w-full items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-flame/50 bg-flame-soft/50 px-4 py-3 font-semibold text-flame transition active:scale-[0.98]"
      >
        <Icon name="plus" className="h-5 w-5" />
        زبون جديد
      </button>

      {customers.length === 0 ? (
        <EmptyState
          icon="users"
          title={q ? "لا نتائج مطابقة" : "لا زبائن بعد"}
          hint={q ? "جرّب اسماً آخر" : "أضف زبونك الأول أو بِع مباشرة من تبويب التوزيع"}
        />
      ) : (
        <ul className="space-y-2">
          <AnimatePresence initial={false}>
            {customers.map((c, i) => {
              const bal = balanceOf(c);
              return (
                <motion.li
                  key={c.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: -30 }}
                  transition={{ delay: Math.min(i * 0.03, 0.3) }}
                >
                  <button
                    onClick={() => setSelected(c)}
                    className="flex w-full items-center gap-3 rounded-2xl border border-line bg-card p-3.5 text-right shadow-lift transition active:scale-[0.98]"
                  >
                    <span className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-flame-soft font-display text-lg font-bold text-flame">
                      {c.name.slice(0, 1)}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate font-bold">{c.name}</span>
                      <span className="block text-xs text-ink-faint tnum" dir="ltr">
                        {c.phone || "—"}
                      </span>
                    </span>
                    {bal > 0 ? (
                      <span className="text-left">
                        <span className="block text-sm font-black text-bad tnum">{formatRials(bal)}</span>
                        <span className="block text-[10px] font-semibold text-ink-faint">ريال دين</span>
                      </span>
                    ) : bal < 0 ? (
                      <span className="text-left">
                        <span className="block text-sm font-black text-good tnum">{formatRials(-bal)}</span>
                        <span className="block text-[10px] font-semibold text-ink-faint">رصيد له</span>
                      </span>
                    ) : (
                      <StatusPill kind="paid">خالص</StatusPill>
                    )}
                  </button>
                </motion.li>
              );
            })}
          </AnimatePresence>
        </ul>
      )}

      {/* إضافة زبون */}
      <Sheet open={addOpen} onClose={() => setAddOpen(false)} title="زبون جديد">
        <div className="space-y-4">
          <Field label="الاسم">
            <input className="field" value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="اسم الزبون" />
          </Field>
          <Field label="الهاتف (اختياري)">
            <input className="field tnum" dir="ltr" inputMode="tel" value={newPhone} onChange={(e) => setNewPhone(e.target.value)} />
          </Field>
          <FlameButton onClick={submitAdd} disabled={!newName.trim()}>
            إضافة الزبون
          </FlameButton>
          {state.customers.some((c) => c.name === newName.trim()) && newName.trim() && (
            <p className="rounded-xl bg-credit-soft px-3 py-2 text-xs font-bold text-credit">
              يوجد زبون بنفس الاسم — لن يُقبل التكرار
            </p>
          )}
        </div>
      </Sheet>

      {/* ملف الزبون */}
      <CustomerDetailSheet customer={selected} onClose={() => setSelected(null)} onDeleted={() => { setSelected(null); toast("تم حذف الزبون"); }} />
    </div>
  );
}

function CustomerDetailSheet({
  customer,
  onClose,
  onDeleted,
}: {
  customer: Customer | null;
  onClose: () => void;
  onDeleted: () => void;
}) {
  const { run, refresh } = useStore();
  const [detail, setDetail] = useState<CustomerDetail | null>(null);
  const [payOpen, setPayOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [amount, setAmount] = useState("");
  const [payNotes, setPayNotes] = useState("");
  const [eName, setEName] = useState("");
  const [ePhone, setEPhone] = useState("");

  useEffect(() => {
    if (!customer) return;
    let alive = true;
    setDetail(null);
    apiGet<CustomerDetail>(`/api/customer?id=${customer.id}`)
      .then((d) => alive && setDetail(d))
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, [customer]);

  if (!customer) return null;
  const bal = detail?.balance ?? balanceOf(customer);

  const submitPay = async () => {
    const v = Math.round(Number(amount.replace(/,/g, "")));
    const ok = await run("recordPayment", { customerId: customer.id, amount: v, notes: payNotes }, "تم تسجيل التحصيل");
    if (ok) {
      setPayOpen(false);
      setAmount("");
      setPayNotes("");
      const d = await apiGet<CustomerDetail>(`/api/customer?id=${customer.id}`);
      setDetail(d);
    }
  };

  const submitEdit = async () => {
    const ok = await run("updateCustomer", { id: customer.id, name: eName, phone: ePhone }, "تم حفظ التعديلات");
    if (ok) {
      setEditOpen(false);
      await refresh();
      const d = await apiGet<CustomerDetail>(`/api/customer?id=${customer.id}`);
      setDetail(d);
    }
  };

  const doDelete = async () => {
    const ok = await run("deleteCustomer", { id: customer.id });
    if (ok) onDeleted();
  };

  return (
    <Sheet open={!!customer} onClose={onClose} title={detail?.customer?.name ?? customer.name}>
      {/* بطاقة الرصيد */}
      <div className={`rounded-3xl p-5 text-center ${bal > 0 ? "bg-bad-soft" : bal < 0 ? "bg-good-soft" : "bg-card border border-line"}`}>
        <p className="text-sm font-semibold text-ink-soft">{bal > 0 ? "الدين المتبقي" : bal < 0 ? "رصيد دائن لصالح الزبون" : "الحساب خالص"}</p>
        <MoneyText value={Math.abs(bal)} className={`font-display text-4xl font-black ${bal > 0 ? "text-bad" : "text-good"}`} />
        {detail && (
          <div className="mt-3 flex justify-center gap-6 text-xs text-ink-soft">
            <span>اشترى: <b className="tnum">{formatRials(detail.totalBought)}</b></span>
            <span>دفع: <b className="tnum">{formatRials(detail.totalPaid)}</b></span>
          </div>
        )}
      </div>

      <div className="mt-4 grid grid-cols-2 gap-2">
        <FlameButton onClick={() => setPayOpen(true)} disabled={bal <= 0 && !detail}>
          <Icon name="wallet" className="h-5 w-5" /> تحصيل دفعة
        </FlameButton>
        <FlameButton tone="ghost" onClick={() => { setEName(detail?.customer?.name ?? customer.name); setEPhone(detail?.customer?.phone ?? customer.phone); setEditOpen(true); }}>
          تعديل البيانات
        </FlameButton>
      </div>

      {/* السجل */}
      {!detail ? (
        <div className="mt-4 space-y-2">
          <div className="skeleton h-14 rounded-xl" />
          <div className="skeleton h-14 rounded-xl" />
        </div>
      ) : detail.sales.length === 0 && detail.payments.length === 0 ? (
        <div className="mt-4">
          <EmptyState icon="box" title="لا عمليات بعد" hint="أول عملية بيع ستظهر هنا" />
        </div>
      ) : (
        <div className="mt-4 space-y-2">
          <p className="text-sm font-bold text-ink-soft">سجل المعاملات</p>
          {[...detail.sales.map((s) => ({ kind: "sale" as const, id: s.id, date: s.saleDate, s, p: null })),
            ...detail.payments.map((p) => ({ kind: "pay" as const, id: p.id, date: p.paymentDate, s: null, p }))]
            .sort((a, b) => b.date - a.date)
            .slice(0, 60)
            .map((row) => (
              <div key={row.kind + row.id} className="rounded-2xl border border-line bg-card p-3 text-sm">
                {row.kind === "sale" && row.s ? (
                  <>
                    <div className="flex items-center justify-between">
                      <span className="font-bold">بيع {row.s.unitsSold} أسطوانة</span>
                      <StatusPill kind={row.s.status === "PAID" ? "paid" : "credit"}>
                        {row.s.status === "PAID" ? "مدفوع" : "آجل"}
                      </StatusPill>
                    </div>
                    <div className="mt-1 flex items-center justify-between text-xs text-ink-faint">
                      <span className="tnum">{formatDateTime(row.s.saleDate)}</span>
                      <span className="font-bold text-ink tnum">{formatRials(row.s.totalAmount)} ريال</span>
                    </div>
                    {row.s.totalAmount - row.s.amountPaid > 0 && (
                      <p className="mt-1 text-xs font-semibold text-credit">المتبقي منها: {formatRials(row.s.totalAmount - row.s.amountPaid)} ريال</p>
                    )}
                    <div className="mt-2 flex justify-end">
                      <button
                        onClick={async () => {
                          const ok = await run("cancelSale", { saleId: row.s!.id });
                          if (ok) setDetail(await apiGet<CustomerDetail>(`/api/customer?id=${customer.id}`));
                        }}
                        className="flex items-center gap-1 rounded-lg bg-bad-soft px-2.5 py-1.5 text-xs font-bold text-bad transition active:scale-95"
                      >
                        <Icon name="undo" className="h-3.5 w-3.5" /> إلغاء البيع
                      </button>
                    </div>
                  </>
                ) : row.p ? (
                  <>
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-good">تحصيل دفعة</span>
                      <button
                        onClick={async () => {
                          const ok = await run("reversePayment", { paymentId: row.p!.id });
                          if (ok) setDetail(await apiGet<CustomerDetail>(`/api/customer?id=${customer.id}`));
                        }}
                        className="flex items-center gap-1 rounded-lg bg-bad-soft px-2 py-1 text-[11px] font-bold text-bad active:scale-95"
                      >
                        عكس
                      </button>
                    </div>
                    <div className="mt-1 flex items-center justify-between text-xs text-ink-faint">
                      <span className="tnum">{formatDateTime(row.p.paymentDate)}</span>
                      <span className="font-bold text-good tnum">+{formatRials(row.p.amount)} ريال</span>
                    </div>
                  </>
                ) : null}
              </div>
            ))}
        </div>
      )}

      <FlameButton tone="danger" onClick={doDelete}>
        <Icon name="trash" className="h-5 w-5" /> حذف الزبون نهائياً
      </FlameButton>

      {/* تحصيل */}
      <Sheet open={payOpen} onClose={() => setPayOpen(false)} title="تحصيل دفعة">
        <div className="space-y-4">
          <div className="rounded-2xl bg-credit-soft px-4 py-3 text-sm font-semibold text-credit">
            الدين الحالي: <Rial v={Math.max(bal, 0)} /> ريال — السداد الزائد يتحول رصيداً للزبون
          </div>
          <Field label="المبلغ">
            <input className="field tnum" inputMode="numeric" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="0" />
          </Field>
          <Field label="ملاحظات">
            <input className="field" value={payNotes} onChange={(e) => setPayNotes(e.target.value)} />
          </Field>
          <FlameButton onClick={submitPay} disabled={!amount.trim()}>
            تأكيد التحصيل
          </FlameButton>
        </div>
      </Sheet>

      {/* تعديل */}
      <Sheet open={editOpen} onClose={() => setEditOpen(false)} title="تعديل بيانات الزبون">
        <div className="space-y-4">
          <Field label="الاسم">
            <input className="field" value={eName} onChange={(e) => setEName(e.target.value)} />
          </Field>
          <Field label="الهاتف">
            <input className="field tnum" dir="ltr" value={ePhone} onChange={(e) => setEPhone(e.target.value)} />
          </Field>
          <FlameButton onClick={submitEdit} disabled={!eName.trim()}>
            حفظ
          </FlameButton>
        </div>
      </Sheet>
    </Sheet>
  );
}
