import { NextResponse } from "next/server";
import { getSalesBetween, getSalesCount, getSalesPaged, searchSales } from "@/lib/logic";

export async function GET(req: Request) {
  const u = new URL(req.url);
  const q = u.searchParams.get("q");
  try {
    if (q && q.trim()) {
      return NextResponse.json({ ok: true, data: { sales: await searchSales(q), count: 0 } });
    }
    const from = Number(u.searchParams.get("from") ?? 0);
    const to = Number(u.searchParams.get("to") ?? 0);
    if (from > 0 && to > 0) {
      const sales = await getSalesBetween(from, to);
      return NextResponse.json({ ok: true, data: { sales, count: sales.length } });
    }
    const limit = Math.min(Number(u.searchParams.get("limit") ?? 50) || 50, 200);
    const offset = Math.max(Number(u.searchParams.get("offset") ?? 0) || 0, 0);
    const [sales, count] = await Promise.all([getSalesPaged(limit, offset), getSalesCount()]);
    return NextResponse.json({ ok: true, data: { sales, count } });
  } catch {
    return NextResponse.json({ ok: false, error: "حدث خطأ غير متوقع" }, { status: 500 });
  }
}
