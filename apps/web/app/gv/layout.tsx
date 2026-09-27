import type { Metadata } from "next";
import { eq, isNull } from "drizzle-orm";
import { AppShell } from "@/components/app-shell";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { escalations, problems } from "@/lib/db/schema";
import { GV_NAV } from "@/lib/nav";
import { SITE_VERSION } from "@/lib/site";

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default async function Layout({ children }: { children: React.ReactNode }) {
  const u = await requireRole("GV");
  const queue = await db.select().from(problems).where(eq(problems.status, "CHO_GIAO_VIEN_DUYET"));
  const stuck = await db.select().from(escalations).where(isNull(escalations.handledAt));
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
