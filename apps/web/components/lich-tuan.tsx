import Link from "next/link";
import { cn } from "@/lib/cn";
import { THU_TUAN } from "@/lib/lich";

export type HangLich = {
  thu: string;
  gio: string;
  viec: string;
  nhac: string[];
};

export function LichTuan({
  slots,
  homNay,
  buoiTiep,
}: {
  slots: HangLich[];
  homNay: string;
  buoiTiep: string;
}) {
  const coBuoi = new Set(slots.map((s) => s.thu));
  return (
    <div data-testid="lich-tuan">
      <ol className="grid max-w-sm grid-cols-7 border-b border-line" aria-label="Các ngày trong tuần">
        {THU_TUAN.map((d) => {
          const hom = d.ten === homNay;
          const co = coBuoi.has(d.ten);
          return (
            <li key={d.ma} className="flex flex-col items-center gap-1 py-3">
              <span className={cn("text-xs", hom ? "font-medium text-ink" : "text-muted")}>{d.ma}</span>
              <span className={cn("size-2 rounded-full", co ? "bg-ink" : "bg-transparent")} aria-hidden />
            </li>
          );
        })}
      </ol>
      <ol className="mt-2 divide-y divide-line border-y border-line">
        {slots.map((s) => {
          const dang = s.thu === buoiTiep;
          return (
            <li key={s.thu + s.gio}>
              <Link
                href="/hs"
                aria-current={dang ? "date" : undefined}
                className={cn(
                  "block py-4 hover:bg-wash focus-visible:bg-wash",
                  dang ? "border-l-2 border-ink bg-wash pl-4" : "pl-0",
                )}
              >
                <p className="flex items-baseline justify-between gap-4">
                  <span className="font-medium">{s.thu}</span>
                  <span className="tabular text-sm text-muted">{s.gio}</span>
                </p>
                <p className="mt-1 text-sm leading-relaxed text-muted">{s.viec}</p>
                {s.nhac.map((n) => (
                  <p key={n} className="mt-1 text-sm leading-relaxed text-muted">
                    {n}
                  </p>
                ))}
              </Link>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
