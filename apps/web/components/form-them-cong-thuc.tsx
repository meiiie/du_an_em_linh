"use client";

import { useState } from "react";
import { themCongThuc } from "@/lib/actions/gv";
import { Button } from "@/components/ui/button";
import { Field, fieldControl } from "@/components/ui/field";
import { Tex } from "@/components/tex";

export function FormThemCongThuc() {
  const [latex, setLatex] = useState("");
  return (
    <form action={themCongThuc} className="space-y-4">
      <Field label="Tên">
        <input name="title" required placeholder="Đạo hàm lũy thừa…" className={fieldControl} />
      </Field>
      <Field label="Công thức">
        <input
          name="latex"
          value={latex}
          onChange={(e) => setLatex(e.target.value)}
          placeholder="(x^n)' = n x^{n-1}"
          className={fieldControl}
          autoComplete="off"
          spellCheck={false}
        />
      </Field>
      {latex.trim() ? (
        <div className="overflow-x-auto border-y border-line bg-wash px-3 py-2" translate="no" aria-live="polite">
          <Tex tex={latex} block />
        </div>
      ) : null}
      <Field label="Giải thích">
        <textarea name="noi_dung" required rows={3} placeholder="Hàm đồng biến khi…" className={fieldControl} />
      </Field>
      <Button type="submit">Thêm</Button>
    </form>
  );
}
