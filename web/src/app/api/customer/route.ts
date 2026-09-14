import { NextResponse } from "next/server";
import { getCustomerDetail } from "@/lib/logic";

export async function GET(req: Request) {
  const id = new URL(req.url).searchParams.get("id");
  if (!id) return NextResponse.json({ ok: false, error: "معرّف الزبون مطلوب" }, { status: 400 });
  try {
    return NextResponse.json({ ok: true, data: await getCustomerDetail(id) });
  } catch {
    return NextResponse.json({ ok: false, error: "حدث خطأ غير متوقع" }, { status: 500 });
  }
}
