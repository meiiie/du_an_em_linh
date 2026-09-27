"use client";

import { useState } from "react";
import Link from "next/link";
import { tenTaiLieuNgan, thanTrich } from "@/lib/de-hoc-sinh";
import { duongKhoTrichDan, type TrichDanHien } from "@/lib/kien-thuc";
import { chonTrichHien, tenMoKho } from "@/lib/trich-dan-ui";
import { LoiGiaSu } from "./loi-gia-su";
import { cn } from "@/lib/cn";

/** Chip chọn đoạn + Mở về kho — An không rời phiếu khi bấm số. */
export function TrichDanGiaSu({
  items,
  testId,
  moSo,
  onChonSo,
}: {
  items: TrichDanHien[];
  testId?: string;
  moSo?: number | null;
  onChonSo?: (so: number) => void;
}) {
  if (!items.length) return null;
  const trich = chonTrichHien(items, moSo);
  return (
    <div className="mt-2 border-t border-line pt-2 text-xs" data-testid={testId}>
      <p className="text-muted">Đã đọc</p>
      <ul className="mt-1 flex flex-wrap gap-x-1 gap-y-1">
        {items.map((t) => {
          const so = t.so;
          const dang = trich && ((so != null && so === trich.so) || t.id === trich.id);
          return (
            <li key={`${t.loai}-${t.id || t.ten}`} id={so != null ? `td-${so}` : undefined} className="scroll-mt-4">
              {so != null && onChonSo ? (
                <button
                  type="button"
                  data-so={so}
                  aria-pressed={Boolean(dang)}
                  onClick={() => onChonSo(so)}
                  className={cn(
                    "min-h-11 px-2 text-left underline-offset-2",
                    dang ? "font-medium underline" : "text-muted",
                  )}
                >
                  [{so}] {t.loai === "tai_lieu" ? "Tài liệu · " : null}
                  {tenTaiLieuNgan(t.ten)}
                </button>
              ) : (
                <Link href={duongKhoTrichDan(t)} className="underline underline-offset-2">
                  {so != null ? `[${so}] ` : null}
                  {t.loai === "tai_lieu" ? "Tài liệu · " : null}
                  {tenTaiLieuNgan(t.ten)}
                </Link>
              )}
            </li>
          );
        })}
      </ul>
      {trich?.trich ? (
        <p className="mt-1 leading-relaxed text-muted">
          «{thanTrich(trich.trich)}»{" "}
          <Link
            href={duongKhoTrichDan(trich)}
            className="inline-flex min-h-11 items-center underline underline-offset-2"
          >
            {tenMoKho(trich.loai)}
          </Link>
        </p>
      ) : null}
    </div>
  );
}

export function LoiVaNguon({
  text,
  trichDan,
  testId,
}: {
  text: string;
  trichDan?: TrichDanHien[];
  testId?: string;
}) {
  const [moSo, setMoSo] = useState<number | null>(() => chonTrichHien(trichDan || [])?.so ?? null);
  function chon(so: number) {
    setMoSo(so);
    queueMicrotask(() => document.getElementById(`td-${so}`)?.scrollIntoView({ block: "nearest" }));
  }
  return (
    <>
      <LoiGiaSu text={text} trichDan={trichDan} onChonSo={trichDan?.length ? chon : undefined} />
      {trichDan?.length ? <TrichDanGiaSu items={trichDan} testId={testId} moSo={moSo} onChonSo={chon} /> : null}
    </>
  );
}
