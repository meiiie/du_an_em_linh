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
import { layBaiDangDo, layBaiGiao, layIdBaiDaDat, layTenLopHs, trangThaiPhieu } from "@/lib/hs-du-lieu";
import { recommend } from "@/lib/learning";
import { MUC4, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Học",
};

export default async function HsHome({ searchParams }: { searchParams: Promise<{ so?: string }> }) {
  const so = (await searchParams).so === "giao" ? "giao" : "ky-nang";
  const u = await requireRole("HS");
  const [states, skillRows, pubs, giao, tenLop, daDat] = await Promise.all([
    db.select().from(masteryStates).where(eq(masteryStates.studentId, u.id)),
    db.select().from(skills),
    db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH")),
    layBaiGiao(u.id),
    layTenLopHs(u.id),
    layIdBaiDaDat(u.id),
  ]);
  const tenKn = new Map(skillRows.map((s) => [s.code, s.name]));
  const giaoIds = new Set(giao.map((a) => a.problemId));
  const chuaXong = pubs.filter((p) => giaoIds.has(p.id) && !daDat.has(p.id));
  const goiMay = await recommend(u.id);
  // SP-02: máy chỉ gợi ý bài ≤ mức hiện tại + 1. Không có bài hợp mức thì phiếu việc tiếp là bài thầy cô giao
  // (hạn gần nhất), ghi rõ lý do — không giả làm "gợi ý theo mức".
  const giaoSom = [...chuaXong].sort(
    (a, b) =>
      (giao.find((g) => g.problemId === a.id)?.dueAt?.getTime() ?? Infinity) -
      (giao.find((g) => g.problemId === b.id)?.dueAt?.getTime() ?? Infinity),
  )[0];
  // UXT-06-d: bài đang làm dở (đã qua ≥ 1 bước, chưa Đạt) đứng trước gợi ý mới, để Trang Học khớp màn làm bài.
  const dangDo = await layBaiDangDo(u.id, new Set(pubs.map((p) => p.id)), daDat);
  const baiDo = dangDo ? pubs.find((p) => p.id === dangDo.problemId) : undefined;
  const goi =
    (baiDo ? { problem: baiDo, lyDo: "Đang làm dở" } : null) ??
    goiMay ??
    (giaoSom
      ? { problem: giaoSom, lyDo: "Bài thầy cô giao, hạn gần nhất. Hiện chưa có bài đúng mức em để máy gợi ý thêm." }
      : null);
  if (goi) {
    chuaXong.sort((a, b) => {
      if (a.id === goi.problem.id) return -1;
      if (b.id === goi.problem.id) return 1;
      const ha = giao.find((g) => g.problemId === a.id)?.dueAt?.getTime() ?? Infinity;
      const hb = giao.find((g) => g.problemId === b.id)?.dueAt?.getTime() ?? Infinity;
      if (ha !== hb) return ha - hb;
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
  const buoc =
    dangDo && goi?.problem.id === dangDo.problemId
      ? dangDo.buoc
      : goi
        ? await trangThaiPhieu(u.id, goi.problem.id)
        : { done: [], current: null, finished: false };
  const knGoi = goi?.problem.skillCode ? tenKyNangNgan(goi.problem.skillCode, tenKn.get(goi.problem.skillCode)) : "";

  return (
    <main>
      <header className="mb-6 flex flex-wrap items-end justify-between gap-x-6 gap-y-2 border-b border-line pb-4">
        <div className="min-w-0">
          <h1 className="text-pretty text-[1.75rem] font-semibold tracking-tight">Chào {u.displayName}</h1>
          <p className="mt-1 text-sm text-muted">Đơn điệu và cực trị</p>
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
            <SoBaiGiao
              danhSach={chuaXong}
              idGoi={goi?.problem.id}
              giao={Object.fromEntries(
                giao.map((a) => [a.problemId, { bo: a.setName ?? null, han: a.dueAt ? a.dueAt.toISOString() : null }]),
              )}
            />
          ) : (
            <SoKyNang rows={knHang} dangYeu={goi?.problem.skillCode} />
          )}
        </div>
      </div>
    </main>
  );
}
