"use client";

import { useEffect, useRef } from "react";

export function MathInput({
  value,
  onChange,
  testId,
  label,
}: {
  value: string;
  onChange: (v: string) => void;
  testId: string;
  label: string;
}) {
  const ref = useRef<HTMLElement | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    let dead = false;
    const el = ref.current as (HTMLElement & { value?: string; mathVirtualKeyboardPolicy?: string }) | null;
    import("mathlive")
      .then(() => {
        if (dead || !el) return;
        el.mathVirtualKeyboardPolicy = "manual";
        el.value = value;
        const handler = () => onChangeRef.current(el.value || "");
        el.addEventListener("input", handler);
      })
      .catch(() => undefined);
    return () => {
      dead = true;
    };
    // Chỉ gắn một lần; giá trị sau đó đi qua ô LaTeX.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const el = ref.current as (HTMLElement & { value?: string }) | null;
    if (el && el.value !== value) el.value = value;
  }, [value]);

  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium">{label}</span>
      <math-field ref={ref as never} data-testid={`mf-${testId}`} />
      <input
        data-testid={testId}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="mt-1 w-full rounded-button border border-dashed border-line bg-paper px-2 py-1 font-mono text-xs"
        placeholder="LaTeX"
        aria-label={`${label} dạng LaTeX`}
      />
    </label>
  );
}
