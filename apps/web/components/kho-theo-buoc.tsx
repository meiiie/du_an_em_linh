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
      {khung.map((b) => {
        const ten = [...b.congThuc.map((c) => c.ten), ...b.taiLieu.map((d) => d.ten)];
        return (
          <li key={b.ma} className="py-3" data-testid={`kho-buoc-${b.ma}`}>
            <p className="text-sm font-medium">{b.ten}</p>
            <p className="mt-1 text-sm text-muted">
              {ten.length ? ten.join(", ") : "Chưa có đoạn cho bước này."}
            </p>
          </li>
        );
      })}
    </ol>
  );
}
