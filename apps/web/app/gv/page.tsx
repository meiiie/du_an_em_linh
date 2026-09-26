import Link from "next/link";
import { eq, isNull } from "drizzle-orm";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { requireRole } from "@/lib/auth";
import { ghiNhatKy } from "@/lib/actions/hs";
import { db } from "@/lib/db";
import { escalations, problems, users } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function GvHome() {
  const u = await requireRole("GV");
  await ghiNhatKy(u.id, "XEM_TONG_QUAN", "class", "12A1");
  const stuck = await db.select().from(escalations).where(isNull(escalations.handledAt));
  const names = await db.select().from(users);
  const name = new Map(names.map((n) => [n.id, n.displayName]));
  const queue = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  const blocked = await db.select().from(problems).where(eq(problems.status, "BI_CHAN"));
  const published = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  return (
    <main className="space-y-5">
      <PageHeader
        kicker="Cổng giáo viên"
        title="Lớp 12A1 thử"
        description="Dữ liệu tổng hợp, không có học sinh thật. Cổng phụ huynh chưa mở."
      />
      <section className="rounded-card border border-rose-200 bg-canvas p-5" data-testid="canh-bao-ket">
        <h2 className="font-semibold text-danger">Học sinh bị kẹt</h2>
        {stuck.length === 0 ? <p className="mt-1 text-sm text-muted">Chưa có cảnh báo.</p> : null}
        <ul className="mt-2 space-y-1 text-sm">
          {stuck.map((e) => (
            <li key={e.id}>
              {name.get(e.studentId)} — {e.skillCode}: {e.reason}
            </li>
          ))}
        </ul>
      </section>
      <section className="grid gap-3 sm:grid-cols-3">
        <Link href="/gv/duyet" className="rounded-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
          <Card>
            <p className="tabular text-3xl font-semibold text-primary">{queue.length}</p>
            <p className="text-sm text-muted">bài chờ duyệt</p>
          </Card>
        </Link>
        <Link href="/gv/ngan-hang" className="rounded-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
          <Card>
            <p className="tabular text-3xl font-semibold text-danger">{blocked.length}</p>
            <p className="text-sm text-muted">bài bị chặn</p>
          </Card>
        </Link>
        <Link href="/gv/ngan-hang" className="rounded-card focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
          <Card>
            <p className="tabular text-3xl font-semibold text-teal">{published.length}</p>
            <p className="text-sm text-muted">bài đã phát hành</p>
          </Card>
        </Link>
      </section>
    </main>
  );
}
