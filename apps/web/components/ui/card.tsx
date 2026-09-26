import type { HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export function Card({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("rounded-2xl bg-white p-4 ring-1 ring-line", className)} {...props}>
      {children}
    </div>
  );
}
