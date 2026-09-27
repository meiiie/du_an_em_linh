import type { Metadata } from "next";
import Link from "next/link";
import { buttonClasses } from "@/components/ui/button";
import { SITE_NAME } from "@/lib/site";

export const metadata: Metadata = {
  title: "Không tìm thấy trang",
  robots: { index: false, follow: true },
};

export default function KhongTimThay() {
  return (
    <main className="mx-auto flex min-h-screen max-w-lg flex-col justify-center px-5">
      <p className="font-mono text-sm text-muted">404</p>
      <h1 className="mt-2 text-pretty text-3xl font-semibold">Không tìm thấy trang</h1>
      <p className="mt-3 text-sm leading-relaxed text-muted">
        Đường dẫn không có trong {SITE_NAME}. Về trang chủ hoặc vào lớp thử.
      </p>
      <div className="mt-8 flex flex-wrap gap-3">
        <Link href="/" className={buttonClasses()}>
          Trang chủ
        </Link>
        <Link href="/dang-nhap" className={buttonClasses({ variant: "secondary" })}>
          Vào học
        </Link>
      </div>
    </main>
  );
}
