import {
  AI_MAX_TOKENS_CHAT,
  AI_TIMEOUT_CHAT_MS,
  AI_TIMEOUT_PROBE_MS,
  CLOUD_MAC_DINH,
  LMSTUDIO_MAC_DINH,
  NHA,
  OLLAMA_MAC_DINH,
  OPENROUTER_MAC_DINH,
  OPENROUTER_MODEL_MAC_DINH,
  ZAI_MAC_DINH,
  ZAI_MODEL_MAC_DINH,
  chuanHoaBase,
  laDiaChiLoopback,
  laNhaKhoa,
  laUrlCloudHopLe,
  parseProvider,
  resolveProvider,
  thongBaoLoiNha,
  urlChat,
  urlModels,
  type AiLoiKind,
  type AiNhaKhoaId,
  type AiProviderId,
  type AiPublicConfig,
  type ChatMessage,
} from "./ai-catalog";

export type { AiLoiKind, AiNhaKhoaId, AiProviderId, ChatMessage };
export {
  AI_TIMEOUT_CHAT_MS,
  AI_TIMEOUT_PROBE_MS,
  CLOUD_MAC_DINH,
  LMSTUDIO_MAC_DINH,
  NHA,
  OLLAMA_MAC_DINH,
  OPENROUTER_MAC_DINH,
  ZAI_MAC_DINH,
  laDiaChiLoopback,
  laNhaKhoa,
  parseProvider,
  thongBaoLoiNha,
} from "./ai-catalog";

export type CompleteChatResult = {
  text: string;
  offline: boolean;
  provider: AiProviderId;
  model: string | null;
  error: string | null;
  errorKind: AiLoiKind | null;
};

export type ProbeResult = {
  ok: boolean;
  provider: AiProviderId;
  message: string;
  models: string[];
  base: string | null;
};

type FetchLike = typeof fetch;

function env(name: string, fallback = ""): string {
  return (process.env[name] || fallback).trim();
}

function bearerLocal(provider: AiProviderId): string {
  return provider === "lmstudio" ? "lm-studio" : "ollama";
}

export function docBaseLocal(provider: "ollama" | "lmstudio"): string {
  const raw = provider === "ollama" ? env("OLLAMA_BASE_URL", OLLAMA_MAC_DINH) : env("LMSTUDIO_BASE_URL", LMSTUDIO_MAC_DINH);
  const base = chuanHoaBase(raw || (provider === "ollama" ? OLLAMA_MAC_DINH : LMSTUDIO_MAC_DINH));
  return base;
}

export function docBaseCloud(): string {
  return chuanHoaBase(env("LLM_BASE_URL", CLOUD_MAC_DINH) || CLOUD_MAC_DINH);
}

/** OpenRouter / Z.AI: chỉ địa chỉ cứng. Cloud mới đọc LLM_BASE_URL. */
export function docBaseNhaKhoa(provider: AiNhaKhoaId): string {
  if (provider === "openrouter") return OPENROUTER_MAC_DINH;
  if (provider === "zai") return ZAI_MAC_DINH;
  return docBaseCloud();
}

export function docModel(provider: AiProviderId, override?: string | null): string {
  const tuChon = (override || "").trim();
  if (tuChon) return tuChon;
  if (provider === "ollama") return env("OLLAMA_MODEL", "llama3.2") || "llama3.2";
  if (provider === "lmstudio") return env("LMSTUDIO_MODEL", "local-model") || "local-model";
  if (provider === "cloud") return env("LLM_MODEL", "gpt-4o-mini") || "gpt-4o-mini";
  if (provider === "openrouter") return env("OPENROUTER_MODEL", OPENROUTER_MODEL_MAC_DINH) || OPENROUTER_MODEL_MAC_DINH;
  if (provider === "zai") return env("ZAI_MODEL", ZAI_MODEL_MAC_DINH) || ZAI_MODEL_MAC_DINH;
  return "";
}

/**
 * Env của đúng nhà, hoặc khóa lớp khi lớp đang chọn nhà đó.
 * Không có classProvider (form dán khóa / probe nhà đang nối) thì khóa dán dùng cho nhà đang gọi.
 */
export function docKhoaNha(
  provider: AiNhaKhoaId,
  classKey?: string | null,
  classProvider?: string | null,
): string | null {
  const fromEnv = env(NHA[provider].envKhoa || "LLM_API_KEY");
  if (fromEnv) return fromEnv;
  const pasted = (classKey || "").trim();
  if (!pasted) return null;
  if (classProvider == null) return pasted;
  return parseProvider(classProvider) === provider ? pasted : null;
}

