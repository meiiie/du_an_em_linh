import { Shell } from "@/components/shell";
import { requireRole } from "@/lib/auth";

export default async function Layout({ children }: { children: React.ReactNode }) {
  const u = await requireRole("GV");
  return (
    <Shell role="GV" name={u.displayName}>
      {children}
    </Shell>
  );
}
