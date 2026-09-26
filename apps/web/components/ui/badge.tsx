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
        "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold",
        tone === "neutral" && "bg-paper text-muted",
        tone === "ok" && "bg-teal/10 text-teal",
        tone === "warn" && "bg-amber-50 text-warn",
        tone === "bad" && "bg-rose-50 text-danger",
        tone === "info" && "bg-primary/10 text-primary",
        className,
      )}
    >
      {children}
    </span>
  );
}
