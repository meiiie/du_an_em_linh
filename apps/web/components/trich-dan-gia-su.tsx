import Link from "next/link";
import { tenTaiLieuNgan, thanTrich } from "@/lib/de-hoc-sinh";
import { duongKhoTrichDan, type TrichDanHien } from "@/lib/kien-thuc";

/** Đoạn Z.AI đã mở — An kiểm được, bấm về đúng mục kho. */
export function TrichDanGiaSu({
  items,
  testId,
}: {
  items: TrichDanHien[];
  testId?: string;
}) {
  if (!items.length) return null;
  return (
    <div className="mt-2 space-y-2 text-xs" data-testid={testId}>
      <p className="text-muted">Đã đọc</p>
      <ul className="space-y-2">
        {items.map((t) => (
          <li key={`${t.loai}-${t.id || t.ten}`}>
            <Link href={duongKhoTrichDan(t)} className="underline underline-offset-2">
              {tenTaiLieuNgan(t.ten)}
            </Link>
            {t.trich ? <p className="mt-1 leading-relaxed text-muted">«{thanTrich(t.trich)}»</p> : null}
          </li>
        ))}
      </ul>
    </div>
  );
}
