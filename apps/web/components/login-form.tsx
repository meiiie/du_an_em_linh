"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { useFormStatus } from "react-dom";
import { Eye, EyeOff, Pencil } from "lucide-react";
import { BrandMark } from "@/components/brand-mark";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/cn";

const TAI = [
  { nhan: "Học sinh An", email: "hs.an@demo.local" },
  { nhan: "Giáo viên", email: "gv@demo.local" },
] as const;

const oNhap =
  "w-full min-h-12 rounded-button border border-line bg-canvas px-4 text-[15px] leading-6 text-ink outline-none transition-[border-color,box-shadow] duration-150 hover:border-muted focus-visible:border-ink focus-visible:ring-2 focus-visible:ring-ink/15 disabled:cursor-not-allowed disabled:opacity-50 read-only:bg-wash read-only:text-ink";

function NutVaoHoc() {
  const { pending } = useFormStatus();
  return (
    <Button className="min-h-12 w-full font-semibold" type="submit" disabled={pending}>
      {pending ? "Đang vào lớp…" : "Vào học"}
    </Button>
  );
}

export function LoginForm({
  loi,
  dangNhap,
}: {
  loi: boolean;
  dangNhap: (formData: FormData) => void | Promise<void>;
}) {
  const [buoc, setBuoc] = useState<"email" | "mat-khau">("email");
  const [email, setEmail] = useState("");
  const [hienMatKhau, setHienMatKhau] = useState(false);
  const emailRef = useRef<HTMLInputElement>(null);
  const matKhauRef = useRef<HTMLInputElement>(null);
  const lanDau = useRef(true);

  useEffect(() => {
    if (typeof sessionStorage === "undefined") return;
    const nho = sessionStorage.getItem("dn-email") || "";
    if (nho) {
      setEmail(nho);
      if (loi) setBuoc("mat-khau");
    }
  }, [loi]);

  useEffect(() => {
    const o = buoc === "email" ? emailRef.current : matKhauRef.current;
    if (!o) return;
    if (lanDau.current) {
      lanDau.current = false;
      if (!window.matchMedia("(pointer: fine)").matches) return;
    }
    o.focus();
  }, [buoc]);

  function luuEmail(v: string) {
    setEmail(v);
    try {
      sessionStorage.setItem("dn-email", v);
    } catch {
      /* chế độ riêng tư */
    }
  }

  function sangMatKhau(e?: FormEvent) {
    e?.preventDefault();
    const v = email.trim().toLowerCase();
    if (!v) {
      emailRef.current?.focus();
      return;
    }
    luuEmail(v);
    setHienMatKhau(false);
    setBuoc("mat-khau");
  }

  return (
    <section
      className="mx-auto flex w-full max-w-[448px] flex-col items-center place-self-center py-8 text-center motion-safe:animate-[login-vao_180ms_ease-out]"
      aria-labelledby="tieu-de-dang-nhap"
    >
      <p className="sr-only" aria-live="polite">
        Bước {buoc === "email" ? "1" : "2"} / 2
      </p>
      <BrandMark size="lg" />
      <h1 id="tieu-de-dang-nhap" className="mt-5 text-pretty text-[1.75rem] font-semibold leading-9 tracking-tight">
        Đăng nhập
      </h1>
      <p className="mt-2 min-h-[22px] text-sm leading-relaxed text-muted">
        {buoc === "email"
          ? "Nhập email tài khoản thử để vào Học toán với AI."
          : "Xác nhận mật khẩu để vào phiếu học."}
      </p>

      {loi ? (
        <p
          id="loi-dang-nhap"
          className="mt-6 w-full rounded-button bg-wash px-3 py-3 text-left text-[13px] leading-5 text-mark"
          role="alert"
        >
          Email hoặc mật khẩu chưa đúng. Thử lại — mật khẩu thử nằm dưới form.
        </p>
      ) : null}

      {buoc === "email" ? (
        <form id="form-dang-nhap" className="mt-8 grid w-full gap-5 text-left" onSubmit={sangMatKhau}>
          <label className="block min-w-0 text-[13px] font-semibold leading-5">
            <span className="mb-2 block">Email</span>
            <input
              ref={emailRef}
              name="email"
              type="email"
              autoComplete="username"
              enterKeyHint="next"
              maxLength={120}
              spellCheck={false}
              autoCapitalize="none"
              autoCorrect="off"
              required
              data-testid="email"
              className={oNhap}
              placeholder="hs.an@demo.local"
              value={email}
              onChange={(e) => luuEmail(e.target.value)}
            />
          </label>
          <Button className="min-h-12 w-full font-semibold" type="submit" disabled={!email.trim()}>
            Tiếp tục
          </Button>
        </form>
      ) : (
        <form id="form-dang-nhap" action={dangNhap} className="mt-8 grid w-full gap-5 text-left">
          <label className="block min-w-0 text-[13px] font-semibold leading-5">
            <span className="mb-2 block">Email</span>
            <div className="relative">
              <input
                name="email"
                type="email"
                autoComplete="username"
                readOnly
                data-testid="email"
                className={cn(oNhap, "pr-14")}
                value={email}
              />
              <button
                type="button"
                className="absolute inset-y-0.5 right-0.5 grid w-12 place-items-center rounded-[5px] text-muted hover:bg-wash hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink"
                aria-label="Sửa email"
                title="Sửa email"
                onClick={() => setBuoc("email")}
              >
                <Pencil size={18} aria-hidden />
              </button>
            </div>
          </label>
          <label className="block min-w-0 text-[13px] font-semibold leading-5">
            <span className="mb-2 block">Mật khẩu</span>
            <div className="relative">
              <input
                ref={matKhauRef}
                id="o-mat-khau"
                name="password"
                type={hienMatKhau ? "text" : "password"}
                autoComplete="current-password"
                enterKeyHint="go"
                maxLength={72}
                required
                data-testid="password"
                className={cn(oNhap, "pr-14")}
                placeholder="Nhập mật khẩu"
                aria-invalid={loi || undefined}
                aria-describedby={loi ? "loi-dang-nhap" : undefined}
              />
              <button
                type="button"
                className="absolute inset-y-0.5 right-0.5 grid w-12 place-items-center rounded-[5px] text-muted hover:bg-wash hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ink"
                aria-pressed={hienMatKhau}
                aria-label={hienMatKhau ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                title={hienMatKhau ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                aria-controls="o-mat-khau"
                onClick={() => setHienMatKhau((v) => !v)}
              >
                {hienMatKhau ? <EyeOff size={18} aria-hidden /> : <Eye size={18} aria-hidden />}
              </button>
            </div>
          </label>
          <div className="grid gap-3">
            <NutVaoHoc />
            <Button className="min-h-12 w-full font-semibold" variant="secondary" type="button" onClick={() => setBuoc("email")}>
              Quay lại
            </Button>
          </div>
        </form>
      )}

      <ul className="mt-8 flex w-full flex-wrap justify-center gap-2" aria-label="Tài khoản thử">
        {TAI.map((t) => (
          <li key={t.email}>
            <Button
              variant="secondary"
              className="text-xs font-medium"
              onClick={() => {
                luuEmail(t.email);
                setBuoc("mat-khau");
              }}
            >
              {t.nhan}
            </Button>
          </li>
        ))}
      </ul>
      <p className="mt-4 text-xs leading-[18px] text-muted">
        Mật khẩu thử: học sinh <span className="font-mono">hocsinh123</span> · giáo viên{" "}
        <span className="font-mono">giaovien123</span>
      </p>
    </section>
  );
}
