"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { guiThayCo, hoiGiaSu } from "@/lib/actions/hs";
import { luaChonNhaHocSinh, parseProvider, type AiProviderId, type AiPublicConfig } from "@/lib/ai-catalog";
import { chuTrangThaiGiaSu, gomSse, type GiaSuBuocSse } from "@/lib/sse";
import { moTaCheDo } from "@/lib/tutor";
import { Button, buttonClasses } from "./ui/button";
import { fieldControl } from "./ui/field";
import { cn } from "@/lib/cn";

type Msg = { role: "hs" | "gia_su"; text: string; error?: boolean; trichDan?: { loai: string; ten: string }[] };

type KetHoi = {
  ok: boolean;
  tra_loi: string;
  offline?: boolean;
  provider?: string;
  error?: string | null;
  trich_dan?: { loai: string; ten: string }[];
};

const LOI_CHAO: Msg = {
  role: "gia_su",
  text: "Mình là gia sư AI. Mình đọc công thức và tài liệu lớp, sửa bài và giảng, không đưa đáp án.",
};

function khoaDraft(problemId: string) {
  return `gs-draft:${problemId}`;
}
function khoaNha(problemId: string) {
  return `gs-provider:${problemId}`;
}

function fitTextarea(el: HTMLTextAreaElement) {
  el.style.height = "0px";
  el.style.height = `${Math.min(Math.max(el.scrollHeight, 44), 160)}px`;
}

async function docSseHoi(opts: {
  problemId: string;
  text: string;
  provider: string;
  model?: string;
  signal: AbortSignal;
  onBuoc: (b: GiaSuBuocSse) => void;
}): Promise<KetHoi> {
  const res = await fetch("/api/hs/gia-su", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      problemId: opts.problemId,
      text: opts.text,
      provider: opts.provider,
      model: opts.model,
    }),
    signal: opts.signal,
  });
  if (!res.ok || !res.body) {
    return hoiGiaSu(opts.problemId, opts.text, { provider: opts.provider, model: opts.model });
  }
  const reader = res.body.getReader();
  const dec = new TextDecoder();
  let leftover = "";
  let ket: KetHoi | null = null;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    leftover += dec.decode(value, { stream: true });
    const gom = gomSse(leftover);
    leftover = gom.leftover;
    for (const ev of gom.events) {
      if (ev.event === "trang_thai") {
        const buoc = (JSON.parse(ev.data) as { buoc?: GiaSuBuocSse }).buoc;
        if (buoc) opts.onBuoc(buoc);
      } else if (ev.event === "xong" || ev.event === "loi") {
        ket = JSON.parse(ev.data) as KetHoi;
      }
    }
  }
  return ket || { ok: false, tra_loi: "Gia sư đang bận. Em cứ sửa bước được tô và nộp lại.", offline: true };
}

