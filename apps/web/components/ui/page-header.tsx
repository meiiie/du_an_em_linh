import type { ReactNode } from "react";

export function PageHeader({
  kicker,
  title,
  description,
  actions,
}: {
  kicker?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <header className="mb-6 flex flex-wrap items-end justify-between gap-4 border-b border-line pb-4">
      <div className="min-w-0">
        {kicker ? <p className="text-sm text-muted">{kicker}</p> : null}
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight text-ink">{title}</h1>
        {description ? <p className="mt-1 max-w-[65ch] text-sm text-muted">{description}</p> : null}
      </div>
      {actions}
    </header>
  );
}
