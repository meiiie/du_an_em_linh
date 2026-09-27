"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { guiThayCo } from "@/lib/actions/hs";
import { luaChonNhaHocSinh, parseProvider, type AiProviderId, type AiPublicConfig } from "@/lib/ai-catalog";
import type { TrichDanHien } from "@/lib/kien-thuc";
import { chuTrangThaiGiaSu, docJsonSse, gomSse, type GiaSuBuocSse } from "@/lib/sse";
import { moTaCheDo } from "@/lib/tutor";
import { LoiGiaSu } from "./loi-gia-su";
import { TrichDanGiaSu } from "./trich-dan-gia-su";
import { Button, buttonClasses } from "./ui/button";
import { fieldControl } from "./ui/field";
import { cn } from "@/lib/cn";

type Msg = { role: "hs" | "gia_su"; text: string; error?: boolean; trichDan?: TrichDanHien[] };

type KetHoi = {
  ok: boolean;
  tra_loi: string;
  offline?: boolean;
  provider?: string;
  error?: string | null;
  trich_dan?: TrichDanHien[];
};

const LOI_CHAO: Msg = {
  role: "gia_su",
  text: "Mình là gia sư AI. Mình đọc công thức và tài liệu lớp, sửa bài và giảng, không đưa đáp án.",
};

const LOI_KHONG_NOI: KetHoi = { ok: false, tra_loi: "Không nối được gia sư. Không gửi lại.", offline: true };

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

function dangGanDay(el: HTMLDivElement) {
  return el.scrollHeight - el.scrollTop - el.clientHeight < 48;
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
  if (res.status === 401) return { ok: false, tra_loi: "Chưa vào lớp.", offline: true };
  if (!res.ok) {
    try {
      const j = (await res.json()) as { tra_loi?: string };
      return { ok: false, tra_loi: j.tra_loi || LOI_KHONG_NOI.tra_loi, offline: true };
    } catch {
      return LOI_KHONG_NOI;
    }
  }
  if (!res.body) return LOI_KHONG_NOI;
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
        const buoc = docJsonSse<{ buoc?: GiaSuBuocSse }>(ev.data)?.buoc;
        if (buoc) opts.onBuoc(buoc);
      } else if (ev.event === "xong" || ev.event === "loi") {
        ket = docJsonSse<KetHoi>(ev.data);
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
  initialChat: { role: "hs" | "gia_su"; text: string; trichDan?: TrichDanHien[] }[];
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
  const [hienXuong, setHienXuong] = useState(false);
  const seq = useRef(0);
  const box = useRef<HTMLTextAreaElement>(null);
  const log = useRef<HTMLDivElement>(null);
  const huy = useRef<AbortController | null>(null);
  const ganDay = useRef(true);
  const choices = luaChonNhaHocSinh(ai);
  const cauCuoiHs = [...chat].reverse().find((m) => m.role === "hs")?.text;
  const loiCuoi = Boolean(chat[chat.length - 1]?.error);

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
    if (!ganDay.current) return;
    log.current?.scrollTo({ top: log.current.scrollHeight });
  }, [chat, thinking, buocSse]);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key !== "Escape" || !huy.current) return;
      e.preventDefault();
      dung();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

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
    queueMicrotask(() => box.current?.focus());
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
    ganDay.current = true;
    setHienXuong(false);
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
        queueMicrotask(() => box.current?.focus());
      }
    }
  }

  async function nhoThayCo() {
    if (thinking) return;
    const my = ++seq.current;
    ganDay.current = true;
    setChat((c) => [...c, { role: "hs", text: "Gửi thầy cô giúp em" }]);
    setThinking(true);
    const res = await guiThayCo(problemId);
    if (my !== seq.current) return;
    setThinking(false);
    setChat((c) => [...c, { role: "gia_su", text: res.tra_loi }]);
    queueMicrotask(() => box.current?.focus());
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
        {loiCuoi && cauCuoiHs && !thinking ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            data-testid="chip-hoi-lai"
            onClick={() => {
              ghiDraft(cauCuoiHs);
              box.current?.focus();
            }}
          >
            Hỏi lại
          </Button>
        ) : null}
      </div>

      <div
        ref={log}
        data-testid="tutor-log"
        onScroll={() => {
          const el = log.current;
          if (!el) return;
          ganDay.current = dangGanDay(el);
          setHienXuong(!ganDay.current);
        }}
        className="mt-4 min-h-40 flex-1 space-y-4 overflow-y-auto overscroll-contain [overflow-anchor:auto] md:min-h-0"
      >
        {chat.map((m, i) => (
          <div
            key={i}
            className={cn(
              "min-w-0 text-sm",
              m.role === "hs"
                ? "ml-auto max-w-[85%] bg-ink px-4 py-3 text-chalk"
                : m.error
                  ? "border-l-2 border-warn bg-amber-50 px-4 py-3 text-amber-950"
                  : "",
            )}
          >
            {m.role === "gia_su" && !m.error ? (
              <LoiGiaSu text={m.text} />
            ) : (
              <p className="whitespace-pre-wrap leading-relaxed">{m.text}</p>
            )}
            {m.trichDan && m.trichDan.length ? (
              <TrichDanGiaSu items={m.trichDan} testId={i === chat.length - 1 ? "tutor-trich-dan" : undefined} />
            ) : null}
          </div>
        ))}
        {thinking ? (
          <p className="text-sm text-muted" data-testid="tutor-thinking" aria-live="polite">
            {chuTrangThaiGiaSu(buocSse)}
          </p>
        ) : null}
      </div>

      {hienXuong ? (
        <button
          type="button"
          data-testid="tutor-xuong"
          className="mt-2 self-start text-xs underline underline-offset-2"
          onClick={() => {
            ganDay.current = true;
            setHienXuong(false);
            log.current?.scrollTo({ top: log.current.scrollHeight });
          }}
        >
          Xuống
        </button>
      ) : null}

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
          onChange={(e) => ghiDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.nativeEvent.isComposing || e.repeat) return;
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              if (!thinking) void sendChat();
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
