import type { Metadata } from "next";
import { and, eq, inArray, isNull } from "drizzle-orm";
import { AppShell } from "@/components/app-shell";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { escalations, problems } from "@/lib/db/schema";
import { hsCuaGv } from "@/lib/lop";
import { GV_NAV } from "@/lib/nav";
import { SITE_VERSION } from "@/lib/site";

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default async function Layout({ children }: { children: React.ReactNode }) {
  const u = await requireRole("GV");
  const queue = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  // F-08: chỉ cảnh báo của HS thuộc lớp GV này dạy
  const hsIds = await hsCuaGv(u.id);
  const stuck = hsIds.length
    ? await db.select().from(escalations).where(and(isNull(escalations.handledAt), inArray(escalations.studentId, hsIds)))
    : [];
  return (
    <AppShell
      role="GV"
      name={u.displayName}
      items={GV_NAV}
      badges={{ "/gv/duyet": queue.length, "/gv": stuck.length }}
      phien={SITE_VERSION}
    >
      {children}
    </AppShell>
  );
}