export function docKhoaCloud(classKey?: string | null, classProvider?: string | null): string | null {
  return docKhoaNha("cloud", classKey, classProvider);
}

export function cauHinhCongKhai(opts: {
  classProvider?: string | null;
  classModel?: string | null;
  allowLocal?: boolean;
  classApiKey?: string | null;
}): AiPublicConfig {
  const lop = resolveProvider({ classProvider: opts.classProvider });
  return {
    classProvider: lop,
    classModel: (opts.classModel || "").trim() || null,
    allowLocal: opts.allowLocal !== false,
    cloudReady: Boolean(docKhoaNha("cloud", opts.classApiKey, lop)),
    openrouterReady: Boolean(docKhoaNha("openrouter", opts.classApiKey, lop)),
    zaiReady: Boolean(docKhoaNha("zai", opts.classApiKey, lop)),
  };
}

async function motLan(
  fetchFn: FetchLike,
  url: string,
  init: RequestInit,
  timeoutMs: number,
  ngoai?: AbortSignal,
): Promise<{ res?: Response; kind?: "timeout" | "network" | "aborted" }> {
  const ac = new AbortController();
  const t = setTimeout(() => ac.abort(), timeoutMs);
  const theoNgoai = () => ac.abort();
  ngoai?.addEventListener("abort", theoNgoai);
  try {
    if (ngoai?.aborted) return { kind: "aborted" };
    const res = await fetchFn(url, { ...init, signal: ac.signal, cache: "no-store" });
    return { res };
  } catch (e) {
    if (ngoai?.aborted) return { kind: "aborted" };
    if (ac.signal.aborted) return { kind: "timeout" };
    if (e instanceof Error && e.name === "AbortError") return { kind: "aborted" };
    return { kind: "network" };
  } finally {
    clearTimeout(t);
    ngoai?.removeEventListener("abort", theoNgoai);
  }
}

function thatBai(provider: AiProviderId, kind: AiLoiKind, chiTiet?: string): CompleteChatResult {
  return {
    text: thongBaoLoiNha(kind, provider, chiTiet),
    offline: true,
    provider,
    model: null,
    error: thongBaoLoiNha(kind, provider, chiTiet),
    errorKind: kind,
  };
}

function docNoiDung(msg: { content?: unknown }): string {
  const c = msg.content;
  if (typeof c === "string") return c.trim();
  if (Array.isArray(c)) {
    return c
      .map((p) => {
        if (typeof p === "string") return p;
        if (p && typeof p === "object" && "text" in p) return String((p as { text?: string }).text || "");
        return "";
      })
      .join("")
      .trim();
  }
  return "";
}

function headerChat(provider: AiProviderId, key: string): Record<string, string> {
  const headers: Record<string, string> = {
    "content-type": "application/json",
    authorization: `Bearer ${key}`,
  };
  if (provider === "openrouter") {
    headers["HTTP-Referer"] = env("APP_URL") || "https://hoc-toan-ai.onrender.com";
    headers["X-OpenRouter-Title"] = "Học toán với AI";
  }
  if (provider === "zai") {
    headers["Accept-Language"] = "vi-VN,vi";
  }
  return headers;
}

/**
 * Một lần HTTP. Không retry, không queue, không fallback nhà khác.
 * Local chỉ loopback. Nhà khóa: địa chỉ cứng / env https, khóa chính thức (env hoặc khóa lớp).
 */
