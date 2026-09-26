import type { HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export function Card({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("rounded-card bg-canvas p-5 ring-1 ring-line", className)} {...props}>
      {children}
    </div>
  );
}
