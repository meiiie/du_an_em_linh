import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export function Field({
  label,
  children,
  className,
}: {
  label: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <label className={cn("block text-sm", className)}>
      <span className="mb-2 block font-medium text-ink">{label}</span>
      {children}
    </label>
  );
}

export const fieldControl =
  "w-full min-h-10 rounded-button border border-line bg-canvas px-4 py-2 text-sm text-ink outline-none transition-colors focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-ink/15 [@media(pointer:coarse)]:min-h-11";
