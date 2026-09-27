import Link from "next/link";
import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export function WorkRow({
  href,
  kicker,
  title,
  meta,
  testId,
  mark,
}: {
  href: string;
  kicker?: ReactNode;
  title: ReactNode;
  meta?: ReactNode;
  testId?: string;
  mark?: boolean;
}) {
  return (
    <Link
      href={href}
      data-testid={testId}
      className={cn(
        "flex min-h-11 items-start justify-between gap-4 border-b border-line py-3 last:border-0 hover:bg-wash focus-visible:bg-wash",
        mark && "bg-wash",
      )}
    >
      <span className="min-w-0">
        {kicker ? <span className="mb-0.5 block text-xs text-muted">{kicker}</span> : null}
        <span className="block min-w-0 overflow-x-auto text-sm font-medium text-ink">{title}</span>
      </span>
      {meta ? <span className="shrink-0 pt-0.5 text-xs text-muted">{meta}</span> : null}
    </Link>
  );
}
