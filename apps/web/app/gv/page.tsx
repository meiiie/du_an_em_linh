import type { Metadata } from "next";
import Link from "next/link";
import { desc, eq, isNull } from "drizzle-orm";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { laNhaKhoa, parseProvider } from "@/lib/ai-catalog";
import { docKhoaNha } from "@/lib/ai-harness";
import { cn } from "@/lib/cn";
import { tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { db } from "@/lib/db";
import { classSettings, documents, escalations, formulaSheets, formulas, problems, skills, users } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Lớp",
};

function Hang({
  href,
  label,
  meta,
  mark,
  testId,
}: {
  href: string;
  label: string;
  meta: string;
  mark?: boolean;
  testId?: string;
}) {
  return (
    <li>
      <Link
        href={href}
        data-testid={testId}
        className="flex min-h-11 items-center justify-between gap-4 py-3 hover:bg-wash"
      >
        <span className="text-sm">{label}</span>
        <span className={cn("tabular text-sm", mark && "text-mark", !mark && meta === "Đã kết nối" && "text-pass")}>
          {meta}
        </span>
      </Link>
    </li>
  );
}

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
  const nha = parseProvider(setting?.aiProvider);
  const daKetNoi = laNhaKhoa(nha) && Boolean(docKhoaNha(nha, setting?.aiApiKey, nha));
  const nTaiLieu = (await db.select().from(documents)).filter((d) => d.licenseStatus !== "chua_ro").length;
  const latestSheet = (await db.select().from(formulaSheets).orderBy(desc(formulaSheets.version)))[0];
  const nCongThuc = latestSheet
    ? (await db.select().from(formulas).where(eq(formulas.formulaSheetId, latestSheet.id))).length
    : 0;
  return (
    <main>
      <header className="mb-6 border-b border-line pb-4">
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Lớp 12A1 thử</h1>
        <p className="mt-1 text-sm text-muted">Đơn điệu và cực trị</p>
      </header>

      <ul className="divide-y divide-line border-b border-line">
        <li>
          <Link
            href="/gv/ket-noi-ai"
            data-testid="san-sang-ai"
            className="flex min-h-11 items-center justify-between gap-4 py-3 hover:bg-wash"
          >
            <span className="text-sm">Gia sư</span>
            <span className="text-right text-sm">
              <span className={daKetNoi ? "text-pass" : undefined}>{daKetNoi ? "Đã kết nối" : "Chưa kết nối"}</span>
              <span className="mt-1 block text-xs text-muted">
                {nTaiLieu} tài liệu · {nCongThuc} công thức
              </span>
            </span>
          </Link>
        </li>
      </ul>

      <section className="border-b border-line py-4" data-testid="canh-bao-ket">
        <h2 className="text-sm font-semibold text-mark">Đang kẹt</h2>
        {stuck.length === 0 ? <p className="mt-2 text-sm text-muted">Không ai kẹt.</p> : null}
        <ul className="mt-2 space-y-1 text-sm">
          {stuck.map((e) => (
            <li key={e.id}>
              {name.get(e.studentId)} — {tenKn.get(e.skillCode) || e.skillCode}
            </li>
          ))}
        </ul>
      </section>

      <ul className="divide-y divide-line border-b border-line">
        <Hang href="/gv/duyet" label="Chờ duyệt" meta={String(queue.length)} />
        <Hang href="/gv/ngan-hang" label="Bị chặn" meta={String(blocked.length)} mark={blocked.length > 0} />
        <Hang href="/gv/ngan-hang" label="Đã mở" meta={String(published.length)} />
      </ul>
    </main>
  );
}
