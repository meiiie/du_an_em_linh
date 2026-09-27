import Link from "next/link";
import { buttonClasses } from "@/components/ui/button";
import { Tex } from "@/components/tex";
import { cn } from "@/lib/cn";
import { hamLatex, loiGoiHocSinh, nhanMuc4, tenBuocNgan, thanDe, tenKyNangNgan } from "@/lib/de-hoc-sinh";
import type { TrangThaiBuoc } from "@/lib/hs-du-lieu";
import { BUOC } from "@/lib/levels";

type Problem = {
  id: string;
  code: string;
  skillCode: string | null;
  mucDo4: string;
  statementText: string;
  statementLatex: string;
};

export function PhieuViecTiep({
  problem,
  lyDo,
  tenKn,
  buoc,
}: {
  problem: Problem | null;
  lyDo: string;
  tenKn: string;
  buoc: TrangThaiBuoc;
}) {
  if (!problem) {
    return (
      <section className="border-b border-line pb-6 lg:border-b-0 lg:pb-0" aria-labelledby="viec-tiep">
        <h2 id="viec-tiep" className="text-base font-semibold">
          Bài tiếp theo
        </h2>
        <p className="mt-2 max-w-[65ch] text-sm text-muted">Thầy cô chưa mở bài.</p>
      </section>
    );
  }

  const ham = hamLatex(problem.statementLatex);
  const muc = nhanMuc4(problem.mucDo4);
  const kn = tenKn || tenKyNangNgan(problem.skillCode || "");

  return (
    <section className="border-b border-line pb-6 lg:border-b-0 lg:pb-0" aria-labelledby="viec-tiep">
      <div className="flex gap-4 sm:gap-6">
        <ol className="hidden w-24 shrink-0 flex-col sm:flex" aria-label="Năm bước làm bài">
          {BUOC.map((b, i) => {
            const xong = buoc.done.includes(b.ma);
            const dang = buoc.current === b.ma && !buoc.finished;
            return (
              <li key={b.ma} className="flex min-h-11 items-center gap-2">
                <span
                  className={cn(
                    "grid size-8 place-items-center rounded-button text-xs font-medium",
                    xong && "bg-ink text-chalk",
                    dang && "text-ink ring-1 ring-ink",
                    !xong && !dang && "text-muted ring-1 ring-line",
                  )}
                  aria-current={dang ? "step" : undefined}
                >
                  {i + 1}
                </span>
                <span className={cn("text-[11px] leading-4", dang || xong ? "text-ink" : "text-muted")}>
                  {tenBuocNgan(b.ma)}
                </span>
              </li>
            );
          })}
        </ol>

        <div className="min-w-0 flex-1 border-l-2 border-ink pl-4 sm:pl-6">
          <div className="flex items-baseline justify-between gap-3">
            <h2 id="viec-tiep" className="text-pretty text-base font-semibold">
              Bài tiếp theo
            </h2>
            <p className="shrink-0 text-xs text-muted">{muc}</p>
          </div>
          <p className="mt-2 max-w-[65ch] text-sm leading-relaxed">{thanDe(problem.statementText)}</p>

          {ham ? (
            <p className="mt-4 max-w-[65ch] overflow-x-auto text-[1.25rem] leading-8" translate="no">
              <Tex tex={ham} />
            </p>
          ) : (
            <p className="mt-4 max-w-[65ch] text-lg font-medium leading-snug">{problem.statementText}</p>
          )}

          <ol className="mt-4 flex gap-3 overflow-x-auto sm:hidden" aria-label="Năm bước làm bài">
            {BUOC.map((b, i) => {
              const xong = buoc.done.includes(b.ma);
              const dang = buoc.current === b.ma && !buoc.finished;
              const ten = tenBuocNgan(b.ma);
              return (
                <li key={b.ma} className="flex shrink-0 flex-col items-center gap-1">
                  <span
                    className={cn(
                      "grid size-8 place-items-center rounded-button text-xs font-medium",
                      xong && "bg-ink text-chalk",
                      dang && "text-ink ring-1 ring-ink",
                      !xong && !dang && "text-muted ring-1 ring-line",
                    )}
                    aria-current={dang ? "step" : undefined}
                    aria-label={`${i + 1}. ${ten}${xong ? ", đã xong" : dang ? ", đang làm" : ""}`}
                  >
                    {i + 1}
                  </span>
                  <span className={cn("text-[11px] leading-4", dang || xong ? "text-ink" : "text-muted")}>{ten}</span>
                </li>
              );
            })}
          </ol>

          <p className="mt-4 max-w-[65ch] text-sm leading-relaxed text-muted">{loiGoiHocSinh(lyDo, kn)}</p>

          <Link href={`/hs/luyen/${problem.id}`} className={cn(buttonClasses(), "mt-4")}>
            Làm bước tiếp
          </Link>
        </div>
      </div>
    </section>
  );
}
