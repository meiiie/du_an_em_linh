import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export function Badge({
  children,
  tone = "neutral",
  className,
}: {
  children: ReactNode;
  tone?: "neutral" | "ok" | "warn" | "bad" | "info";
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide",
        tone === "neutral" && "bg-stone-100 text-stone-700",
        tone === "ok" && "bg-emerald-50 text-emerald-800",
        tone === "warn" && "bg-amber-50 text-amber-900",
        tone === "bad" && "bg-rose-50 text-rose-800",
        tone === "info" && "bg-sky-50 text-sky-800",
        className,
      )}
    >
      {children}
    </span>
  );
}
