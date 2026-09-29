import type { Metadata } from "next";
import Link from "next/link";
import { and, eq, inArray } from "drizzle-orm";
import { buttonClasses } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { db } from "@/lib/db";
import { enrollments, masteryStates, skills, users } from "@/lib/db/schema";
import { caiDatLopCuaGv, lopGvDay } from "@/lib/lop";
import { LABEL3, LABEL4, TO3, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Mức",
};

export default async function Page({ searchParams }: { searchParams: Promise<{ muc?: string }> }) {
  const u = await requireRole("GV");
  const { lop } = await caiDatLopCuaGv(u);
  await ghiNhatKy(u.id, "XEM_TIEN_DO", "class", lop?.id || "-", "xem ma trận kỹ năng");
  const sp = await searchParams;
  const view3 = sp.muc === "3";
  // F-08: chỉ HS của lớp GV này dạy
  const lops = await lopGvDay(u.id);
  const ens = lops.length
    ? await db.select().from(enrollments).where(and(eq(enrollments.roleInClass, "HS"), inArray(enrollments.classId, lops)))
    : [];
  const hsIds = [...new Set(ens.map((e) => e.userId))];
  const allUsers = hsIds.length ? await db.select().from(users).where(inArray(users.id, hsIds)) : [];
  const name = new Map(allUsers.map((x) => [x.id, x.displayName]));
  const skillRows = (await db.select().from(skills)).filter((s) => s.isCore);
  const states = hsIds.length ? await db.select().from(masteryStates).where(inArray(masteryStates.studentId, hsIds)) : [];
  return (
    <main data-testid="tien-do">
      <PageHeader
        title={view3 ? "Mức lớp · 3 mức" : "Mức lớp"}
        actions={
          <Link
            data-testid="toggle-muc"
            href={view3 ? "/gv/tien-do" : "/gv/tien-do?muc=3"}
            className={buttonClasses({ variant: "secondary" })}
          >
            {view3 ? "4 mức" : "3 mức"}
          </Link>
        }
      />
      <ul className="divide-y divide-line border-y border-line md:hidden">
        {ens.map((e) => (
          <li key={e.userId} className="py-4">
            <p className="text-sm font-medium">{name.get(e.userId)}</p>
            <ul className="mt-2 space-y-1 text-sm">
              {skillRows.map((s) => {
                const st = states.find((x) => x.studentId === e.userId && x.skillCode === s.code);
                const muc4 = st?.currentMucDo4 as Muc4 | undefined;
                const label = muc4 ? (view3 ? LABEL3[TO3[muc4]] : LABEL4[muc4]) : "—";
                return (
                  <li key={s.code} className="flex justify-between gap-4">
                    <span className="text-muted">{tenKyNangNgan(s.code, s.name)}</span>
                    <span>{label}</span>
                  </li>
                );
              })}
            </ul>
          </li>
        ))}
      </ul>
      <div className="hidden overflow-x-auto border-y border-line md:block">
        <table className="min-w-full text-left text-sm">
          <thead>
            <tr className="border-b border-line bg-wash">
              <th className="p-3 font-medium">Học sinh</th>
              {skillRows.map((s) => (
                <th key={s.code} className="p-3 font-medium">
                  {tenKyNangNgan(s.code, s.name)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {ens.map((e) => (
              <tr key={e.userId} className="border-t border-line">
                <td className="p-3 font-medium">{name.get(e.userId)}</td>
                {skillRows.map((s) => {
                  const st = states.find((x) => x.studentId === e.userId && x.skillCode === s.code);
                  if (!st)
                    return (
                      <td key={s.code} className="p-3 text-muted">
                        —
                      </td>
                    );
                  const muc4 = st.currentMucDo4 as Muc4;
                  const label = view3 ? LABEL3[TO3[muc4]] : LABEL4[muc4];
                  return (
                    <td key={s.code} className="p-3">
                      {label}
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
