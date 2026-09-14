import { NextResponse } from "next/server";
import { getStationData } from "@/lib/logic";

export async function GET() {
  try {
    return NextResponse.json({ ok: true, data: await getStationData() });
  } catch {
    return NextResponse.json({ ok: false, error: "حدث خطأ غير متوقع" }, { status: 500 });
  }
}
