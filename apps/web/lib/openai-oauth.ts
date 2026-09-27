import { createHash, randomBytes } from "crypto";

/** OAuth chính thức Sign in with ChatGPT — chỉ khi trường có client_id do OpenAI cấp. Không dùng client_id Codex/CLI. */

export const OPENAI_AUTHORIZE = "https://auth.openai.com/oauth/authorize";
export const OPENAI_TOKEN = "https://auth.openai.com/oauth/token";
export const OPENAI_USERINFO = "https://auth.openai.com/oauth/userinfo";
export const OPENAI_KEYS_PAGE = "https://platform.openai.com/api-keys";

export function oauthDaDangKy() {
  return Boolean((process.env.OPENAI_OAUTH_CLIENT_ID || "").trim());
}

export function oauthClientId() {
  return (process.env.OPENAI_OAUTH_CLIENT_ID || "").trim();
}

export function oauthClientSecret() {
  return (process.env.OPENAI_OAUTH_CLIENT_SECRET || "").trim() || null;
}

export function oauthRedirectUri() {
  const base = (process.env.APP_URL || "http://127.0.0.1:3000").replace(/\/$/, "");
  return `${base}/gv/ket-noi-ai/callback`;
}

export function taoPkce() {
  const verifier = randomBytes(32).toString("base64url");
  const challenge = createHash("sha256").update(verifier).digest("base64url");
  const state = randomBytes(16).toString("base64url");
  return { verifier, challenge, state };
}

export function urlDangNhapChatGpt(opts: { state: string; challenge: string }) {
  const q = new URLSearchParams({
    client_id: oauthClientId(),
    redirect_uri: oauthRedirectUri(),
    response_type: "code",
    scope: "openid email profile",
    state: opts.state,
    code_challenge: opts.challenge,
    code_challenge_method: "S256",
  });
  return `${OPENAI_AUTHORIZE}?${q.toString()}`;
}

export async function doiCodeLayToken(opts: { code: string; verifier: string }) {
  const body = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: oauthClientId(),
    code: opts.code,
    redirect_uri: oauthRedirectUri(),
    code_verifier: opts.verifier,
  });
  const secret = oauthClientSecret();
  if (secret) body.set("client_secret", secret);
  const res = await fetch(OPENAI_TOKEN, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body,
    cache: "no-store",
  });
  if (!res.ok) {
    return { ok: false as const, loi: `OpenAI token ${res.status}. Không dùng client_id nội bộ, không thử lại im lặng.` };
  }
  const data = (await res.json()) as { access_token?: string; id_token?: string };
  if (!data.access_token) return { ok: false as const, loi: "OpenAI không trả access_token." };
  return { ok: true as const, accessToken: data.access_token, idToken: data.id_token || null };
}

export async function docHoSoChatGpt(accessToken: string) {
  const res = await fetch(OPENAI_USERINFO, {
    headers: { authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  });
  if (!res.ok) return { email: null as string | null, sub: null as string | null };
  const data = (await res.json()) as { email?: string; sub?: string };
  return { email: data.email || null, sub: data.sub || null };
}
