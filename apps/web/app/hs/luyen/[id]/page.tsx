import Link from "next/link";
import { caiDatLopCuaHs } from "@/lib/lop";
import { eq } from "drizzle-orm";
import { notFound } from "next/navigation";
import { SolveClient } from "@/components/solve-client";
import { lichSuGiaSu } from "@/lib/actions/hs";
import type { AiPublicConfig } from "@/lib/ai-catalog";
import { cauHinhCongKhai } from "@/lib/ai-harness";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { problems, solutions } from "@/lib/db/schema";
import { hamLatex } from "@/lib/de-hoc-sinh";
import { trangThaiPhieu } from "@/lib/hs-du-lieu";
import { loiGiaiHocSinh } from "@/lib/loi-giai";
import { capGoiYTheoBuoc } from "@/lib/gia-su-luot";
import { tienTrinhBai } from "@/lib/tien-trinh";
import { BUOC } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function LuyenPage({ params }: { params: Promise<{ id: string }> }) {
  const u = await requireRole("HS");
  const { id } = await params;
  const rows = await db.select().from(problems).where(eq(problems.id, id)).limit(1);
  const p = rows[0];
  if (!p) notFound();
  if (p.status === "CHO_GIAO_VIEN_DUYET") {
    return (
      <div className="border-y border-line py-6">
        <p>Bài này thầy cô chưa mở.</p>
        <Link href="/hs" className="mt-2 inline-block text-sm underline underline-offset-2">
          Về trang học
        </Link>
      </div>
    );
  }
  if (p.status !== "DA_PHAT_HANH" || !p.hamSympy || (p.dangTraLoi && p.dangTraLoi !== "TU_LUAN_5_BUOC")) {
    const dangKhac = p.dangTraLoi && p.dangTraLoi !== "TU_LUAN_5_BUOC";
    return (
      <div className="border-y border-line py-6">
        <p>
          {dangKhac
            ? "Dạng câu này (không theo khung 5 bước) chưa có khung làm bài trên ứng dụng. Em làm ra giấy và nộp thầy cô."
            : "Bài chưa mở để làm."}
        </p>
        <Link href="/hs" className="mt-2 inline-block text-sm underline underline-offset-2">
          Về trang học
        </Link>
      </div>
    );
  }
  // F-08: cài đặt của lớp HS này
  const setting = await caiDatLopCuaHs(u.id);
  const showSolution = setting?.moLoiGiaiSauKhiNop === true;
  const ai: AiPublicConfig = cauHinhCongKhai({
    classProvider: setting?.aiProvider,
    classModel: setting?.aiModel,
    allowLocal: setting?.aiAllowLocal,
    classApiKey: setting?.aiApiKey,
  });
  // F-05: chỉ đưa lời giải vào trang khi CHÍNH HS này đã xong bài; chưa xong thì lời giải không có trong HTML/RSC.
  const daXong = showSolution ? (await trangThaiPhieu(u.id, p.id)).finished : false;
  const sol = daXong ? (await db.select().from(solutions).where(eq(solutions.problemId, p.id)).limit(1))[0] : null;
  const loiGiai = daXong ? loiGiaiHocSinh(sol?.baiLam, sol?.finalAnswer) : null;
  const lichSu = await lichSuGiaSu(p.id);
  // UX-06 / UX-07: tải lại mở đúng bước đang dở và đúng cấp gợi ý (dựng từ lượt nộp gần nhất và phiên gia sư).
  const batDau = p.buocBatDau ? Math.max(0, BUOC.findIndex((b) => b.ma === p.buocBatDau)) : 0;
  const [tienTrinh, capGoiY] = await Promise.all([tienTrinhBai(u.id, p.id, batDau), capGoiYTheoBuoc(u.id, p.id)]);
  return (
    <main>
      <SolveClient
        problemId={p.id}
        title={p.statementText}
        latex={hamLatex(p.statementLatex)}
        moLoiGiai={showSolution}
        loiGiai={loiGiai}
        initialChat={lichSu.messages}
        ai={ai}
        buocBatDau={p.buocBatDau}
        tienTrinh={tienTrinh}
        capGoiY={capGoiY}
      />
    </main>
  );
}
