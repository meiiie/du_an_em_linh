import Link from "next/link";
import { cn } from "@/lib/cn";

const TAB =
  "inline-flex min-h-10 shrink-0 items-center whitespace-nowrap border-b-2 px-3 text-sm -mb-px [@media(pointer:coarse)]:min-h-11";

export function SoNav({
  active,
  nGiao,
}: {
  active: "ky-nang" | "giao";
  nGiao: number;
}) {
  return (
    <nav aria-label="Sổ" className="flex items-end border-b border-line">
      <Link
        href="/hs"
        scroll={false}
        data-testid="tab-ky-nang"
        aria-current={active === "ky-nang" ? "page" : undefined}
        className={cn(
          TAB,
          active === "ky-nang" ? "border-ink font-medium text-ink" : "border-transparent text-muted hover:text-ink",
        )}
      >
        Kỹ năng
      </Link>
      <Link
        href="/hs?so=giao"
        scroll={false}
        data-testid="tab-bai-giao"
        aria-current={active === "giao" ? "page" : undefined}
        className={cn(
          TAB,
          active === "giao" ? "border-ink font-medium text-ink" : "border-transparent text-muted hover:text-ink",
        )}
      >
        Bài giao
        {nGiao > 0 ? <span className="tabular ml-2 text-xs text-muted">{nGiao}</span> : null}
      </Link>
    </nav>
  );
}
