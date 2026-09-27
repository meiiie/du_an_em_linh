import type { Metadata } from "next";
import Link from "next/link";
import { eq } from "drizzle-orm";
import { PhieuViecTiep } from "@/components/phieu-viec-tiep";
import { SoKyNang } from "@/components/so-ky-nang";
import { Tex } from "@/components/tex";
import { WorkRow } from "@/components/work-row";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { masteryStates, problems, skills } from "@/lib/db/schema";
import { hamLatex, nhanMuc4, tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { layBaiGiao, layIdBaiDaDat, layTenLopHs, trangThaiPhieu } from "@/lib/hs-du-lieu";
import { recommend } from "@/lib/learning";
import { MUC4, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Lộ trình",
};

export default async function HsHome() {
  const u = await requireRole("HS");
  const [states, skillRows, pubs, waiting, giao, tenLop, daDat] = await Promise.all([
    db.select().from(masteryStates).where(eq(masteryStates.studentId, u.id)),
    db.select().from(skills),
    db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH")),
    db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET")),
    layBaiGiao(u.id),
    layTenLopHs(u.id),
    layIdBaiDaDat(u.id),
  ]);
  const tenKn = new Map(skillRows.map((s) => [s.code, s.name]));
  const giaoIds = new Set(giao.map((a) => a.problemId));
  const chuaXong = pubs.filter((p) => giaoIds.has(p.id) && !daDat.has(p.id));
  const goi = await recommend(u.id);
  if (goi) {
    chuaXong.sort((a, b) => {
      if (a.id === goi.problem.id) return -1;
      if (b.id === goi.problem.id) return 1;
      return MUC4.indexOf(a.mucDo4 as Muc4) - MUC4.indexOf(b.mucDo4 as Muc4);
    });
  }
  const knHang = [...states]
    .sort((a, b) => a.mastery - b.mastery || b.stuckCounter - a.stuckCounter)
    .map((s) => ({
      skillCode: s.skillCode,
      name: tenKn.get(s.skillCode),
      mastery: s.mastery,
      currentMucDo4: s.currentMucDo4,
    }));
  const buoc = goi ? await trangThaiPhieu(u.id, goi.problem.id) : { done: [], current: null, finished: false };
  const knGoi = goi?.problem.skillCode ? tenKyNangNgan(goi.problem.skillCode, tenKn.get(goi.problem.skillCode)) : "";

  return (
    <main>
      <header className="mb-8 border-b border-line pb-6">
        <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Chào {u.displayName}</h1>
        {tenLop ? <p className="mt-2 text-sm text-muted">{tenLop}</p> : null}
        <p className={tenLop ? "mt-1 text-sm text-muted" : "mt-2 text-sm text-muted"}>Toán 12, đơn điệu và cực trị</p>
      </header>

      <div className="lg:grid lg:grid-cols-[minmax(0,1.25fr)_minmax(16rem,20rem)] lg:items-start">
        <div className="min-w-0 lg:border-r lg:border-line lg:pr-8">
          <PhieuViecTiep
            problem={goi?.problem ?? null}
            lyDo={goi?.lyDo ?? ""}
            tenKn={knGoi}
            buoc={buoc}
          />
        </div>

        <div className="mt-8 min-w-0 space-y-8 lg:mt-0 lg:pl-8">
          <SoKyNang rows={knHang} dangYeu={goi?.problem.skillCode} />

          <section aria-labelledby="bai-giao">
            <div className="mb-1 flex items-end justify-between gap-2">
              <h2 id="bai-giao" className="text-base font-semibold">
                Bài giao cho em
              </h2>
              <Link href="/hs/bai" className="text-sm text-muted underline-offset-2 hover:text-ink hover:underline">
                Xem ngân bài
              </Link>
            </div>
            {chuaXong.length === 0 ? (
              <p className="mt-3 text-sm text-muted">Thầy cô chưa giao bài mới. Vào ngân bài để chọn bài đã phát hành.</p>
            ) : (
              <div className="mt-3 border-y border-line">
                {chuaXong.map((p) => {
                  const ham = hamLatex(p.statementLatex);
                  return (
                    <WorkRow
                      key={p.id}
                      href={`/hs/luyen/${p.id}`}
                      testId={`bai-${p.code}`}
                      mark={p.id === goi?.problem.id}
                      kicker={<span translate="no">{p.code}</span>}
                      title={ham ? <Tex tex={ham} /> : p.statementText}
                      meta={nhanMuc4(p.mucDo4)}
                    />
                  );
                })}
              </div>
            )}
            {waiting.length ? (
              <p className="mt-3 text-sm text-warn">
                {waiting.length} bài đang chờ thầy cô duyệt, chưa mở để làm.
              </p>
            ) : null}
          </section>
        </div>
      </div>
    </main>
  );
}
