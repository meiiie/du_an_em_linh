import { db } from "./db";
import { llmCalls } from "./db/schema";
import { completeChat, parseProvider, type AiProviderId, type ChatMessage } from "./ai-harness";

/** Tên demo, không chữ nghề «giáo viên» — chữ đó nằm trong prompt hệ thống. */
const NAME_RE = /\b(an|bình|binh|chi)\b/gi;

export function redact(text: string) {
  return text
    .replace(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi, "[email]")
    .replace(/\b0\d{8,10}\b/g, "[sdt]")
    .replace(NAME_RE, "[ten]");
}

/** Prompt hệ thống giữ nguyên — chữ «giáo viên» là nghề, không phải tên học sinh. */
export function locTinNhan(messages: ChatMessage[]): ChatMessage[] {
  return messages.map((m) => (m.role === "system" ? m : { ...m, content: redact(m.content) }));
}

export type { ChatMessage };

export async function callLLM(opts: {
  purpose: string;
  pseudonymId: string;
  messages: ChatMessage[];
  offlineText: string;
  provider?: string | null;
  model?: string | null;
  classApiKey?: string | null;
  classProvider?: string | null;
  signal?: AbortSignal;
}) {
  const cleaned = locTinNhan(opts.messages);
  const provider = parseProvider(opts.provider);
  const result = await completeChat({
    provider,
    model: opts.model,
    classApiKey: opts.classApiKey,
    classProvider: opts.classProvider,
    messages: cleaned,
    offlineText: opts.offlineText,
    signal: opts.signal,
  });
  await db.insert(llmCalls).values({
    id: crypto.randomUUID(),
    purpose: opts.purpose,
    model: result.model,
    provider: result.provider,
    pseudonymId: opts.pseudonymId,
    offline: result.offline,
  });
  return result;
}

export function nhaMacDinh(): AiProviderId {
  return "offline";
}
