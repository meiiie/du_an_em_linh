import { Shell } from "@/components/shell";
import { requireRole } from "@/lib/auth";

export default async function Layout({ children }: { children: React.ReactNode }) {
  const u = await requireRole("HS");
  return (
    <Shell role="HS" name={u.displayName}>
      {children}
    </Shell>
  );
}
