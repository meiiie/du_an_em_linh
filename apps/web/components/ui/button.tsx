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
        "inline-flex items-center justify-center gap-2 rounded-button font-medium transition-colors disabled:opacity-60",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink focus-visible:ring-offset-2",
        size === "sm" ? "px-3 py-1.5 text-sm" : "px-4 py-2.5 text-sm",
        variant === "primary" && "bg-ink text-chalk hover:bg-primary-hover",
        variant === "accent" && "bg-pass text-white hover:bg-pass/90",
        variant === "secondary" && "bg-canvas text-ink ring-1 ring-line hover:bg-wash",
        variant === "ghost" && "text-muted hover:bg-wash hover:text-ink",
        variant === "danger" && "bg-mark text-white hover:bg-mark/90",
        className,
      )}
      {...props}
    />
  );
}