export async function completeChat(opts: {
  provider: AiProviderId;
  model?: string | null;
  classApiKey?: string | null;
  classProvider?: string | null;
  messages: ChatMessage[];
  offlineText: string;
  fetchFn?: FetchLike;
  signal?: AbortSignal;
}): Promise<CompleteChatResult> {
  const provider = parseProvider(opts.provider);
  const fetchFn = opts.fetchFn || fetch;

  if (provider === "offline") {
    return {
      text: opts.offlineText,
      offline: true,
      provider: "offline",
      model: null,
      error: null,
      errorKind: null,
    };
  }

  let base: string;
  let key: string | null = null;

  if (laNhaKhoa(provider)) {
    base = docBaseNhaKhoa(provider);
    if (!laUrlCloudHopLe(base)) return thatBai(provider, "bad_cloud_url", base);
    key = docKhoaNha(provider, opts.classApiKey, opts.classProvider);
    if (!key) return thatBai(provider, "no_key");
  } else {
    base = docBaseLocal(provider);
    if (!laDiaChiLoopback(base)) return thatBai(provider, "not_loopback", base);
    key = bearerLocal(provider);
  }

  const model = docModel(provider, opts.model);
  const body: Record<string, unknown> = {
    model,
    temperature: 0.2,
    max_tokens: AI_MAX_TOKENS_CHAT,
    stream: false,
    messages: opts.messages,
  };
  if (provider === "zai") {
    // Tắt thinking: FlashX hay hết token cho reasoning, content rỗng. Lớp không xem CoT.
    body.thinking = { type: "disabled" };
  }
  const started = await motLan(
    fetchFn,
    urlChat(base),
    {
      method: "POST",
      headers: headerChat(provider, key),
      body: JSON.stringify(body),
    },
    AI_TIMEOUT_CHAT_MS,
    opts.signal,
  );

  if (started.kind === "timeout") return thatBai(provider, "timeout", base);
  if (started.kind === "aborted") return thatBai(provider, "aborted");
  if (started.kind === "network" || !started.res) return thatBai(provider, "network", base);

  if (!started.res.ok) {
    return thatBai(provider, "http", String(started.res.status));
  }

  let data: { choices?: { message?: { content?: unknown } }[] };
  try {
    data = (await started.res.json()) as typeof data;
  } catch {
    return thatBai(provider, "empty");
  }
  const text = docNoiDung(data.choices?.[0]?.message || {});
  if (!text) return thatBai(provider, "empty");

  return {
    text,
    offline: false,
    provider,
    model,
    error: null,
    errorKind: null,
  };
}

export async function probeProvider(opts: {
  provider: AiProviderId;
  classApiKey?: string | null;
  classProvider?: string | null;
  fetchFn?: FetchLike;
}): Promise<ProbeResult> {
  const provider = parseProvider(opts.provider);
  if (provider === "offline") {
    return {
      ok: true,
      provider,
      message: `${NHA.offline.ten} luôn sẵn — không gọi mạng.`,
      models: [],
      base: null,
    };
  }

  const fetchFn = opts.fetchFn || fetch;
  let base: string;
  let key: string;

  if (laNhaKhoa(provider)) {
    base = docBaseNhaKhoa(provider);
    if (!laUrlCloudHopLe(base)) {
      return { ok: false, provider, message: thongBaoLoiNha("bad_cloud_url", provider, base), models: [], base };
    }
    const k = docKhoaNha(provider, opts.classApiKey, opts.classProvider);
    if (!k) {
      return { ok: false, provider, message: thongBaoLoiNha("no_key", provider), models: [], base };
    }
    key = k;
  } else {
    base = docBaseLocal(provider);
    if (!laDiaChiLoopback(base)) {
      return { ok: false, provider, message: thongBaoLoiNha("not_loopback", provider, base), models: [], base };
    }
    key = bearerLocal(provider);
  }

  const started = await motLan(
    fetchFn,
    urlModels(base),
    { method: "GET", headers: { authorization: `Bearer ${key}` } },
    AI_TIMEOUT_PROBE_MS,
  );

  if (started.kind === "timeout") {
    return {
      ok: false,
      provider,
      message: `${NHA[provider].ten} hết giờ khi GET /models. Không thử lại, không chuyển nhà.`,
      models: [],
      base,
    };
  }
  if (started.kind !== undefined || !started.res) {
    return {
      ok: false,
      provider,
      message: thongBaoLoiNha("network", provider, base),
      models: [],
      base,
    };
  }
  if (!started.res.ok) {
    return {
      ok: false,
      provider,
      message: thongBaoLoiNha("http", provider, String(started.res.status)),
      models: [],
      base,
    };
  }

  let names: string[] = [];
  try {
    const data = (await started.res.json()) as { data?: { id?: string }[] };
    names = (data.data || []).map((m) => m.id || "").filter(Boolean).slice(0, 24);
  } catch {
    names = [];
  }

  return {
    ok: true,
    provider,
    message: names.length
      ? `${NHA[provider].ten} trả lời /models (${names.length} mô hình).`
      : `${NHA[provider].ten} trả lời /models.`,
    models: names,
    base,
  };
}
