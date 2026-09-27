import type { Metadata } from "next";
import { AppShell } from "@/components/app-shell";
import { requireRole } from "@/lib/auth";
import { demBaiChuaXong } from "@/lib/hs-du-lieu";
import { HS_NAV } from "@/lib/nav";

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default async function Layout({ children }: { children: React.ReactNode }) {
  const u = await requireRole("HS");
  const chuaXong = await demBaiChuaXong(u.id);
  return (
    <AppShell role="HS" name={u.displayName} items={HS_NAV} badges={{ "/hs/bai": chuaXong }}>
      {children}
    </AppShell>
  );
}
