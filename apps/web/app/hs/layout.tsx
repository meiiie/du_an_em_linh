import { eq } from "drizzle-orm";
import { AppShell } from "@/components/app-shell";
import { requireRole } from "@/lib/auth";
import { db } from "@/lib/db";
import { problems } from "@/lib/db/schema";
import { HS_NAV } from "@/lib/nav";

export default async function Layout({ children }: { children: React.ReactNode }) {
  const u = await requireRole("HS");
  const pubs = await db.select().from(problems).where(eq(problems.status, "DA_PHAT_HANH"));
  return (
    <AppShell role="HS" name={u.displayName} items={HS_NAV} badges={{ "/hs/bai": pubs.length }}>
      {children}
    </AppShell>
  );
}
