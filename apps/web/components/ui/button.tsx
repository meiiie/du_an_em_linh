import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger" | "accent";
  size?: "sm" | "md";
};

export function Button({ className, variant = "primary", size = "md", type = "button", ...props }: Props) {
  return (
    <button
      type={type}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition disabled:opacity-60",
        size === "sm" ? "px-3 py-1.5 text-sm" : "px-4 py-2.5 text-sm",
        variant === "primary" && "bg-ink text-paper hover:bg-ink/90",
        variant === "accent" && "bg-teal text-white hover:bg-teal/90",
        variant === "secondary" && "bg-white text-ink ring-1 ring-line hover:bg-paper",
        variant === "ghost" && "text-muted hover:bg-white/60 hover:text-ink",
        variant === "danger" && "bg-rose-800 text-white hover:bg-rose-900",
        className,
      )}
      {...props}
    />
  );
}
