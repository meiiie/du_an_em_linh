import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { and, desc, eq, inArray, isNull } from "drizzle-orm";
import { PageHeader } from "@/components/ui/page-header";
import { Tex } from "@/components/tex";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { daXuLyCanhBao } from "@/lib/actions/canh-bao";
import { dungCanhBao, gioCanhBao } from "@/lib/canh-bao-hien";
import { hamLatex, tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { db } from "@/lib/db";
import { escalations, problems, skills, submissions, users } from "@/lib/db/schema";
import { gvDayHs } from "@/lib/lop";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Học sinh",
};

const TRANG_THAI: Record<string, string> = {
  DAT: "Đạt",
  SAI: "Sai",
  KHONG_KIEM_DUOC: "Không kiểm được",
  dang_cham: "Đang chấm",
  da_cham: "Đã chấm",
};

/** UX-09-c: trang đích của một cảnh báo kẹt — ai, bài nào, nộp gần nhất ra sao. F-08: chỉ HS lớp GV dạy. */
export default async function Page({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ bai?: string }> }) {
  const u = await requireRole("GV");
  const { id } = await params;
  const { bai } = await searchParams;
  if (!/^[0-9a-f-]{36}$/i.test(id) || !(await gvDayHs(u.id, id))) notFound();
  const hs = (await db.select().from(users).where(eq(users.id, id)).limit(1))[0];
  if (!hs) notFound();
  await ghiNhatKy(u.id, "XEM_HOC_SINH", "user", id, bai ? `bài ${bai}` : "xem học sinh");
  const baiRow = bai && /^[0-9a-f-]{36}$/i.test(bai) ? (await db.select().from(problems).where(eq(problems.id, bai)).limit(1))[0] : undefined;
  const nop = baiRow
    ? await db
        .select()
        .from(submissions)
        .where(and(eq(submissions.studentId, id), eq(submissions.problemId, baiRow.id)))
        .orderBy(desc(submissions.submittedAt))
        .limit(5)
    : [];
  const mo = await db
    .select()
    .from(escalations)
    .where(and(eq(escalations.studentId, id), isNull(escalations.handledAt)))
    .orderBy(desc(escalations.createdAt));
  const kn = await db.select().from(skills);
  const tenKn = new Map(kn.map((s) => [s.code, tenKyNangNgan(s.code, s.name)]));
  const baiIds = [...new Set(mo.map((e) => e.problemId).filter((x): x is string => Boolean(x)))];
  const baiRows = baiIds.length ? await db.select({ id: problems.id, code: problems.code }).from(problems).where(inArray(problems.id, baiIds)) : [];
  const canhBao = mo.map((e) => dungCanhBao(e, new Map([[id, hs.displayName]]), tenKn, new Map(baiRows.map((b) => [b.id, b]))));
  const ham = baiRow ? hamLatex(baiRow.statementLatex) : null;
  return (
    <main data-testid="gv-hoc-sinh">
      <PageHeader kicker="Học sinh" title={hs.displayName} />
      {baiRow ? (
        <section className="border-b border-line pb-4" data-testid="gv-hs-bai">
          <h2 className="text-sm font-semibold">
            Bài <span className="tabular">{baiRow.code}</span>
          </h2>
          <div className="mt-2">{ham ? <Tex tex={ham} /> : null}</div>
          <p className="mt-1 text-sm text-muted">{baiRow.statementText}</p>
          <h3 className="mt-4 text-sm font-medium">Lần nộp gần nhất</h3>
          {nop.length ? (
            <ul className="mt-1 divide-y divide-line text-sm" data-testid="gv-hs-nop">
              {nop.map((n) => (
                <li key={n.id} className="flex justify-between gap-4 py-2">
                  <span>{TRANG_THAI[n.ketQua || n.status] || n.ketQua || n.status}</span>
                  <span className="tabular text-muted">{gioCanhBao(n.submittedAt)}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-1 text-sm text-muted">Chưa nộp bài này.</p>
          )}
        </section>
      ) : null}
      <section className="mt-6" data-testid="gv-hs-canh-bao">
        <h2 className="text-sm font-semibold">Cảnh báo chưa xử lý</h2>
        {canhBao.length ? (
          <ul className="mt-2 divide-y divide-line text-sm">
            {canhBao.map((c) => (
              <li key={c.id} className="flex items-center justify-between gap-2 py-2">
                <span>
                  {c.kyNang}
                  {c.buoc ? ` · bước ${c.buoc}` : ""} · {c.bai ? c.bai.code : "Không gắn bài"} · {c.nho ? "Học sinh nhờ thầy cô: " : ""}
                  {c.lyDo} · <span className="tabular">{c.luc}</span>
                </span>
                <form action={daXuLyCanhBao.bind(null, c.id)}>
                  <button type="submit" className="min-h-11 min-w-11 rounded-button border border-line px-3 text-sm hover:bg-wash">
                    Đã xử lý
                  </button>
                </form>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-muted">Không còn cảnh báo mở.</p>
        )}
      </section>
      <p className="mt-6">
        <Link href="/gv" className="inline-flex min-h-11 items-center text-sm underline underline-offset-2">
          ← Về lớp
        </Link>
      </p>
    </main>
  );
}
