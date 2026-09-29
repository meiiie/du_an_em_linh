import type { Metadata } from "next";
import { AppShell } from "@/components/app-shell";
import { requireRole } from "@/lib/auth";
import { cheDoTest } from "@/lib/che-do-test";
import { HS_NAV } from "@/lib/nav";
import { SITE_VERSION } from "@/lib/site";

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default async function Layout({ children }: { children: React.ReactNode }) {
  const u = await requireRole("HS");
  return (
    <AppShell
      role="HS"
      name={u.displayName}
      items={HS_NAV}
      phien={SITE_VERSION}
      datLaiTest={cheDoTest() ? { email: u.email } : undefined}
    >
      {children}
    </AppShell>
  );
}
