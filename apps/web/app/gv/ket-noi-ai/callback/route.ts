import { eq } from "drizzle-orm";
import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { auditLogs, classSettings } from "@/lib/db/schema";
import { docHoSoChatGpt, doiCodeLayToken } from "@/lib/openai-oauth";

export async function GET(req: Request) {
  const user = await requireRole("GV");
  const url = new URL(req.url);
  const origin = process.env.APP_URL || `${url.protocol}//${url.host}`;
  const fail = (msg: string) => NextResponse.redirect(new URL("/gv/ket-noi-ai?loi=" + encodeURIComponent(msg), origin));

  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");
  const jar = await cookies();
  const expectState = jar.get("chatgpt_oauth_state")?.value;
  const verifier = jar.get("chatgpt_oauth_verifier")?.value;
  jar.delete("chatgpt_oauth_state");
  jar.delete("chatgpt_oauth_verifier");
  if (!code || !state || !expectState || state !== expectState || !verifier) {
    return fail("Phiên đăng nhập ChatGPT không khớp. Không phát lại mã.");
  }
  const tok = await doiCodeLayToken({ code, verifier });
  if (!tok.ok) return fail(tok.loi);
  const hoSo = await docHoSoChatGpt(tok.accessToken);
  const rows = await db.select().from(classSettings);
  if (rows[0]) {
    await db
      .update(classSettings)
      .set({
        aiOpenaiSub: hoSo.sub,
        aiOpenaiEmail: hoSo.email,
        aiConnectedAt: new Date(),
      })
      .where(eq(classSettings.classId, rows[0].classId));
  }
  await db.insert(auditLogs).values({
    id: crypto.randomUUID(),
    actorUserId: user.id,
    action: "DANG_NHAP_CHATGPT_OAUTH",
    entity: "class_settings",
    entityId: rows[0]?.classId || null,
    at: new Date(),
    reason: "dinh_danh",
  });
  return NextResponse.redirect(new URL("/gv/ket-noi-ai?oauth=1", origin));
}
