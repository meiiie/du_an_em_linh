import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { redirect } from "next/navigation";
import { LoginForm } from "@/components/login-form";
import { createSession, verifyPassword } from "@/lib/auth";
import { db } from "@/lib/db";
import { userRoles, users } from "@/lib/db/schema";

async function dangNhap(formData: FormData) {
  "use server";
  const email = String(formData.get("email") || "").trim().toLowerCase();
  const password = String(formData.get("password") || "");
  const found = await db.select().from(users).where(eq(users.email, email)).limit(1);
  if (!found[0] || !verifyPassword(password, found[0].passwordHash)) {
    redirect("/dang-nhap?loi=1");
  }
  const roles = await db.select().from(userRoles).where(eq(userRoles.userId, found[0].id));
  await createSession(found[0].id);
  redirect(roles.some((r) => r.roleCode === "GV") ? "/gv" : "/hs");
}

export const metadata: Metadata = {
  title: "Đăng nhập",
  robots: { index: false, follow: false },
};

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ loi?: string }> }) {
  const sp = await searchParams;
  return (
    <main className="min-h-dvh bg-canvas pb-[env(safe-area-inset-bottom)]">
      <a href="#form-dang-nhap" className="skip-link">
        Bỏ qua đến form đăng nhập
      </a>
      <div className="mx-auto flex min-h-dvh max-w-5xl flex-col px-6 py-6 sm:px-8 sm:py-8">
        {/* Tâm quang học ~46% đỉnh trang (không phải giữa hình học 50%): dư dưới : dư trên = 3:2 */}
        <div className="flex min-h-0 flex-1 flex-col">
          <div className="min-h-0 flex-[2_1_0]" aria-hidden />
          <LoginForm loi={sp.loi === "1"} dangNhap={dangNhap} />
          <div className="min-h-0 flex-[3_1_0]" aria-hidden />
        </div>
        <footer className="flex flex-wrap items-center justify-center gap-2 text-center text-xs leading-[18px] text-muted">
          <span>Toán 12 · đơn điệu và cực trị</span>
          <span aria-hidden>·</span>
          <span>Tài khoản thử — không có học sinh thật</span>
        </footer>
      </div>
    </main>
  );
}
