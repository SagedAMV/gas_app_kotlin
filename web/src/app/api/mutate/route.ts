import { NextResponse } from "next/server";
import { BizError,
  cancelSale,
  cancelStationPayment,
  cancelStationPurchase,
  clearPin,
  createCustomer,
  deleteCustomer,
  payStation,
  purchaseFromStation,
  recordCustomerPayment,
  recordSale,
  reverseCustomerPayment,
  setDefaultPrice,
  setPin,
  setReducedMotion,
  tryUnlockPin,
  updateCustomer,
} from "@/lib/logic";

/**
 * نقطة كتابة موحّدة — كل عملية معدِّلة تمر من هنا (مرآة توحد دوال
 * AppViewModel في تطبيق Kotlin). كل عملية ذرّية في طبقة المنطق نفسها.
 */
type Body = Record<string, unknown>;

const str = (b: Body, k: string) => (typeof b[k] === "string" ? (b[k] as string) : "");
const num = (b: Body, k: string) => {
  const v = b[k];
  return typeof v === "number" && Number.isFinite(v) ? v : NaN;
};

export async function POST(req: Request) {
  let body: Body;
  try {
    body = (await req.json()) as Body;
  } catch {
    return NextResponse.json({ ok: false, error: "طلب غير صالح" }, { status: 400 });
  }
  const op = str(body, "op");
  try {
    switch (op) {
      case "recordSale": {
        const r = await recordSale({
          customerName: str(body, "customerName"),
          phone: str(body, "phone"),
          units: num(body, "units"),
          price: num(body, "price"),
          paidNow: num(body, "paidNow"),
          notes: str(body, "notes"),
        });
        return NextResponse.json({ ok: true, data: r });
      }
      case "cancelSale":
        await cancelSale(str(body, "saleId"));
        return NextResponse.json({ ok: true });
      case "recordPayment":
        await recordCustomerPayment(str(body, "customerId"), num(body, "amount"), str(body, "notes"));
        return NextResponse.json({ ok: true });
      case "reversePayment":
        await reverseCustomerPayment(str(body, "paymentId"));
        return NextResponse.json({ ok: true });
      case "purchaseFromStation":
        await purchaseFromStation({
          units: num(body, "units"),
          cost: num(body, "cost"),
          paidNow: num(body, "paidNow"),
          notes: str(body, "notes"),
        });
        return NextResponse.json({ ok: true });
      case "payStation":
        await payStation(num(body, "amount"), str(body, "notes"));
        return NextResponse.json({ ok: true });
      case "cancelStationPurchase":
        await cancelStationPurchase(str(body, "purchaseId"));
        return NextResponse.json({ ok: true });
      case "cancelStationPayment":
        await cancelStationPayment(str(body, "paymentId"));
        return NextResponse.json({ ok: true });
      case "createCustomer":
        await createCustomer(str(body, "name"), str(body, "phone"));
        return NextResponse.json({ ok: true });
      case "updateCustomer":
        await updateCustomer(str(body, "id"), str(body, "name"), str(body, "phone"));
        return NextResponse.json({ ok: true });
      case "deleteCustomer":
        await deleteCustomer(str(body, "id"));
        return NextResponse.json({ ok: true });
      case "setDefaultPrice":
        await setDefaultPrice(num(body, "price"));
        return NextResponse.json({ ok: true });
      case "setReducedMotion":
        await setReducedMotion(body.on === true);
        return NextResponse.json({ ok: true });
      case "setPin":
        await setPin(str(body, "pin"));
        return NextResponse.json({ ok: true });
      case "clearPin":
        await clearPin();
        return NextResponse.json({ ok: true });
      case "tryPin": {
        const r = await tryUnlockPin(str(body, "pin"));
        return NextResponse.json(r);
      }
      default:
        return NextResponse.json({ ok: false, error: "عملية غير معروفة" }, { status: 400 });
    }
  } catch (e) {
    if (e instanceof BizError) return NextResponse.json({ ok: false, error: e.message });
    // قيد قاعدة البيانات (مفتاح أجنبي / CHECK) — رسالة قابلة للفهم بدل تسريب تقني.
    const msg = e instanceof Error ? e.message : "";
    if (msg.includes("violates foreign key"))
      return NextResponse.json({ ok: false, error: "لا يمكن تنفيذ العملية — توجد سجلات مرتبطة" });
    if (msg.includes("violates check constraint"))
      return NextResponse.json({ ok: false, error: "قيمة خارج الحدود المسموحة" });
    return NextResponse.json({ ok: false, error: "حدث خطأ غير متوقع" }, { status: 500 });
  }
}
