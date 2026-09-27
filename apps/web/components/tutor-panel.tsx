"use client";

import { useEffect, useRef, useState } from "react";
import { guiThayCo, hoiGiaSu } from "@/lib/actions/hs";
import { luaChonNhaHocSinh, parseProvider, type AiProviderId, type AiPublicConfig } from "@/lib/ai-catalog";
import { moTaCheDo } from "@/lib/tutor";
import { Button, buttonClasses } from "./ui/button";
import { fieldControl } from "./ui/field";
import { cn } from "@/lib/cn";

type Msg = { role: "hs" | "gia_su"; text: string; error?: boolean };

const LOI_CHAO: Msg = {
  role: "gia_su",
  text: "Mình là gia sư AI. Mình sửa bài và giảng cho em hiểu, không đưa đáp án trong lúc làm.",
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
  const [lastOffline, setLastOffline] = useState(ai.classProvider === "offline");
  const [lastError, setLastError] = useState<string | null>(null);
  const seq = useRef(0);
  const box = useRef<HTMLTextAreaElement>(null);
  const log = useRef<HTMLDivElement>(null);
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
  }, [chat, thinking]);

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
    setThinking(false);
    setChat((c) => [...c, { role: "gia_su", text: "Đã dừng. Không gửi lại câu hỏi.", error: true }]);
    setLastError("aborted");
  }

  async function sendChat(raw?: string) {
    const text = (raw ?? ask).trim();
    if (!text || thinking) return;
    const my = ++seq.current;
    if (!raw) ghiDraft("");
    setChat((c) => [...c, { role: "hs", text }]);
    setThinking(true);
    setLastError(null);
    const res = await hoiGiaSu(problemId, text, { provider, model: ai.classModel || undefined });
    if (my !== seq.current) return;
    setThinking(false);
    if (res.ok) {
      setLastOffline(res.offline);
      setLastError(res.error);
      setChat((c) => [...c, { role: "gia_su", text: res.tra_loi, error: Boolean(res.error) }]);
    } else {
      setLastError(res.tra_loi);
      setChat((c) => [...c, { role: "gia_su", text: res.tra_loi, error: true }]);
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
      className={`border-t border-line pt-4 md:border-t-0 md:pt-0 ${open ? "block" : "hidden md:block"}`}
      data-testid="tutor-panel"
      aria-busy={thinking}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold">Gia sư AI</p>
          <p className="text-xs text-muted" data-testid="tutor-che-do">
            {moTaCheDo({ provider, offline: lastOffline, error: lastError })}. Không phải giáo viên. Không đọc lời giải
            chuẩn.
          </p>
        </div>
        <button type="button" className={cn(buttonClasses({ variant: "ghost", size: "sm" }), "md:hidden")} onClick={onClose}>
          Đóng
        </button>
      </div>

      <label className="mt-3 block text-sm">
        <span className="mb-2 block font-medium">Nhà trên phiên này</span>
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

      <div className="mt-3 flex flex-wrap gap-2">
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

      <div ref={log} data-testid="tutor-log" className="mt-4 max-h-80 space-y-2 overflow-y-auto overscroll-contain">
        {chat.map((m, i) => (
          <p
            key={i}
            className={`px-4 py-3 text-sm ${
              m.role === "hs" ? "bg-ink text-chalk" : m.error ? "bg-amber-50 text-amber-950" : "bg-wash"
            }`}
          >
            {m.text}
          </p>
        ))}
        {thinking ? (
          <p className="px-4 py-3 text-sm text-muted" data-testid="tutor-thinking" aria-live="polite">
            Đang nghĩ… lọc đáp án sau khi có cả câu. Không phát luồng từng chữ.
          </p>
        ) : null}
      </div>

      <div className="mt-4 flex items-end gap-2" data-testid="tutor-composer">
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
