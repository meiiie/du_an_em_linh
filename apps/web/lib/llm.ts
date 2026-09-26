import { db } from "./db";
import { llmCalls } from "./db/schema";

const NAME_RE = /\b(an|bình|binh|chi|giáo viên|giao vien)\b/gi;

export function redact(text: string) {
  return text
    .replace(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi, "[email]")
    .replace(/\b0\d{8,10}\b/g, "[sdt]")
    .replace(NAME_RE, "[ten]");
}

type ChatMessage = { role: "system" | "user" | "assistant"; content: string };

export async function callLLM(opts: {
  purpose: string;
  pseudonymId: string;
  messages: ChatMessage[];
  offlineText: string;
}) {
  const cleaned = opts.messages.map((m) => ({ ...m, content: redact(m.content) }));
  const key = process.env.LLM_API_KEY;
  const base = process.env.LLM_BASE_URL || "https://api.openai.com/v1";
  const model = process.env.LLM_MODEL || "gpt-4o-mini";
  if (!key) {
    await db.insert(llmCalls).values({
      id: crypto.randomUUID(),
      purpose: opts.purpose,
      model: null,
      provider: "offline",
      pseudonymId: opts.pseudonymId,
      offline: true,
    });
    return { text: opts.offlineText, offline: true, provider: "offline" };
  }
  try {
    const res = await fetch(base.replace(/\/$/, "") + "/chat/completions", {
      method: "POST",
      headers: { "content-type": "application/json", authorization: `Bearer ${key}` },
      body: JSON.stringify({ model, temperature: 0.2, messages: cleaned }),
    });
    if (!res.ok) throw new Error(String(res.status));
    const data = (await res.json()) as { choices?: { message?: { content?: string } }[] };
    const text = data.choices?.[0]?.message?.content?.trim() || opts.offlineText;
    await db.insert(llmCalls).values({
      id: crypto.randomUUID(),
      purpose: opts.purpose,
      model,
      provider: "openai-compatible",
      pseudonymId: opts.pseudonymId,
      offline: false,
    });
    return { text, offline: false, provider: "openai-compatible" };
  } catch {
    await db.insert(llmCalls).values({
      id: crypto.randomUUID(),
      purpose: opts.purpose,
      model,
      provider: "offline-fallback",
      pseudonymId: opts.pseudonymId,
      offline: true,
    });
    return { text: opts.offlineText, offline: true, provider: "offline-fallback" };
  }
}
