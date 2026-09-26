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

const STEPS = ["Tập xác định", "Đạo hàm", "Nghiệm y′", "Xét dấu", "Kết luận"];

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ loi?: string }> }) {
  const sp = await searchParams;
  return (
    <main className="min-h-screen bg-canvas">
      <a href="#form-dang-nhap" className="skip-link">
        Bỏ qua đến form đăng nhập
      </a>
      <div className="mx-auto grid min-h-screen max-w-5xl lg:grid-cols-2">
        <section className="hidden flex-col justify-between border-r border-line px-10 py-12 lg:flex">
          <div className="flex items-center gap-2">
            <BrandMark />
            <p className="text-sm text-muted">Nguyên mẫu NCKH</p>
          </div>
          <div>
            <h1 className="text-pretty text-4xl font-semibold leading-tight">Học toán với AI</h1>
            <p className="mt-3 max-w-sm text-sm leading-relaxed text-muted">
              Phiếu 5 bước cho đơn điệu và cực trị. Gia sư sửa bài, không đưa đáp án. Mọi bài qua cổng ba tầng.
            </p>
            <ol className="mt-8 space-y-0">
              {STEPS.map((ten, i) => (
                <li key={ten} className="flex items-center gap-3 border-b border-line py-2.5 text-sm last:border-0">
                  <span className="tabular w-6 font-mono text-muted">{i + 1}</span>
                  {ten}
                </li>
              ))}
            </ol>
          </div>
          <p className="text-sm text-muted">Tài khoản thử. Không có học sinh thật.</p>
        </section>
        <section className="flex flex-col justify-center px-5 py-12 sm:px-10">
          <div className="mb-6 flex items-center gap-2 lg:hidden">
            <BrandMark />
            <div>
              <p className="text-sm text-muted">Nguyên mẫu NCKH</p>
              <h1 className="text-pretty text-2xl font-semibold">Học toán với AI</h1>
            </div>
          </div>
          <p className="text-sm text-muted lg:hidden">Toán 12 — đơn điệu và cực trị. Tài khoản thử.</p>
          <h2 className="hidden text-pretty text-2xl font-semibold lg:block">Vào lớp thử</h2>
          <form id="form-dang-nhap" action={dangNhap} className="mt-6 max-w-md space-y-3">
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
            {sp.loi ? <p className="text-sm text-mark">Email hoặc mật khẩu chưa đúng. Thử lại với tài khoản bên dưới.</p> : null}
            <button className="w-full rounded-button bg-ink px-4 py-2.5 text-sm font-medium text-chalk hover:bg-primary-hover" type="submit">
              Vào học
            </button>
          </form>
          <ul className="mt-6 max-w-md space-y-1 text-xs text-muted">
            <li>Giáo viên: gv@demo.local / giaovien123</li>
            <li>Học sinh: hs.an@demo.local, hs.binh@demo.local, hs.chi@demo.local / hocsinh123</li>
          </ul>
        </section>
      </div>
    </main>
  );
}
