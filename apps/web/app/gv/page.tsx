import type { Metadata } from "next";
import Link from "next/link";
import { and, desc, eq, inArray, isNull } from "drizzle-orm";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { laNhaKhoa, parseProvider } from "@/lib/ai-catalog";
import { docKhoaNha } from "@/lib/ai-harness";
import { cn } from "@/lib/cn";
import { tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { db } from "@/lib/db";
import { documents, escalations, formulaSheets, formulas, problems, skills, users } from "@/lib/db/schema";
import { caiDatLopCuaGv, hsCuaGv } from "@/lib/lop";
import { daXuLyCanhBao } from "@/lib/actions/canh-bao";
import { dungCanhBao } from "@/lib/canh-bao-hien";

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
        className="flex min-h-11 items-center justify-between gap-4 py-3 transition-colors duration-150 hover:bg-wash"
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
  const { lop, setting } = await caiDatLopCuaGv(u);
  await ghiNhatKy(u.id, "XEM_TONG_QUAN", "class", lop?.id || "-");
  // F-08: chỉ HS và cảnh báo của lớp GV này dạy
  const hsIds = await hsCuaGv(u.id);
  const stuck = hsIds.length
    ? await db.select().from(escalations).where(and(isNull(escalations.handledAt), inArray(escalations.studentId, hsIds)))
    : [];
  const names = hsIds.length ? await db.select().from(users).where(inArray(users.id, hsIds)) : [];
  const name = new Map(names.map((n) => [n.id, n.displayName]));
  const kn = await db.select().from(skills);
  const tenKn = new Map(kn.map((s) => [s.code, tenKyNangNgan(s.code, s.name)]));
  const baiIds = [...new Set(stuck.map((e) => e.problemId).filter((x): x is string => Boolean(x)))];
  const baiRows = baiIds.length ? await db.select({ id: problems.id, code: problems.code }).from(problems).where(inArray(problems.id, baiIds)) : [];
  const baiMap = new Map(baiRows.map((b) => [b.id, b]));
  // UX-09: mới nhất trước; mỗi mục đủ ai, kỹ năng/bước, bài, lý do, lúc nào
  const canhBao = [...stuck]
    .sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())
    .map((e) => dungCanhBao(e, name, tenKn, baiMap));
  const queue = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  const blocked = await db.select().from(problems).where(eq(problems.status, "BI_CHAN"));
  const published = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
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
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">{lop ? `Lớp ${lop.name}` : "Chưa có lớp"}</h1>
        <p className="mt-1 text-sm text-muted">Đơn điệu và cực trị</p>
      </header>

      <ul className="divide-y divide-line border-b border-line">
        <li>
          <Link
            href="/gv/ket-noi-ai"
            data-testid="san-sang-ai"
            className="flex min-h-11 items-center justify-between gap-4 py-3 transition-colors duration-150 hover:bg-wash"
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
        <h2 className="text-sm font-semibold text-mark">Đang kẹt · nhờ thầy cô</h2>
        {canhBao.length === 0 ? <p className="mt-2 text-sm text-muted">Không ai kẹt.</p> : null}
        <ul className="mt-2 divide-y divide-line text-sm">
          {canhBao.map((c) => (
            <li key={c.id} data-testid={`canh-bao-${c.id}`} data-loai={c.nho ? "NHO_GV" : "KET"} className="flex items-stretch gap-2">
              <Link href={c.href} className="flex min-h-11 min-w-0 flex-1 flex-col justify-center py-2 underline-offset-2 hover:underline">
                <span className="font-medium">
                  {c.ten} — {c.kyNang}
                  {c.buoc ? ` · bước ${c.buoc}` : ""}
                </span>{" "}
                <span className="mt-0.5 text-xs text-muted">
                  <span data-truong="bai">{c.bai ? c.bai.code : "Không gắn bài"}</span>
                  {" · "}
                  <span data-truong="ly-do" className={c.nho ? "text-mark" : undefined}>
                    {c.nho ? "Học sinh nhờ thầy cô: " : ""}
                    {c.lyDo}
                  </span>
                  {" · "}
                  <span data-truong="luc" className="tabular">
                    {c.luc}
                  </span>
                </span>
              </Link>{" "}
              <form action={daXuLyCanhBao.bind(null, c.id)} className="flex shrink-0 items-center">
                <button
                  type="submit"
                  data-testid={`xu-ly-${c.id}`}
                  className="min-h-11 min-w-11 rounded-button border border-line px-3 text-sm hover:bg-wash"
                >
                  Đã xử lý
                </button>
              </form>
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
