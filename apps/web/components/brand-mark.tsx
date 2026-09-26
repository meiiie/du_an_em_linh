import { cn } from "@/lib/cn";

export function BrandMark({ className, invert = false }: { className?: string; invert?: boolean }) {
  return (
    <span
      className={cn(
        "inline-flex size-8 items-center justify-center rounded-button",
        invert ? "bg-chalk text-ink" : "bg-ink text-chalk",
        className,
      )}
      aria-hidden
    >
      <svg viewBox="0 0 32 32" className="h-4 w-4" fill="none">
        <path
          d="M4 22C10 22 12 8 16 8s6 16 12 2"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
        />
        <path d="M6 26h20" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" opacity="0.4" />
      </svg>
    </span>
  );
}
