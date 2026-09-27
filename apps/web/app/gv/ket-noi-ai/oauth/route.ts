import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { requireRole } from "@/lib/auth";
import { oauthDaDangKy, taoPkce, urlDangNhapChatGpt } from "@/lib/openai-oauth";

export async function GET() {
  await requireRole("GV");
  if (!oauthDaDangKy()) {
    return NextResponse.redirect(new URL("/gv/ket-noi-ai?loi=" + encodeURIComponent("Chưa có OPENAI_OAUTH_CLIENT_ID do OpenAI cấp. Dùng khóa API cùng tài khoản ChatGPT."), process.env.APP_URL || "http://127.0.0.1:3000"));
  }
  const pkce = taoPkce();
  const jar = await cookies();
  const exp = new Date(Date.now() + 10 * 60 * 1000);
  jar.set("chatgpt_oauth_state", pkce.state, { httpOnly: true, sameSite: "lax", path: "/", expires: exp });
  jar.set("chatgpt_oauth_verifier", pkce.verifier, { httpOnly: true, sameSite: "lax", path: "/", expires: exp });
  return NextResponse.redirect(urlDangNhapChatGpt({ state: pkce.state, challenge: pkce.challenge }));
}
