import type { Metadata } from "next";
import { and, eq, inArray } from "drizzle-orm";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Tex } from "@/components/tex";
import { requireRole } from "@/lib/auth";
import { giaoBoBai } from "@/lib/actions/gv";
import { hamLatex, nhanMuc4 } from "@/lib/de-hoc-sinh";
import { LABEL4, MUC4, STATUS_LABEL, type Muc4 } from "@/lib/levels";
import { db } from "@/lib/db";
import { assignments, enrollments, problems, submissions, users } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Đề bài",
};

const THU_TU: Record<string, number> = {
  BI_CHAN: 0,
  CHO_GIAO_VIEN_DUYET: 1,
  NHAP: 2,
  DA_PHAT_HANH: 3,
};

function tone(status: string) {
  if (status === "DA_PHAT_HANH") return "ok" as const;
  if (status === "BI_CHAN") return "bad" as const;
  return "warn" as const;
}

function ngayVN(d: Date | null) {
  if (!d) return "không hạn";
  return d.toLocaleDateString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", day: "2-digit", month: "2-digit", year: "numeric" });
}

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ muc?: string; loi?: string; da_giao?: string }>;
}) {
  const user = await requireRole("GV");
  const sp = await searchParams;
  const muc = MUC4.includes(sp.muc as Muc4) ? (sp.muc as Muc4) : null;
  const rows = [...(await db.select().from(problems))].sort(
    (a, b) => (THU_TU[a.status] ?? 9) - (THU_TU[b.status] ?? 9),
  );
  const giaoDuoc = rows.filter((p) => p.status === "DA_PHAT_HANH" && (!muc || p.mucDo4 === muc));
  // F-09: bài chưa phát hành (chưa qua 3 tầng DAT/GV_DUYET) hiện nhưng KHÔNG giao được, kèm lý do và nút "Duyệt trước"
  const chuaGiaoDuoc = rows.filter((p) => p.status !== "DA_PHAT_HANH" && (!muc || p.mucDo4 === muc));

  // Học sinh thuộc lớp của giáo viên này
  const lopGv = await db
    .select({ classId: enrollments.classId })
    .from(enrollments)
    .where(and(eq(enrollments.userId, user.id), eq(enrollments.roleInClass, "GV")));
  const lopIds = lopGv.map((l) => l.classId);
  const hsEn = lopIds.length
    ? await db
        .select({ userId: enrollments.userId })
        .from(enrollments)
        .where(and(inArray(enrollments.classId, lopIds), eq(enrollments.roleInClass, "HS")))
    : [];
  const hsIds = hsEn.map((h) => h.userId);
  const hs = hsIds.length ? await db.select().from(users).where(inArray(users.id, hsIds)) : [];

  // Bộ bài đã giao (gộp theo tên bộ + hạn)
  const giao = hsIds.length ? await db.select().from(assignments).where(inArray(assignments.studentId, hsIds)) : [];
  const dat = hsIds.length
    ? await db
        .select({ studentId: submissions.studentId, problemId: submissions.problemId })
        .from(submissions)
        .where(and(inArray(submissions.studentId, hsIds), eq(submissions.status, "da_cham"), eq(submissions.ketQua, "DAT")))
    : [];
  const daXong = new Set(dat.map((d) => `${d.studentId}|${d.problemId}`));
  const bo = new Map<string, { ten: string; han: Date | null; bai: Set<string>; hs: Set<string>; xong: number; tong: number }>();
  for (const a of giao) {
    const ten = a.setName || "Bài được giao";
    const key = `${ten}|${a.dueAt?.toISOString() ?? ""}`;
    const b = bo.get(key) || { ten, han: a.dueAt ?? null, bai: new Set(), hs: new Set(), xong: 0, tong: 0 };
    b.bai.add(a.problemId);
    b.hs.add(a.studentId);
    b.tong++;
    if (daXong.has(`${a.studentId}|${a.problemId}`)) b.xong++;
    bo.set(key, b);
  }

  return (
    <main>
      <PageHeader title="Đề bài" />

      <section aria-labelledby="giao-bo" className="mb-8">
        <h2 id="giao-bo" className="mb-2 text-base font-semibold">
          Giao bộ bài
        </h2>
        {sp.loi ? (
          <p role="alert" className="mb-2 text-sm text-danger">
            {sp.loi}
          </p>
        ) : null}
        {sp.da_giao ? (
          <p role="status" data-testid="da-giao" className="mb-2 text-sm text-pass">
            Đã giao bộ “{sp.da_giao}”.
          </p>
        ) : null}
        <nav aria-label="Lọc theo mức" className="mb-3 flex flex-wrap gap-2 text-sm">
          <a href="/gv/ngan-hang" className={!muc ? "font-semibold underline" : "underline-offset-2 hover:underline"}>
            Mọi mức
          </a>
          {MUC4.map((m) => (
            <a
              key={m}
              href={`/gv/ngan-hang?muc=${m}`}
              className={muc === m ? "font-semibold underline" : "underline-offset-2 hover:underline"}
            >
              {LABEL4[m]}
            </a>
          ))}
        </nav>
        {chuaGiaoDuoc.length ? (
          <ul className="mb-3 divide-y divide-line border-y border-line text-sm" data-testid="ds-chua-giao-duoc">
            {chuaGiaoDuoc.map((p) => {
              const ham = hamLatex(p.statementLatex);
              const lyDo =
                p.status === "CHO_GIAO_VIEN_DUYET"
                  ? "Chưa giao được: còn tầng kiểm định máy chưa kiểm được, cần thầy cô duyệt."
                  : p.status === "BI_CHAN"
                    ? "Không giao được: kiểm định báo sai."
                    : "Chưa giao được: chưa kiểm định.";
              return (
                <li key={p.id} className="flex flex-wrap items-center gap-3 py-2">
                  <input type="checkbox" disabled aria-disabled className="h-5 w-5" aria-label="Chưa giao được" />
                  <span className="min-w-0 overflow-x-auto text-muted">{ham ? <Tex tex={ham} /> : p.statementText}</span>
                  <Badge tone={tone(p.status)}>{STATUS_LABEL[p.status] || p.status}</Badge>
                  <span className="text-xs text-muted">{lyDo}</span>
                  {p.status === "CHO_GIAO_VIEN_DUYET" ? (
                    <a href="/gv/duyet" className="inline-flex min-h-11 items-center text-sm underline underline-offset-2">
                      Duyệt trước
                    </a>
                  ) : null}
                </li>
              );
            })}
          </ul>
        ) : null}
        {giaoDuoc.length === 0 ? (
          <div>
            <p className="text-sm text-muted">Không có bài đã phát hành ở mức này.</p>
            <Button type="button" disabled data-testid="giao-bo" className="mt-2">
              Giao bộ bài
            </Button>
          </div>
        ) : (
          <form action={giaoBoBai} data-testid="form-giao-bo" className="space-y-3">
            <fieldset className="border-y border-line py-2">
              <legend className="sr-only">Chọn bài</legend>
              {giaoDuoc.map((p) => {
                const ham = hamLatex(p.statementLatex);
                return (
                  <label key={p.id} className="flex min-h-11 items-center gap-3 py-1 text-sm">
                    <input type="checkbox" name="problemId" value={p.id} className="h-5 w-5" data-testid={`chon-${p.code}`} />
                    <span className="min-w-0 overflow-x-auto">{ham ? <Tex tex={ham} /> : p.statementText}</span>
                    <Badge>{LABEL4[p.mucDo4 as Muc4] || nhanMuc4(p.mucDo4)}</Badge>
                  </label>
                );
              })}
            </fieldset>
            <div className="grid gap-3 sm:grid-cols-3">
              <label className="block text-sm">
                <span className="mb-1 block font-medium">Tên bộ</span>
                <input
                  name="tenBo"
                  required
                  maxLength={120}
                  defaultValue={muc ? `Bộ ${LABEL4[muc]}` : ""}
                  className="w-full min-h-11 rounded-button border border-line bg-canvas px-3"
                />
              </label>
              <label className="block text-sm">
                <span className="mb-1 block font-medium">Giao cho</span>
                <select name="dich" className="w-full min-h-11 rounded-button border border-line bg-canvas px-3">
                  <option value="lop">Cả lớp</option>
                  {hs.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.displayName}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm">
                <span className="mb-1 block font-medium">Hạn nộp</span>
                <input type="date" name="han" className="w-full min-h-11 rounded-button border border-line bg-canvas px-3" />
              </label>
            </div>
            <Button type="submit" data-testid="giao-bo">
              Giao bộ bài
            </Button>
          </form>
        )}
      </section>

      {bo.size ? (
        <section aria-labelledby="bo-da-giao" className="mb-8">
          <h2 id="bo-da-giao" className="mb-2 text-base font-semibold">
            Bộ đã giao
          </h2>
          <ul className="divide-y divide-line border-y border-line text-sm" data-testid="ds-bo-da-giao">
            {[...bo.values()].map((b) => (
              <li key={`${b.ten}-${b.han?.toISOString() ?? ""}`} className="flex flex-wrap items-center gap-2 py-3">
                <span className="font-medium">{b.ten}</span>
                <span className="text-muted">
                  {b.bai.size} bài · {b.hs.size} học sinh · hạn {ngayVN(b.han)} · xong {b.xong}/{b.tong}
                </span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <h2 className="mb-2 text-base font-semibold">Mọi đề</h2>
      {rows.length === 0 ? <p className="text-sm text-muted">Chưa có đề.</p> : null}
      {rows.length ? (
        <ul className="divide-y divide-line border-y border-line">
          {rows.map((p) => {
            const ham = hamLatex(p.statementLatex);
            return (
              <li key={p.id} className="py-4">
                <div className="flex min-w-0 flex-wrap items-center gap-2">
                  {ham ? (
                    <span className="min-w-0 max-w-full overflow-x-auto">
                      <Tex tex={ham} className="text-sm font-medium" />
                    </span>
                  ) : (
                    <p className="text-sm font-medium">{p.statementText}</p>
                  )}
                  <Badge>{LABEL4[p.mucDo4 as Muc4] || nhanMuc4(p.mucDo4)}</Badge>
                  <Badge tone={tone(p.status)}>{STATUS_LABEL[p.status] || p.status}</Badge>
                </div>
              </li>
            );
          })}
        </ul>
      ) : null}
    </main>
  );
}
