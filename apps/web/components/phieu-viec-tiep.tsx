import Link from "next/link";
import { buttonClasses } from "@/components/ui/button";
import { Tex } from "@/components/tex";
import { cn } from "@/lib/cn";
import { hamLatex, loiGoiHocSinh, nhanMuc4, soBuoc, tenBuocTrang, thanDe, tenKyNangNgan } from "@/lib/de-hoc-sinh";
import type { TrangThaiBuoc } from "@/lib/hs-du-lieu";

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
  const so = buoc.current ? soBuoc(buoc.current) : "";
  const ten = buoc.current ? tenBuocTrang(buoc.current) : "";

  return (
    <section className="border-b border-line pb-6 lg:border-b-0 lg:pb-0" aria-labelledby="viec-tiep">
      <div className="flex items-baseline justify-between gap-3">
        <h2 id="viec-tiep" className="text-pretty text-base font-semibold">
          Bài tiếp theo
        </h2>
        <p className="shrink-0 text-xs text-muted">{muc}</p>
      </div>
      <p className="mt-2 max-w-[65ch] text-sm leading-relaxed">{thanDe(problem.statementText)}</p>

      {ham ? (
        <p className="mt-6 max-w-[65ch] overflow-x-auto text-[1.75rem] leading-tight sm:text-[2rem]" translate="no">
          <Tex tex={ham} block />
        </p>
      ) : (
        <p className="mt-6 max-w-[65ch] text-lg font-medium leading-snug">{problem.statementText}</p>
      )}

      {!buoc.finished && so ? (
        <p className="mt-4 text-sm">
          <span className="font-mono text-xs tabular text-muted">{so}</span>
          <span className="mx-2 text-muted">/</span>
          <span className="font-medium">{ten}</span>
        </p>
      ) : null}

      <p className="mt-4 max-w-[65ch] text-sm leading-relaxed text-muted">{loiGoiHocSinh(lyDo, kn)}</p>

      <Link href={`/hs/luyen/${problem.id}`} className={cn(buttonClasses(), "mt-4")}>
        Làm bước tiếp
      </Link>
    </section>
  );
}
