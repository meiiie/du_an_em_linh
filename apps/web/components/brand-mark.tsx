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
      <svg viewBox="0 0 32 32" className="h-4 w-4" fill="none" aria-hidden>
        <path
          d="M4 20C7 20 8 8 11.5 8s3.7 14 7 14 3.5-8 6.5-12"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path d="M4 25h20" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" opacity="0.4" />
      </svg>
    </span>
  );
}
