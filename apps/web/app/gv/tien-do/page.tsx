import type { Metadata } from "next";
import Link from "next/link";
import { and, eq, inArray, isNull, sql } from "drizzle-orm";
import { buttonClasses } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { db } from "@/lib/db";
import { enrollments, escalations, gradingResults, masteryStates, skills, submissions, users } from "@/lib/db/schema";
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
  // §(23): lỗi trình bày dấu U (toán đúng) đếm riêng, không tính vào mức
  const trinhBay = hsIds.length
    ? await db
        .select({ hs: submissions.studentId, n: sql<number>`count(*)::int` })
        .from(gradingResults)
        .innerJoin(submissions, eq(submissions.id, gradingResults.submissionId))
        .where(and(eq(gradingResults.toanDung, true), inArray(submissions.studentId, hsIds)))
        .groupBy(submissions.studentId)
    : [];
  // UX-09-e: ô (HS × kỹ năng) có cảnh báo kẹt / lời nhờ chưa xử lý mang dấu «kẹt»
  const moKet = hsIds.length
    ? await db
        .select({ hs: escalations.studentId, kn: escalations.skillCode })
        .from(escalations)
        .where(and(isNull(escalations.handledAt), inArray(escalations.studentId, hsIds)))
    : [];
  const ket = new Set(moKet.map((k) => `${k.hs}|${k.kn}`));
  const DauKet = () => (
    <span
      data-ket="true"
      aria-label="Đang kẹt, có cảnh báo chưa xử lý"
      title="Đang kẹt, có cảnh báo chưa xử lý"
      className="ml-2 inline-block rounded-button border border-mark px-1.5 text-xs font-medium text-mark"
    >
      kẹt
    </span>
  );
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
                    <span>
                      <span>{label}</span>
                      {ket.has(`${e.userId}|${s.code}`) ? <DauKet /> : null}
                    </span>
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
                  const dau = ket.has(`${e.userId}|${s.code}`) ? <DauKet /> : null;
                  if (!st)
                    return (
                      <td key={s.code} className="p-3">
                        <span className="text-muted">—</span>
                        {dau}
                      </td>
                    );
                  const muc4 = st.currentMucDo4 as Muc4;
                  const label = view3 ? LABEL3[TO3[muc4]] : LABEL4[muc4];
                  return (
                    <td key={s.code} className="p-3">
                      <span>{label}</span>
                      {dau}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <section className="mt-8" data-testid="loi-trinh-bay-u">
        <h2 className="text-sm font-medium">Lỗi trình bày: dùng U khi kết luận (toán đúng)</h2>
        <p className="mt-1 text-sm text-muted">Đếm riêng, không tính vào mức. Học sinh viết lại tách khoảng, nối bằng «và» thì mới xong bài.</p>
        {trinhBay.length ? (
          <ul className="mt-2 divide-y divide-line border-y border-line text-sm">
            {trinhBay.map((r) => (
              <li key={r.hs} className="flex justify-between py-2">
                <span>{name.get(r.hs)}</span>
                <span className="tabular">{r.n} lần</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-muted">Chưa có.</p>
        )}
      </section>
    </main>
  );
}
