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
  "w-full rounded-lg border border-line bg-white px-3 py-2 text-sm text-ink outline-none ring-ink/0 transition focus:border-ink focus:ring-2 focus:ring-ink/10";
