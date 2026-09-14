import { NextResponse } from "next/server";
import { getPinStatus, getReducedMotion } from "@/lib/logic";

export async function GET() {
  try {
    const [pin, reducedMotion] = await Promise.all([getPinStatus(), getReducedMotion()]);
    return NextResponse.json({ ok: true, data: { ...pin, reducedMotion } });
  } catch {
    return NextResponse.json({ ok: false, error: "حدث خطأ غير متوقع" }, { status: 500 });
  }
}
