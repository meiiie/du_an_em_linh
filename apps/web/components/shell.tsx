import Link from "next/link";
import { clearSession } from "@/lib/auth";

export function Shell({
  role,
  name,
  children,
}: {
  role: "HS" | "GV";
  name: string;
  children: React.ReactNode;
}) {
  const links =
    role === "HS"
      ? [
          ["/hs", "Lộ trình"],
          ["/hs/lich", "Thời gian biểu"],
        ]
      : [
          ["/gv", "Tổng quan"],
          ["/gv/tai-lieu", "Tài liệu"],
          ["/gv/cong-thuc", "Công thức"],
          ["/gv/ngan-hang", "Ngân hàng"],
          ["/gv/duyet", "Duyệt bài"],
          ["/gv/tien-do", "Tiến độ"],
          ["/gv/sinh-bai", "Sinh biến thể"],
        ];
  return (
    <div className="mx-auto min-h-screen max-w-6xl px-3 pb-16 pt-4 sm:px-6">
      <header className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className={`text-xs font-semibold uppercase tracking-wide ${role === "GV" ? "text-clay" : "text-navy"}`}>
            {role === "GV" ? "Cổng giáo viên" : "Phần học sinh"}
          </p>
          <p className="font-semibold">{name}</p>
        </div>
        <nav className="flex flex-wrap gap-1.5">
          {links.map(([href, label]) => (
            <Link key={href} href={href} className="rounded-full bg-white px-3 py-1.5 text-sm shadow-sm ring-1 ring-stone-200">
              {label}
            </Link>
          ))}
          <form
            action={async () => {
              "use server";
              await clearSession();
              const { redirect } = await import("next/navigation");
              redirect("/dang-nhap");
            }}
          >
            <button className="rounded-full px-3 py-1.5 text-sm text-slate-600" type="submit">
              Thoát
            </button>
          </form>
        </nav>
      </header>
      {children}
    </div>
  );
}
