"use client";

import { useEffect, useRef, useState } from "react";
import { Tex } from "./tex";

type MathFieldEl = HTMLElement & {
  value?: string;
  getValue?: (fmt?: string) => string;
  setValue?: (v: string, opts?: Record<string, unknown>) => void;
  mathVirtualKeyboardPolicy?: string;
  menuItems?: unknown[];
};

/**
 * Ô nhập công thức: MathLive là đường chính, ô gõ thường là đường dự phòng luôn dùng được.
 * Sửa lỗi 29/09 (UX giai đoạn 2):
 * - Gán giá trị cho <math-field> TRƯỚC khi phần tử được định nghĩa tạo ra thuộc tính riêng che getter của MathLive,
 *   nên `value` luôn rỗng và bài nộp bị coi là trống. Giờ chờ `customElements.whenDefined`, xoá thuộc tính riêng, đọc
 *   bằng `getValue("latex")`.
 * - Thiết bị chạm: bàn phím ảo tự hiện (`auto`), nút bàn phím không bị ẩn.
 */
export function MathInput({
  value,
  onChange,
  testId,
  label,
  nhe = false,
}: {
  value: string;
  onChange: (v: string) => void;
  testId: string;
  label: string;
  /** Nhãn là ký hiệu toán, không phải tiêu đề form. */
  nhe?: boolean;
}) {
  const ref = useRef<MathFieldEl | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const [sanSang, setSanSang] = useState(false);

  useEffect(() => {
    let dead = false;
    let el: MathFieldEl | null = null;
    const handler = () => {
      if (!el) return;
      const v = typeof el.getValue === "function" ? el.getValue("latex") : el.value || "";
      onChangeRef.current(v || "");
    };
    import("mathlive")
      .then(() => customElements.whenDefined("math-field"))
      .then(() => {
        el = ref.current;
        if (dead || !el) return;
        // Xoá thuộc tính riêng (nếu React/ai đó gán trước khi nâng cấp) để getter/setter của MathLive hoạt động
        for (const k of ["value", "mathVirtualKeyboardPolicy", "menuItems"] as const) {
          if (Object.prototype.hasOwnProperty.call(el, k)) delete (el as unknown as Record<string, unknown>)[k];
        }
        const cham = window.matchMedia?.("(pointer: coarse)").matches;
        el.mathVirtualKeyboardPolicy = cham ? "auto" : "manual";
        el.menuItems = [];
        if (typeof el.setValue === "function") el.setValue(value, { silenceNotifications: true });
        else el.value = value;
        el.addEventListener("input", handler);
        el.addEventListener("change", handler);
        setSanSang(true);
      })
      .catch(() => undefined);
    return () => {
      dead = true;
      if (el) {
        el.removeEventListener("input", handler);
        el.removeEventListener("change", handler);
      }
    };
    // Chỉ gắn một lần; giá trị sau đó đồng bộ ở effect dưới.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const el = ref.current;
    if (!el || !sanSang) return;
    const cur = typeof el.getValue === "function" ? el.getValue("latex") : el.value;
    if (cur !== value) {
      if (typeof el.setValue === "function") el.setValue(value, { silenceNotifications: true });
      else el.value = value;
    }
  }, [value, sanSang]);

  return (
    <label className="block text-sm">
      <span className={nhe ? "mb-2 block font-mono text-sm text-muted" : "mb-2 block font-medium"}>{label}</span>
      <math-field ref={ref as never} data-testid={`mf-${testId}`} />
      <input
        data-testid={testId}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-2 w-full min-h-11 rounded-button border border-line bg-canvas px-3 py-2 font-mono text-sm"
        placeholder="hoặc gõ thường, ví dụ 3x^2-12x+9"
        inputMode="text"
        autoCapitalize="off"
        autoCorrect="off"
        spellCheck={false}
        aria-label={`${label} (gõ thường)`}
      />
      {value.trim() ? (
        <span className="mt-1 block text-xs text-muted" data-testid={`hieu-${testId}`}>
          Máy hiểu là: <Tex tex={value} />
        </span>
      ) : null}
    </label>
  );
}
