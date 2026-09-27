import Link from "next/link";
import { cn } from "@/lib/cn";
import { cacGioSlot, ngayTrongTuanHienTai, THU_TUAN, type SlotLich } from "@/lib/lich";

export type HangLich = SlotLich & { nhac: string[] };

export function LichTuan({
  slots,
  homNay,
  buoiTiep,
}: {
  slots: HangLich[];
  homNay: string;
  buoiTiep: string;
}) {
  const ngay = ngayTrongTuanHienTai();
  const gioHang = cacGioSlot(slots);
  const o = new Map(slots.map((s) => [`${s.thu}|${s.gio}`, s]));
  const thuIdx = new Map<string, number>(THU_TUAN.map((t, i) => [t.ten, i]));
  const ds = [...slots].sort(
    (a, b) => (thuIdx.get(a.thu) ?? 9) - (thuIdx.get(b.thu) ?? 9) || a.gio.localeCompare(b.gio, "vi"),
  );
  return (
    <div data-testid="lich-tuan">
      <ul className="divide-y divide-line border-y border-line md:hidden">
        {ds.map((s) => {
          const hom = s.thu === homNay;
          const dang = s.thu === buoiTiep;
          return (
            <li key={`${s.thu}|${s.gio}`} className={cn("py-3", hom && "bg-wash")}>
              <Link href="/hs" aria-current={dang ? "date" : undefined} className="block min-h-11">
                <p className="text-xs text-muted">
                  {s.thu}
                  <span className="tabular"> · {s.gio}</span>
                  {dang ? <span className="text-ink"> · buổi tiếp</span> : null}
                </p>
                <p className={cn("mt-1 text-sm leading-snug", dang && "font-medium")}>{s.viec}</p>
                {s.nhac[0] ? <p className="mt-1 text-xs text-muted">{s.nhac[0]}</p> : null}
              </Link>
            </li>
          );
        })}
      </ul>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[40rem] table-fixed border-collapse text-left">
          <colgroup>
            <col className="w-14" />
            {ngay.map((d) => (
              <col key={d.ma} />
            ))}
          </colgroup>
          <thead>
            <tr className="border-b border-line">
              <th className="w-14 py-3 pr-2" scope="col">
                <span className="sr-only">Giờ</span>
              </th>
              {ngay.map((d) => {
                const hom = d.ten === homNay;
                return (
                  <th
                    key={d.ma}
                    scope="col"
                    className={cn("px-2 py-3 font-normal", hom && "bg-wash")}
                  >
                    <span className={cn("block text-xs", hom ? "font-medium text-ink" : "text-muted")}>{d.ma}</span>
                    <span className={cn("mt-1 block text-sm tabular", hom ? "font-medium" : "text-muted")}>{d.ngay}</span>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {gioHang.map((gio) => (
              <tr key={gio} className="border-b border-line">
                <th scope="row" className="tabular py-3 pr-2 text-xs font-normal text-muted">
                  {gio}
                </th>
                {ngay.map((d) => {
                  const s = o.get(`${d.ten}|${gio}`);
                  const hom = d.ten === homNay;
                  const dang = Boolean(s && s.thu === buoiTiep);
                  return (
                    <td key={d.ma} className={cn("align-top px-2 py-3", hom && "bg-wash")}>
                      {s ? (
                        <Link
                          href="/hs"
                          aria-current={dang ? "date" : undefined}
                          className={cn("block text-sm leading-snug hover:underline", dang && "font-medium")}
                        >
                          {s.viec}
                          {s.nhac[0] ? <span className="mt-1 block text-xs font-normal text-muted">{s.nhac[0]}</span> : null}
                        </Link>
                      ) : null}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
