import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "danger" | "accent";
export type ButtonSize = "sm" | "md" | "icon";

export function buttonClasses({
  variant = "primary",
  size = "md",
}: {
  variant?: ButtonVariant;
  size?: ButtonSize;
} = {}) {
  return cn(
    "inline-flex items-center justify-center gap-2 rounded-button font-medium transition-colors disabled:opacity-60",
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink focus-visible:ring-offset-2",
    size === "md" && "min-h-10 px-4 text-sm [@media(pointer:coarse)]:min-h-11",
    size === "sm" && "min-h-8 px-3 text-sm [@media(pointer:coarse)]:min-h-11",
    size === "icon" && "size-11 p-0",
    variant === "primary" && "bg-ink text-chalk hover:bg-primary-hover",
    variant === "accent" && "bg-pass text-white hover:bg-pass/90",
    variant === "secondary" && "bg-canvas text-ink ring-1 ring-line hover:bg-wash",
    variant === "ghost" && "text-muted hover:bg-wash hover:text-ink",
    variant === "danger" && "bg-mark text-white hover:bg-mark/90",
  );
}

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

export function Button({ className, variant = "primary", size = "md", type = "button", ...props }: Props) {
  return <button type={type} className={cn(buttonClasses({ variant, size }), className)} {...props} />;
}
