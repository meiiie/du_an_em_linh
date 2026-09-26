import Link from "next/link";
import { eq } from "drizzle-orm";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { db } from "@/lib/db";
import { enrollments, masteryStates, skills, users } from "@/lib/db/schema";
import { LABEL3, LABEL4, TO3, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function Page({ searchParams }: { searchParams: Promise<{ muc?: string }> }) {
  const u = await requireRole("GV");
  await ghiNhatKy(u.id, "XEM_TIEN_DO", "class", "12A1", "xem ma trận kỹ năng");
  const sp = await searchParams;
  const view3 = sp.muc === "3";
  const ens = await db.select().from(enrollments).where(eq(enrollments.roleInClass, "HS"));
  const allUsers = await db.select().from(users);
  const name = new Map(allUsers.map((x) => [x.id, x.displayName]));
  const skillRows = (await db.select().from(skills)).filter((s) => s.isCore);
  const states = await db.select().from(masteryStates);
  return (
    <main data-testid="tien-do">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-bold">{view3 ? "Tiến độ theo 3 mức (CV 7991)" : "Tiến độ theo 4 mức"}</h1>
        <Link
          data-testid="toggle-muc"
          href={view3 ? "/gv/tien-do" : "/gv/tien-do?muc=3"}
          className="rounded-full bg-white px-3 py-1.5 text-sm shadow ring-1 ring-stone-200"
        >
          {view3 ? "Xem 4 mức" : "Xem 3 mức Biết / Hiểu / Vận dụng"}
        </Link>
      </div>
      <p className="mb-2 text-xs text-slate-500">
        Ô ghi mức hiện tại và xác suất thành thạo của mô hình. Mức 3 được suy ra lúc hiển thị, không lưu riêng trên học sinh.
      </p>
      <div className="overflow-x-auto rounded-2xl bg-white shadow-sm ring-1 ring-stone-200">
        <table className="min-w-full text-left text-xs">
          <thead>
            <tr>
              <th className="p-2">Học sinh</th>
              {skillRows.map((s) => (
                <th key={s.code} className="p-2">
                  {s.code}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {ens.map((e) => (
              <tr key={e.userId} className="border-t border-stone-100">
                <td className="p-2 font-medium">{name.get(e.userId)}</td>
                {skillRows.map((s) => {
                  const st = states.find((x) => x.studentId === e.userId && x.skillCode === s.code);
                  if (!st) return <td key={s.code} className="p-2 text-slate-400">—</td>;
                  const muc4 = st.currentMucDo4 as Muc4;
                  const label = view3 ? LABEL3[TO3[muc4]] : LABEL4[muc4];
                  return (
                    <td key={s.code} className="p-2">
                      {label}
                      <span className="block text-slate-500">{st.mastery.toFixed(2)}</span>
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  );
}
