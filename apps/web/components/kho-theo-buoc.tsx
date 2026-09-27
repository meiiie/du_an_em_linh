import type { TrichDanKho } from "@/lib/kien-thuc";

export type HangKhoBuoc = {
  ma: string;
  ten: string;
  congThuc: TrichDanKho[];
  taiLieu: TrichDanKho[];
};

export function KhoTheoBuoc({ khung }: { khung: HangKhoBuoc[] }) {
  return (
    <ol className="divide-y divide-line" data-testid="kho-theo-buoc">
      {khung.map((b, i) => (
        <li key={b.ma} className="flex min-h-11 items-center gap-3" data-testid={`kho-buoc-${b.ma}`}>
          <span className="tabular w-6 font-mono text-xs text-muted">{i + 1}</span>
          <p className="text-sm">{b.ten}</p>
        </li>
      ))}
    </ol>
  );
}
