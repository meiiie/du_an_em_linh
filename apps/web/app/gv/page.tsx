import type { Metadata } from "next";
import Link from "next/link";
import { desc, eq, isNull } from "drizzle-orm";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { docKhoaCloud } from "@/lib/ai-harness";
import { tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { db } from "@/lib/db";
import { classSettings, documents, escalations, formulaSheets, formulas, problems, skills, users } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Lớp",
};

export default async function GvHome() {
  const u = await requireRole("GV");
  await ghiNhatKy(u.id, "XEM_TONG_QUAN", "class", "12A1");
  const stuck = await db.select().from(escalations).where(isNull(escalations.handledAt));
  const names = await db.select().from(users);
  const name = new Map(names.map((n) => [n.id, n.displayName]));
  const kn = await db.select().from(skills);
  const tenKn = new Map(kn.map((s) => [s.code, tenKyNangNgan(s.code, s.name)]));
  const queue = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  const blocked = await db.select().from(problems).where(eq(problems.status, "BI_CHAN"));
  const published = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  const setting = (await db.select().from(classSettings).limit(1))[0];
  const chatgpt = Boolean(docKhoaCloud(setting?.aiApiKey));
  const nTaiLieu = (await db.select().from(documents)).filter((d) => d.licenseStatus !== "chua_ro").length;
  const latestSheet = (await db.select().from(formulaSheets).orderBy(desc(formulaSheets.version)))[0];
  const nCongThuc = latestSheet
    ? (await db.select().from(formulas).where(eq(formulas.formulaSheetId, latestSheet.id))).length
    : 0;
  return (
    <main className="space-y-8">
      <header className="border-b border-line pb-4">
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Lớp 12A1 thử</h1>
        <p className="mt-1 text-sm text-muted">Đơn điệu và cực trị</p>
      </header>
      <section className="border-y border-line py-4" data-testid="san-sang-ai">
        <h2 className="text-sm font-semibold">Gia sư</h2>
        <dl className="mt-3 grid gap-px bg-line sm:grid-cols-2">
          <div className="bg-canvas py-3 sm:pr-4">
            <dt className="text-sm text-muted">ChatGPT</dt>
            <dd className="mt-1 text-sm font-medium">{chatgpt ? "Đã kết nối" : "Chưa kết nối"}</dd>
          </div>
          <div className="bg-canvas py-3 sm:pl-4">
            <dt className="text-sm text-muted">Tài liệu</dt>
            <dd className="mt-1 text-sm font-medium">
              {nTaiLieu} tài liệu · {nCongThuc} công thức
            </dd>
          </div>
        </dl>
        <p className="mt-3 text-sm">
          <Link href="/gv/ket-noi-ai" className="underline underline-offset-2" data-testid="toi-ket-noi-ai-tong-quan">
            Kết nối ChatGPT
          </Link>
          {" · "}
          <Link href="/gv/tai-lieu" className="underline underline-offset-2">
            Tài liệu
          </Link>
        </p>
      </section>
      <section className="border-b border-line py-4" data-testid="canh-bao-ket">
        <h2 className="text-sm font-semibold text-mark">Đang kẹt</h2>
        {stuck.length === 0 ? <p className="mt-1 text-sm text-muted">Không ai kẹt.</p> : null}
        <ul className="mt-2 space-y-1 text-sm">
          {stuck.map((e) => (
            <li key={e.id}>
              {name.get(e.studentId)} — {tenKn.get(e.skillCode) || e.skillCode}
            </li>
          ))}
        </ul>
      </section>
      <dl className="grid gap-px bg-line sm:grid-cols-3">
        <Link href="/gv/duyet" className="bg-canvas px-0 py-4 sm:px-4 sm:first:pl-0 hover:bg-wash">
          <dt className="text-sm text-muted">Chờ duyệt</dt>
          <dd className="tabular mt-1 text-2xl font-semibold">{queue.length}</dd>
        </Link>
        <Link href="/gv/ngan-hang" className="bg-canvas px-0 py-4 sm:px-4 hover:bg-wash">
          <dt className="text-sm text-muted">Bị chặn</dt>
          <dd className="tabular mt-1 text-2xl font-semibold text-mark">{blocked.length}</dd>
        </Link>
        <Link href="/gv/ngan-hang" className="bg-canvas px-0 py-4 sm:px-4 sm:last:pr-0 hover:bg-wash">
          <dt className="text-sm text-muted">Đã mở</dt>
          <dd className="tabular mt-1 text-2xl font-semibold text-pass">{published.length}</dd>
        </Link>
      </dl>
    </main>
  );
}
