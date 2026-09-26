import { cn } from "@/lib/cn";

export function MasteryCells({ value }: { value: number }) {
  const filled = Math.round(Math.min(1, Math.max(0, value)) * 10);
  return (
    <div className="flex gap-1" aria-hidden>
      {Array.from({ length: 10 }, (_, i) => (
        <span key={i} className={cn("size-3 rounded-[2px]", i < filled ? "bg-ink" : "bg-line")} />
      ))}
    </div>
  );
}
