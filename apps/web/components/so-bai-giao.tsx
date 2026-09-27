import { Tex } from "@/components/tex";
import { WorkRow } from "@/components/work-row";
import { hamLatex, nhanMuc4 } from "@/lib/de-hoc-sinh";

type Bai = {
  id: string;
  code: string;
  mucDo4: string;
  statementText: string;
  statementLatex: string;
};

export function SoBaiGiao({
  danhSach,
  idGoi,
  waiting,
}: {
  danhSach: Bai[];
  idGoi?: string;
  waiting: number;
}) {
  return (
    <section aria-labelledby="bai-giao">
      <h2 id="bai-giao" className="sr-only">
        Bài tập
      </h2>
      {danhSach.length === 0 ? (
        <p className="mt-3 text-sm text-muted">Thầy cô chưa giao bài. Vào Ngân bài để chọn.</p>
      ) : (
        <div className="border-b border-line">
          {danhSach.map((p) => {
            const ham = hamLatex(p.statementLatex);
            return (
              <WorkRow
                key={p.id}
                href={`/hs/luyen/${p.id}`}
                testId={`bai-${p.code}`}
                mark={p.id === idGoi}
                title={ham ? <Tex tex={ham} /> : p.statementText}
                meta={nhanMuc4(p.mucDo4)}
              />
            );
          })}
        </div>
      )}
      {waiting ? <p className="mt-3 text-sm text-warn">{waiting} bài thầy cô chưa mở.</p> : null}
    </section>
  );
}
