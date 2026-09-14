import { NextResponse } from "next/server";
import { getAppState } from "@/lib/logic";
import type { ReportPeriod } from "@/lib/types";

export async function GET(req: Request) {
  try {
    const p = (new URL(req.url).searchParams.get("period") ?? "ALL") as ReportPeriod;
    const period: ReportPeriod = ["ALL", "MONTH", "WEEK", "DAY"].includes(p) ? p : "ALL";
    return NextResponse.json({ ok: true, data: await getAppState(period) });
  } catch {
    return NextResponse.json({ ok: false, error: "حدث خطأ غير متوقع" }, { status: 500 });
  }
}
