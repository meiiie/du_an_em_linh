/** Nhãn và luật chọn nhà — dùng được cả phía trình duyệt. Không chứa khóa. */

export const AI_PROVIDER_IDS = ["offline", "cloud", "ollama", "lmstudio"] as const;
export type AiProviderId = (typeof AI_PROVIDER_IDS)[number];

export type ChatMessage = { role: "system" | "user" | "assistant"; content: string };

export const OLLAMA_MAC_DINH = "http://127.0.0.1:11434/v1";
export const LMSTUDIO_MAC_DINH = "http://127.0.0.1:1234/v1";
export const CLOUD_MAC_DINH = "https://api.openai.com/v1";

export const AI_TIMEOUT_CHAT_MS = 20_000;
export const AI_TIMEOUT_PROBE_MS = 8_000;

export const NHA: Record<
  AiProviderId,
  { id: AiProviderId; ten: string; ngan: string; local: boolean; canKhoa: boolean }
> = {
  offline: {
    id: "offline",
    ten: "Thang gợi ý đã kiểm",
    ngan: "Thang gợi ý",
    local: false,
    canKhoa: false,
  },
  cloud: {
    id: "cloud",
    ten: "API OpenAI tương thích (khóa chính thức)",
    ngan: "API khóa",
    local: false,
    canKhoa: true,
  },
  ollama: {
    id: "ollama",
    ten: "Ollama trên máy này",
    ngan: "Ollama",
    local: true,
    canKhoa: false,
  },
  lmstudio: {
    id: "lmstudio",
    ten: "LM Studio trên máy này",
    ngan: "LM Studio",
    local: true,
    canKhoa: false,
  },
};

export function parseProvider(raw: unknown): AiProviderId {
  const s = String(raw || "").trim().toLowerCase();
  if ((AI_PROVIDER_IDS as readonly string[]).includes(s)) return s as AiProviderId;
  return "offline";
}

export function laDiaChiLoopback(raw: string): boolean {
  let u: URL;
  try {
    u = new URL(raw);
  } catch {
    return false;
  }
  if (u.protocol !== "http:" && u.protocol !== "https:") return false;
  const host = u.hostname.replace(/^\[|\]$/g, "").toLowerCase();
  return host === "127.0.0.1" || host === "localhost" || host === "::1";
}

/** Cloud: https công cộng, hoặc http trên loopback (máy giả lập). Không http nội bộ. */
export function laUrlCloudHopLe(raw: string): boolean {
  let u: URL;
  try {
    u = new URL(raw);
  } catch {
    return false;
  }
  if (laDiaChiLoopback(raw)) return u.protocol === "http:" || u.protocol === "https:";
  return u.protocol === "https:";
}

export function chuanHoaBase(raw: string): string {
  return raw.trim().replace(/\/+$/, "");
}

export function urlChat(base: string): string {
  return `${chuanHoaBase(base)}/chat/completions`;
}

export function urlModels(base: string): string {
  return `${chuanHoaBase(base)}/models`;
}

export function maskKey(key: string | null | undefined): string | null {
  if (!key) return null;
  if (key.length < 8) return "••••";
  return `••••${key.slice(-4)}`;
}

export type AiPublicConfig = {
  classProvider: AiProviderId;
  classModel: string | null;
  allowLocal: boolean;
  cloudReady: boolean;
};

export function resolveProvider(opts: {
  classProvider?: string | null;
  sessionProvider?: string | null;
  allowLocal?: boolean;
}): AiProviderId {
  const lop = parseProvider(opts.classProvider);
  const session = opts.sessionProvider ? parseProvider(opts.sessionProvider) : null;
  const allowLocal = opts.allowLocal !== false;
  if (!session) return lop;
  if (session === "offline") return "offline";
  if (session === "ollama" || session === "lmstudio") return allowLocal ? session : lop;
  if (session === "cloud") return lop === "cloud" ? "cloud" : lop;
  return lop;
}

export function luaChonNhaHocSinh(cfg: AiPublicConfig): { id: AiProviderId; ten: string; disabled?: boolean }[] {
  const rows: { id: AiProviderId; ten: string; disabled?: boolean }[] = [
    { id: "offline", ten: NHA.offline.ten },
  ];
  if (cfg.allowLocal) {
    rows.push({ id: "ollama", ten: `${NHA.ollama.ten} · 127.0.0.1:11434` });
    rows.push({ id: "lmstudio", ten: `${NHA.lmstudio.ten} · 127.0.0.1:1234` });
  }
  if (cfg.classProvider === "cloud") {
    rows.push({
      id: "cloud",
      ten: cfg.cloudReady ? NHA.cloud.ten : `${NHA.cloud.ten} — chưa có khóa`,
      disabled: !cfg.cloudReady,
    });
  }
  return rows;
}

export function thongBaoLoiNha(kind: AiLoiKind, nha: AiProviderId, chiTiet?: string): string {
  const ten = NHA[nha].ten;
  switch (kind) {
    case "no_key":
      return `Chưa có khóa API chính thức (LLM_API_KEY hoặc khóa lớp). Không chuyển sang Ollama, LM Studio hay thang gợi ý tự động. Giáo viên đặt nhà «${NHA.offline.ten}» nếu lớp muốn chạy không API.`;
    case "not_loopback":
      return `${ten}: địa chỉ không phải loopback (127.0.0.1 / localhost). Harness từ chối — không quét mạng lớp, không gọi đám mây.`;
    case "bad_cloud_url":
      return `${ten}: LLM_BASE_URL không hợp lệ (cần https, hoặc http trên loopback). Không gọi.`;
    case "timeout":
      return `${ten} hết giờ ${AI_TIMEOUT_CHAT_MS / 1000} giây${chiTiet ? ` tại ${chiTiet}` : ""}. Không gửi lại câu hỏi, không chuyển nhà khác.`;
    case "http":
      return `${ten} trả ${chiTiet || "lỗi HTTP"}. Không gửi lại, không chuyển đám mây.`;
    case "network":
      return `Không nối được ${ten}${chiTiet ? ` tại ${chiTiet}` : ""}. Không chuyển đám mây, không xếp hàng, không gửi lại.`;
    case "empty":
      return `${ten} trả lời trống. Không chuyển nhà khác.`;
    case "aborted":
      return `Đã dừng gọi ${ten}. Không gửi lại.`;
  }
}

export type AiLoiKind =
  | "no_key"
  | "not_loopback"
  | "bad_cloud_url"
  | "timeout"
  | "http"
  | "network"
  | "empty"
  | "aborted";
