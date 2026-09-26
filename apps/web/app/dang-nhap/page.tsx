import { eq } from "drizzle-orm";
import { redirect } from "next/navigation";
import { BrandMark } from "@/components/brand-mark";
import { createSession, verifyPassword } from "@/lib/auth";
import { db } from "@/lib/db";
import { userRoles, users } from "@/lib/db/schema";
import { fieldControl } from "@/components/ui/field";

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
    <main className="relative min-h-screen overflow-hidden bg-paper">
      <a href="#form-dang-nhap" className="skip-link">
        Bỏ qua đến form đăng nhập
      </a>
      <div className="pointer-events-none absolute inset-y-0 left-0 hidden w-[40%] bg-navy lg:block" />
      <div className="relative mx-auto grid min-h-screen max-w-6xl lg:grid-cols-[0.9fr_1.1fr]">
        <section className="hidden flex-col justify-between bg-navy px-10 py-12 text-chalk lg:flex">
          <div className="flex items-center gap-3">
            <BrandMark className="bg-primary" />
            <p className="text-sm font-medium text-chalk/70">Nguyên mẫu NCKH</p>
          </div>
          <div>
            <h1 className="text-pretty text-5xl font-semibold leading-[1.1]">Học toán với AI</h1>
            <p className="mt-4 max-w-sm text-base leading-relaxed text-chalk/75">
              Toán 12 — ứng dụng đạo hàm: tính đơn điệu và cực trị. Gia sư sửa bài và giảng, không đưa đáp án. Mọi bài qua cổng kiểm định ba tầng.
            </p>
          </div>
          <p className="text-sm text-chalk/50">Tài khoản thử. Không có học sinh thật.</p>
        </section>
        <section className="flex flex-col justify-center px-5 py-12 sm:px-10">
          <div className="mb-6 flex items-center gap-3 lg:hidden">
            <BrandMark />
            <div>
              <p className="text-sm font-medium text-primary">Nguyên mẫu NCKH</p>
              <h1 className="text-pretty text-3xl font-semibold">Học toán với AI</h1>
            </div>
          </div>
          <p className="text-sm text-muted lg:hidden">Toán 12 — đơn điệu và cực trị. Tài khoản thử.</p>
          <h2 className="hidden text-pretty text-3xl font-semibold lg:block">Vào lớp thử</h2>
          <form id="form-dang-nhap" action={dangNhap} className="mt-6 max-w-md space-y-3 rounded-card bg-canvas p-6 shadow-lift ring-1 ring-line">
            <label className="block text-sm font-medium">
              Email
              <input
                name="email"
                type="email"
                autoComplete="username"
                spellCheck={false}
                required
                data-testid="email"
                className={`mt-1 ${fieldControl}`}
                placeholder="hs.an@demo.local"
              />
            </label>
            <label className="block text-sm font-medium">
              Mật khẩu
              <input
                name="password"
                type="password"
                autoComplete="current-password"
                required
                data-testid="password"
                className={`mt-1 ${fieldControl}`}
              />
            </label>
            {sp.loi ? <p className="text-sm text-danger">Email hoặc mật khẩu chưa đúng. Thử lại với tài khoản bên dưới.</p> : null}
            <button
              className="w-full rounded-button bg-primary px-4 py-2.5 font-semibold text-white hover:bg-primary-hover"
              type="submit"
            >
              Vào học
            </button>
          </form>
          <ul className="mt-4 max-w-md space-y-1 text-xs text-muted">
            <li>Giáo viên: gv@demo.local / giaovien123</li>
            <li>Học sinh: hs.an@demo.local, hs.binh@demo.local, hs.chi@demo.local / hocsinh123</li>
          </ul>
        </section>
      </div>
    </main>
  );
}
