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
      {khung.map((b, i) => {
        const ct = [...new Set(b.congThuc.map((c) => c.ten))].slice(0, 3);
        return (
          <li key={b.ma} className="flex min-h-11 items-start gap-3 py-3" data-testid={`kho-buoc-${b.ma}`}>
            <span className="grid size-8 shrink-0 place-items-center rounded-button text-xs font-medium text-muted ring-1 ring-line">
              {i + 1}
            </span>
            <div className="min-w-0 pt-1">
              <p className="text-sm font-medium">{b.ten}</p>
              <p className="mt-1 text-xs leading-5 text-muted">{ct.length ? ct.join(" · ") : "Chưa có công thức."}</p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
