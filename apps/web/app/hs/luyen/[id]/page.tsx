import Link from "next/link";
import { eq } from "drizzle-orm";
import { notFound } from "next/navigation";
import { SolveClient } from "@/components/solve-client";
import { lichSuGiaSu } from "@/lib/actions/hs";
import { resolveProvider, type AiPublicConfig } from "@/lib/ai-catalog";
import { docKhoaCloud } from "@/lib/ai-harness";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { classSettings, problems, solutions } from "@/lib/db/schema";
import { LABEL4, type Muc4 } from "@/lib/levels";
import { loiGiaiHocSinh } from "@/lib/loi-giai";

export const dynamic = "force-dynamic";

export default async function LuyenPage({ params }: { params: Promise<{ id: string }> }) {
  await requireRole("HS");
  const { id } = await params;
  const rows = await db.select().from(problems).where(eq(problems.id, id)).limit(1);
  const p = rows[0];
  if (!p) notFound();
  if (p.status === "CHO_GIAO_VIEN_DUYET") {
    return (
      <div className="border-y border-line py-6">
        <p>Bài này thầy cô chưa mở.</p>
        <Link href="/hs" className="mt-2 inline-block text-sm underline underline-offset-2">
          Về lộ trình
        </Link>
      </div>
    );
  }
  if (p.status !== "DA_PHAT_HANH" || !p.hamSympy) {
    return (
      <div className="border-y border-line py-6">
        <p>Bài chưa mở để làm.</p>
      </div>
    );
  }
  const settings = await db.select().from(classSettings);
  const showSolution = settings[0]?.moLoiGiaiSauKhiNop === true;
  const ai: AiPublicConfig = {
    classProvider: resolveProvider({ classProvider: settings[0]?.aiProvider }),
    classModel: settings[0]?.aiModel || null,
    allowLocal: settings[0]?.aiAllowLocal !== false,
    cloudReady: Boolean(docKhoaCloud(settings[0]?.aiApiKey)),
  };
  const sol = showSolution ? (await db.select().from(solutions).where(eq(solutions.problemId, p.id)).limit(1))[0] : null;
  const loiGiai = showSolution ? loiGiaiHocSinh(sol?.baiLam, sol?.finalAnswer) : null;
  const lichSu = await lichSuGiaSu(p.id);
  return (
    <main>
      <p className="mb-4 text-sm text-muted">Mức {LABEL4[p.mucDo4 as Muc4]}</p>
      <SolveClient
        problemId={p.id}
        title={p.statementText}
        latex={p.statementLatex.startsWith("y") ? p.statementLatex : `y = ${p.statementLatex}`}
        moLoiGiai={showSolution}
        loiGiai={loiGiai}
        initialChat={lichSu.messages}
        ai={ai}
      />
      <p className="mt-3 text-xs text-muted">
        {showSolution
          ? "Lời giải mở sau khi nộp đủ năm bước. Gia sư không đọc lời giải lúc làm."
          : "Chưa xem được lời giải."}
      </p>
    </main>
  );
}
