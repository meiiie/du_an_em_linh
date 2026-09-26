import { eq } from "drizzle-orm";
import { redirect } from "next/navigation";
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

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ loi?: string }> }) {
  const sp = await searchParams;
  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-4">
      <p className="text-sm font-semibold uppercase tracking-wide text-teal">Nguyên mẫu NCKH</p>
      <h1 className="mt-1 text-3xl font-bold">Học toán với AI</h1>
      <p className="mt-2 text-sm text-slate-600">
        Toán 12 — ứng dụng đạo hàm: tính đơn điệu và cực trị. Tài khoản thử, không có học sinh thật.
      </p>
      <form action={dangNhap} className="mt-6 space-y-3 rounded-2xl border border-stone-300 bg-white p-5 shadow-sm">
        <label className="block text-sm font-medium">
          Email
          <input
            name="email"
            type="email"
            required
            data-testid="email"
            className="mt-1 w-full rounded-xl border border-stone-300 px-3 py-2"
            placeholder="hs.an@demo.local"
          />
        </label>
        <label className="block text-sm font-medium">
          Mật khẩu
          <input
            name="password"
            type="password"
            required
            data-testid="password"
            className="mt-1 w-full rounded-xl border border-stone-300 px-3 py-2"
          />
        </label>
        {sp.loi ? <p className="text-sm text-red-700">Email hoặc mật khẩu chưa đúng.</p> : null}
        <button className="w-full rounded-xl bg-navy px-4 py-2.5 font-semibold text-white" type="submit">
          Vào học
        </button>
      </form>
      <ul className="mt-4 space-y-1 text-xs text-slate-600">
        <li>Giáo viên: gv@demo.local / giaovien123</li>
        <li>Học sinh: hs.an@demo.local, hs.binh@demo.local, hs.chi@demo.local / hocsinh123</li>
      </ul>
    </main>
  );
}
