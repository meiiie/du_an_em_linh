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
      <h2 id="ky-nang" className="text-base font-semibold">
        Kỹ năng
      </h2>
      <p className="mt-1 text-xs text-muted">Ô đặc là mức thành thạo, không phải điểm.</p>
      {rows.length === 0 ? (
        <p className="mt-4 text-sm text-muted">Chưa có ước lượng. Làm một bài để hiện năm kỹ năng.</p>
      ) : (
        <ul className="mt-4 divide-y divide-line border-y border-line">
          {rows.map((s) => {
            const yeu = s.skillCode === dangYeu;
            const ten = tenKyNangNgan(s.skillCode, s.name);
            const muc = nhanMuc4(s.currentMucDo4);
            return (
              <li
                key={s.skillCode}
                className={cn(
                  "border-l-2 py-3 pl-3",
                  yeu ? "border-ink bg-wash" : "border-transparent",
                )}
                aria-current={yeu ? "true" : undefined}
              >
                <div className="flex items-baseline justify-between gap-3">
                  <p className="font-mono text-xs text-muted" translate="no">
                    {s.skillCode}
                  </p>
                  <p className="text-xs text-muted">{muc}</p>
                </div>
                <p className="mt-1 text-sm">{ten}</p>
                <div className="mt-2">
                  <MasteryCells value={s.mastery} />
                  <span className="sr-only">
                    {ten}, {muc}
                  </span>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