export function TutorPanel({
  problemId,
  initialChat,
  ai,
  open,
  onClose,
}: {
  problemId: string;
  initialChat: { role: "hs" | "gia_su"; text: string }[];
  ai: AiPublicConfig;
  open: boolean;
  onClose: () => void;
}) {
  const [chat, setChat] = useState<Msg[]>(initialChat.length ? initialChat : [LOI_CHAO]);
  const [ask, setAsk] = useState("");
  const [provider, setProvider] = useState<AiProviderId>(ai.classProvider);
  const [thinking, setThinking] = useState(false);
  const [buocSse, setBuocSse] = useState<GiaSuBuocSse | null>(null);
  const [lastOffline, setLastOffline] = useState(ai.classProvider === "offline");
  const [lastError, setLastError] = useState<string | null>(null);
  const seq = useRef(0);
  const box = useRef<HTMLTextAreaElement>(null);
  const log = useRef<HTMLDivElement>(null);
  const huy = useRef<AbortController | null>(null);
  const choices = luaChonNhaHocSinh(ai);

  useEffect(() => {
    try {
      const draft = sessionStorage.getItem(khoaDraft(problemId));
      if (draft) setAsk(draft);
      const saved = sessionStorage.getItem(khoaNha(problemId));
      if (saved) setProvider(parseProvider(saved));
    } catch {
      /* sessionStorage có thể bị chặn */
    }
  }, [problemId]);

  useEffect(() => {
    if (box.current) fitTextarea(box.current);
  }, [ask]);

  useEffect(() => {
    log.current?.scrollTo({ top: log.current.scrollHeight });
  }, [chat, thinking, buocSse]);

  function ghiDraft(v: string) {
    setAsk(v);
    try {
      if (v) sessionStorage.setItem(khoaDraft(problemId), v);
      else sessionStorage.removeItem(khoaDraft(problemId));
    } catch {
      /* bỏ */
    }
  }

  function doiNha(v: AiProviderId) {
    setProvider(v);
    try {
      sessionStorage.setItem(khoaNha(problemId), v);
    } catch {
      /* bỏ */
    }
  }

  function dung() {
    seq.current += 1;
    huy.current?.abort();
    huy.current = null;
    setThinking(false);
    setBuocSse(null);
    setChat((c) => [...c, { role: "gia_su", text: "Đã dừng. Không gửi lại câu hỏi.", error: true }]);
    setLastError("aborted");
  }

  function nhanKet(res: KetHoi) {
    if (res.ok) {
      setLastOffline(Boolean(res.offline));
      setLastError(res.error ?? null);
      setChat((c) => [
        ...c,
        { role: "gia_su", text: res.tra_loi, error: Boolean(res.error), trichDan: res.trich_dan || [] },
      ]);
    } else {
      setLastError(res.tra_loi);
      setChat((c) => [...c, { role: "gia_su", text: res.tra_loi, error: true }]);
    }
  }

  async function sendChat(raw?: string) {
    const text = (raw ?? ask).trim();
    if (!text || thinking) return;
    const my = ++seq.current;
    if (!raw) ghiDraft("");
    setChat((c) => [...c, { role: "hs", text }]);
    setThinking(true);
    setBuocSse(null);
    setLastError(null);
    const ac = new AbortController();
    huy.current = ac;
    try {
      const res = await docSseHoi({
        problemId,
        text,
        provider,
        model: ai.classModel || undefined,
        signal: ac.signal,
        onBuoc: (b) => {
          if (my === seq.current) setBuocSse(b);
        },
      });
      if (my !== seq.current) return;
      nhanKet(res);
    } catch (e) {
      if (my !== seq.current) return;
      if (e instanceof DOMException && e.name === "AbortError") return;
      setChat((c) => [...c, { role: "gia_su", text: "Không nối được gia sư. Không gửi lại.", error: true }]);
    } finally {
      if (my === seq.current) {
        setThinking(false);
        setBuocSse(null);
        huy.current = null;
      }
    }
  }

  async function nhoThayCo() {
    if (thinking) return;
    const my = ++seq.current;
    setChat((c) => [...c, { role: "hs", text: "Gửi thầy cô giúp em" }]);
    setThinking(true);
    const res = await guiThayCo(problemId);
    if (my !== seq.current) return;
    setThinking(false);
    setChat((c) => [...c, { role: "gia_su", text: res.tra_loi }]);
  }

  return (
    <aside
      className={cn(
        "flex min-h-0 flex-col border-t border-line pt-4 md:sticky md:top-4 md:h-[calc(100dvh-6.5rem)] md:border-l md:border-t-0 md:pl-6 md:pt-0",
        open ? "flex" : "hidden md:flex",
      )}
      data-testid="tutor-panel"
      aria-busy={thinking}
    >
      <div className="flex shrink-0 items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold">Gia sư AI</p>
          <p className="text-xs text-muted" data-testid="tutor-che-do">
            {moTaCheDo({ provider, offline: lastOffline, error: lastError })}. Không phải giáo viên. Không đọc lời giải
            chuẩn.
          </p>
          <p className="mt-1 text-xs">
            <Link href="/hs/kho" className="underline underline-offset-2" data-testid="tutor-toi-kho">
              Công thức và tài liệu lớp
            </Link>
          </p>
        </div>
        <button type="button" className={cn(buttonClasses({ variant: "ghost", size: "sm" }), "md:hidden")} onClick={onClose}>
          Đóng
        </button>
      </div>

      <label className="mt-3 block shrink-0 text-sm">
        <span className="mb-2 block font-medium">Gia sư lần này</span>
        <select
          data-testid="tutor-provider"
          value={provider}
          onChange={(e) => doiNha(parseProvider(e.target.value))}
          className={fieldControl}
          disabled={thinking}
        >
          {choices.map((c) => (
            <option key={c.id} value={c.id} disabled={c.disabled}>
              {c.ten}
            </option>
          ))}
        </select>
      </label>

      <div className="mt-3 flex shrink-0 flex-wrap gap-2">
        <Button type="button" variant="secondary" size="sm" data-testid="chip-goi-y" disabled={thinking} onClick={() => sendChat("Gợi ý bước này")}>
          Gợi ý bước này
        </Button>
        <Button type="button" variant="secondary" size="sm" data-testid="chip-sai-cho" disabled={thinking} onClick={() => sendChat("Em sai chỗ nào?")}>
          Em sai chỗ nào?
        </Button>
        <Button type="button" variant="ghost" size="sm" data-testid="chip-gui-gv" disabled={thinking} onClick={nhoThayCo}>
          Gửi thầy cô
        </Button>
      </div>

      <div ref={log} data-testid="tutor-log" className="mt-4 min-h-40 flex-1 space-y-2 overflow-y-auto overscroll-contain md:min-h-0">
        {chat.map((m, i) => (
          <div
            key={i}
            className={cn(
              "max-w-[92%] px-4 py-3 text-sm",
              m.role === "hs" ? "ml-auto bg-ink text-chalk" : m.error ? "bg-amber-50 text-amber-950" : "bg-wash",
            )}
          >
            <p>{m.text}</p>
            {m.trichDan && m.trichDan.length ? (
              <p className="mt-2 text-xs text-muted" data-testid={i === chat.length - 1 ? "tutor-trich-dan" : undefined}>
                Đã đọc: {m.trichDan.map((t) => t.ten).join(" · ")}
              </p>
            ) : null}
          </div>
        ))}
        {thinking ? (
          <p className="bg-wash px-4 py-3 text-sm text-muted" data-testid="tutor-thinking" aria-live="polite">
            {chuTrangThaiGiaSu(buocSse)}
          </p>
        ) : null}
      </div>

      <div className="mt-4 flex shrink-0 items-end gap-2 border-t border-line pt-3" data-testid="tutor-composer">
        <label className="sr-only" htmlFor="tutor-input">
          Câu hỏi cho gia sư
        </label>
        <textarea
          id="tutor-input"
          ref={box}
          data-testid="tutor-input"
          rows={1}
          value={ask}
          disabled={thinking}
          onChange={(e) => ghiDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.nativeEvent.isComposing || e.repeat) return;
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              void sendChat();
            }
          }}
          className={`min-h-11 min-w-0 flex-1 resize-none ${fieldControl}`}
          placeholder="Hỏi gợi ý, không hỏi đáp án… Enter gửi, Shift+Enter xuống dòng"
        />
        {thinking ? (
          <Button type="button" data-testid="tutor-send" variant="secondary" onClick={dung} className="min-h-11 min-w-11 px-4">
            Dừng
          </Button>
        ) : (
          <Button
            type="button"
            data-testid="tutor-send"
            onClick={() => sendChat()}
            disabled={!ask.trim()}
            className="min-h-11 min-w-11 px-4"
          >
            Gửi
          </Button>
        )}
      </div>
    </aside>
  );
}
