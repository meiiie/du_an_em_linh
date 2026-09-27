import { chuTrangThaiGiaSu, type GiaSuBuocSse } from "@/lib/sse";
import { cn } from "@/lib/cn";

const BUOC: GiaSuBuocSse[] = ["kho", "goi", "loc"];

/** Chữ SSE + 3 ô CSS. Không khối SVG, không xả token. */
export function DangNghi({ buoc }: { buoc: GiaSuBuocSse | null }) {
  const idx = buoc ? BUOC.indexOf(buoc) : -1;
  return (
    <p className="flex items-center gap-3 text-sm text-muted" data-testid="tutor-thinking" aria-live="polite">
      <span className={cn("gs-nhip", buoc == null && "gs-nhip-cho")} aria-hidden>
        {BUOC.map((b, i) => (
          <span
            key={b}
            className={cn(
              "gs-nhip-o",
              buoc == null && "gs-nhip-o-nay",
              idx === i && "gs-nhip-o-nay",
              idx > i && "gs-nhip-o-xong",
            )}
          />
        ))}
      </span>
      {chuTrangThaiGiaSu(buoc)}
    </p>
  );
}
