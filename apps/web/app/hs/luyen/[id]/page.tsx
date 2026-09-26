import Link from "next/link";
import { eq } from "drizzle-orm";
import { notFound } from "next/navigation";
import { SolveClient } from "@/components/solve-client";
import { Card } from "@/components/ui/card";
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
      <Card>
        <p>Bài này đang chờ thầy cô duyệt.</p>
        <Link href="/hs" className="text-navy">
          Về lộ trình
        </Link>
      </Card>
    );
  }
  if (p.status !== "DA_PHAT_HANH" || !p.hamSympy) {
    return (
      <Card>
        <p>Bài chưa mở để làm.</p>
      </Card>
    );
  }
  const settings = await db.select().from(classSettings);
  const showSolution = settings[0]?.moLoiGiaiSauKhiNop === true;
  const sol = showSolution ? (await db.select().from(solutions).where(eq(solutions.problemId, p.id)).limit(1))[0] : null;
  const loiGiai = showSolution ? loiGiaiHocSinh(sol?.baiLam, sol?.finalAnswer) : null;
  return (
    <main>
      <p className="mb-3 text-sm text-muted">
        Mức {LABEL4[p.mucDo4 as Muc4]} · Bloom {p.bloomLevel} · mã {p.code}
      </p>
      <SolveClient
        problemId={p.id}
        title={p.statementText}
        latex={p.statementLatex.startsWith("y") ? p.statementLatex : `y = ${p.statementLatex}`}
        moLoiGiai={showSolution}
        loiGiai={loiGiai}
      />
      <p className="mt-3 text-xs text-muted">
        {showSolution
          ? "Lớp này bật mở lời giải sau khi nộp xong cả năm bước. Gia sư vẫn không đọc lời giải trong lúc làm."
          : "Mở lời giải sau khi nộp đang tắt (mặc định). Em không xem được đáp án chuẩn."}
      </p>
    </main>
  );
}
