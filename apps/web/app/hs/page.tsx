import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { PhieuViecTiep } from "@/components/phieu-viec-tiep";
import { SoBaiGiao } from "@/components/so-bai-giao";
import { SoKyNang } from "@/components/so-ky-nang";
import { SoNav } from "@/components/so-nav";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { masteryStates, problems, skills } from "@/lib/db/schema";
import { tenKyNangNgan } from "@/lib/de-hoc-sinh";
import { layBaiGiao, layIdBaiDaDat, layTenLopHs, trangThaiPhieu } from "@/lib/hs-du-lieu";
import { recommend } from "@/lib/learning";
import { MUC4, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Lộ trình",
};

export default async function HsHome({ searchParams }: { searchParams: Promise<{ so?: string }> }) {
  const so = (await searchParams).so === "giao" ? "giao" : "ky-nang";
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
      <header className="mb-6 flex flex-wrap items-end justify-between gap-x-6 gap-y-2 border-b border-line pb-4">
        <div className="min-w-0">
          <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Chào {u.displayName}</h1>
          <p className="mt-1 text-sm text-muted">Toán 12, đơn điệu và cực trị</p>
        </div>
        {tenLop ? <p className="text-sm text-muted">{tenLop}</p> : null}
      </header>

      <div className="lg:grid lg:grid-cols-[minmax(0,1fr)_minmax(18rem,24rem)] lg:items-start">
        <div className="min-w-0 lg:border-r lg:border-line lg:pr-8">
          <PhieuViecTiep
            problem={goi?.problem ?? null}
            lyDo={goi?.lyDo ?? ""}
            tenKn={knGoi}
            buoc={buoc}
          />
        </div>

        <div className="mt-6 min-w-0 lg:mt-0 lg:pl-8">
          <SoNav active={so} nGiao={chuaXong.length} />
          {so === "giao" ? (
            <SoBaiGiao danhSach={chuaXong} idGoi={goi?.problem.id} waiting={waiting.length} />
          ) : (
            <SoKyNang rows={knHang} dangYeu={goi?.problem.skillCode} />
          )}
        </div>
      </div>
    </main>
  );
}
