"use client";

import { useState } from "react";
import { kiemTraNhaCungCap } from "@/lib/actions/gv";
import { NHA, parseProvider, type AiProviderId } from "@/lib/ai-catalog";
import { Button } from "./ui/button";
import { fieldControl } from "./ui/field";

export function KiemTraAi({ macDinh }: { macDinh: AiProviderId }) {
  const [provider, setProvider] = useState<AiProviderId>(macDinh);
  const [busy, setBusy] = useState(false);
  const [ket, setKet] = useState<{ ok: boolean; message: string; models: string[] } | null>(null);

  async function chay() {
    setBusy(true);
    setKet(null);
    const r = await kiemTraNhaCungCap(provider);
    setKet({ ok: r.ok, message: r.message, models: r.models });
    setBusy(false);
  }

  return (
    <div className="space-y-3 border-t border-line pt-6" data-testid="ai-probe">
      <p className="text-sm font-medium">Thử kết nối</p>
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
        <label className="block flex-1 text-sm">
          <span className="mb-2 block font-medium">Nhà cần thử</span>
          <select
            data-testid="ai-probe-provider"
            value={provider}
            onChange={(e) => setProvider(parseProvider(e.target.value))}
            className={fieldControl}
          >
            {(Object.keys(NHA) as AiProviderId[]).map((id) => (
              <option key={id} value={id}>
                {NHA[id].ten}
              </option>
            ))}
          </select>
        </label>
        <Button type="button" data-testid="ai-probe-chay" disabled={busy} onClick={chay}>
          {busy ? "Đang thử…" : "Thử kết nối"}
        </Button>
      </div>
      {ket ? (
        <p data-testid="ai-probe-ket" className={`px-4 py-3 text-sm ${ket.ok ? "bg-pass/10 text-pass" : "bg-amber-50 text-amber-950"}`}>
          {ket.message}
          {ket.models.length ? ` ${ket.models.slice(0, 6).join(", ")}` : ""}
        </p>
      ) : null}
    </div>
  );
}
