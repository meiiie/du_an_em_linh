import Link from "next/link";
import { eq } from "drizzle-orm";
import { notFound } from "next/navigation";
import { SolveClient } from "@/components/solve-client";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { classSettings, problems } from "@/lib/db/schema";
import { LABEL4, type Muc4 } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function LuyenPage({ params }: { params: Promise<{ id: string }> }) {
  await requireRole("HS");
  const { id } = await params;
  const rows = await db.select().from(problems).where(eq(problems.id, id)).limit(1);
  const p = rows[0];
  if (!p) notFound();
  if (p.status === "CHO_GIAO_VIEN_DUYET") {
    return (
      <main className="rounded-2xl bg-white p-4">
        <p>Bài này đang chờ thầy cô duyệt.</p>
        <Link href="/hs" className="text-navy">
          Về lộ trình
        </Link>
      </main>
    );
  }
  if (p.status !== "DA_PHAT_HANH" || !p.hamSympy) {
    return (
      <main className="rounded-2xl bg-white p-4">
        <p>Bài chưa mở để làm.</p>
      </main>
    );
  }
  const settings = await db.select().from(classSettings);
  const showSolution = settings[0]?.moLoiGiaiSauKhiNop === true;
  return (
    <main>
      <p className="mb-2 text-sm text-slate-600">
        Mức {LABEL4[p.mucDo4 as Muc4]} · Bloom {p.bloomLevel} · mã {p.code}
      </p>
      <SolveClient problemId={p.id} title={p.statementText} latex={p.statementLatex.startsWith("y") ? p.statementLatex : `y = ${p.statementLatex}`} />
      <p className="mt-3 text-xs text-slate-500">
        {showSolution
          ? "Lớp này bật mở lời giải sau khi nộp."
          : "Mở lời giải sau khi nộp đang tắt (mặc định). Em không xem được đáp án chuẩn."}
      </p>
    </main>
  );
}
