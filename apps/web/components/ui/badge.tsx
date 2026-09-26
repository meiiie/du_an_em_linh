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
        "inline-flex items-center rounded px-2 py-0.5 text-xs font-medium",
        tone === "neutral" && "bg-wash text-muted",
        tone === "ok" && "bg-pass/10 text-pass",
        tone === "warn" && "bg-amber-50 text-warn",
        tone === "bad" && "bg-red-50 text-mark",
        tone === "info" && "bg-wash text-ink",
        className,
      )}
    >
      {children}
    </span>
  );
}
