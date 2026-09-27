import { MasteryCells } from "@/components/mastery-cells";
import { cn } from "@/lib/cn";
import { nhanMuc4, tenKyNangNgan } from "@/lib/de-hoc-sinh";

export function SoKyNang({
  rows,
  dangYeu,
}: {
  rows: { skillCode: string; name?: string; mastery: number; currentMucDo4: string }[];
  dangYeu?: string | null;
}) {
  return (
    <section aria-labelledby="ky-nang">
      <h2 id="ky-nang" className="sr-only">
        Kỹ năng
      </h2>
      {rows.length === 0 ? (
        <p className="mt-3 text-sm text-muted">Chưa làm bài — làm một bài để hiện kỹ năng.</p>
      ) : (
        <ul className="mt-3 divide-y divide-line border-y border-line">
          {rows.map((s) => {
            const yeu = s.skillCode === dangYeu;
            const ten = tenKyNangNgan(s.skillCode, s.name);
            const muc = nhanMuc4(s.currentMucDo4);
            return (
              <li
                key={s.skillCode}
                className={cn("border-l-2 py-3 pl-3", yeu ? "border-ink bg-wash" : "border-transparent")}
                aria-current={yeu ? "true" : undefined}
                aria-label={`${ten}, ${muc}`}
              >
                <div className="flex items-baseline justify-between gap-3">
                  <p className="text-sm">{ten}</p>
                  <p className="shrink-0 text-xs text-muted">{muc}</p>
                </div>
                <div className="mt-2">
                  <MasteryCells value={s.mastery} />
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
