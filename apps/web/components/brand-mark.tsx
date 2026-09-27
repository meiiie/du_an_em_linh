import { cn } from "@/lib/cn";

const CO = { sm: "size-8", md: "size-10", lg: "size-16" } as const;
const NET = { sm: "h-4 w-4", md: "h-5 w-5", lg: "h-8 w-8" } as const;

export function BrandMark({
  className,
  invert = false,
  size = "sm",
}: {
  className?: string;
  invert?: boolean;
  size?: keyof typeof CO;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center justify-center rounded-button",
        CO[size],
        invert ? "bg-chalk text-ink" : "bg-ink text-chalk",
        className,
      )}
      aria-hidden
    >
      <svg viewBox="0 0 32 32" className={NET[size]} fill="none" aria-hidden>
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
