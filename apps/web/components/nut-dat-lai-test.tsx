"use client";

import { useState } from "react";
import { RotateCcw } from "lucide-react";

/**
 * Nút đặt lại dữ liệu thử — CHỈ hiện khi chế độ test (APP_ENV=test, không Render; layout quyết định có render hay không).
 * Học sinh: đặt lại bài làm / gia sư / cảnh báo của chính mình. Giáo viên: đặt lại mọi học sinh demo.
 * Gọi POST /api/test/reset (route tự trả 404 ngoài chế độ test).
 */
export function NutDatLaiTest({ email }: { email: string | null }) {
  const [dang, setDang] = useState(false);
  const [loi, setLoi] = useState<string | null>(null);
  async function datLai() {
    const hoi = email
      ? "Đặt lại toàn bộ bài làm, lịch sử gia sư và cảnh báo của em về như lúc mới nạp dữ liệu?"
      : "Đặt lại bài làm, lịch sử gia sư và cảnh báo của MỌI học sinh demo?";
    if (!window.confirm(hoi)) return;
    setDang(true);
    setLoi(null);
    try {
      const r = await fetch("/api/test/reset", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(email ? { email } : {}),
      });
      if (!r.ok) throw new Error(String(r.status));
      // Nháp bài làm / câu hỏi gia sư lưu ở trình duyệt cũng xoá để màn hình về như lúc mới nạp dữ liệu.
      try {
        for (const k of Object.keys(localStorage)) if (k.startsWith("nhap:")) localStorage.removeItem(k);
        for (const k of Object.keys(sessionStorage)) if (k.startsWith("gs-")) sessionStorage.removeItem(k);
      } catch {
        /* bộ nhớ trình duyệt có thể bị chặn */
      }
      window.location.reload();
    } catch (e) {
      setLoi(`Không đặt lại được (${e instanceof Error ? e.message : "lỗi"}).`);
      setDang(false);
    }
  }
  return (
    <div className="mt-3 rounded-button border border-dashed border-white/25 p-2" data-testid="khoi-dat-lai-test">
      <p className="text-[11px] uppercase tracking-wide text-chalk/50">Chỉ môi trường test</p>
      <button
        type="button"
        data-testid="dat-lai-test"
        onClick={datLai}
        disabled={dang}
        className="mt-1 inline-flex min-h-11 items-center gap-2 text-sm text-chalk/80 hover:text-chalk disabled:opacity-60"
      >
        <RotateCcw className="h-4 w-4" aria-hidden />
        {dang ? "Đang đặt lại…" : email ? "Đặt lại dữ liệu của em" : "Đặt lại dữ liệu học sinh"}
      </button>
      {loi ? (
        <p className="mt-1 text-xs text-chalk/70" role="alert">
          {loi}
        </p>
      ) : null}
    </div>
  );
}
