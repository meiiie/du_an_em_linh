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
      <span className="mb-1 block font-medium text-ink">{label}</span>
      {children}
    </label>
  );
}

export const fieldControl =
  "w-full rounded-button border border-line bg-canvas px-3 py-2 text-sm text-ink outline-none transition-colors focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/20";
