import { cn } from "@/lib/cn";

export function BrandMark({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex h-8 w-8 items-center justify-center rounded-lg bg-navy text-chalk",
        className,
      )}
      aria-hidden
    >
      <svg viewBox="0 0 32 32" className="h-5 w-5" fill="none">
        <path
          d="M4 22C10 22 12 8 16 8s6 16 12 2"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
        />
        <path d="M6 26h20" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" opacity="0.45" />
      </svg>
    </span>
  );
}
