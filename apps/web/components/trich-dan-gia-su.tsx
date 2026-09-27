import Link from "next/link";
import { tenTaiLieuNgan, thanTrich } from "@/lib/de-hoc-sinh";
import { duongKhoTrichDan, type TrichDanHien } from "@/lib/kien-thuc";

/** Chip tên + một đoạn — An kiểm được, bấm về đúng mục. */
export function TrichDanGiaSu({
  items,
  testId,
}: {
  items: TrichDanHien[];
  testId?: string;
}) {
  if (!items.length) return null;
  const trich = items.find((t) => t.dung && t.trich) || items.find((t) => t.trich);
  return (
    <div className="mt-2 border-t border-line pt-2 text-xs" data-testid={testId}>
      <p className="text-muted">Đã đọc</p>
      <ul className="mt-1 flex flex-wrap gap-x-3 gap-y-1">
        {items.map((t) => (
          <li key={`${t.loai}-${t.id || t.ten}`}>
            <Link href={duongKhoTrichDan(t)} className="underline underline-offset-2">
              {t.so != null ? `[${t.so}] ` : null}
              {t.loai === "tai_lieu" ? "Tài liệu · " : null}
              {tenTaiLieuNgan(t.ten)}
            </Link>
          </li>
        ))}
      </ul>
      {trich?.trich ? <p className="mt-1 leading-relaxed text-muted">«{thanTrich(trich.trich)}»</p> : null}
    </div>
  );
}
