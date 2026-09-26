"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import {
  BookOpen,
  CalendarDays,
  FileText,
  Home,
  Inbox,
  LayoutDashboard,
  Library,
  Menu,
  Settings,
  Sigma,
  Sparkles,
  X,
} from "lucide-react";
import { BrandMark } from "@/components/brand-mark";
import { dangXuat } from "@/lib/actions/auth";
import { cn } from "@/lib/cn";
import { navActive, type NavItem } from "@/lib/nav";

const ICONS = {
  home: Home,
  book: BookOpen,
  calendar: CalendarDays,
  inbox: Inbox,
  bank: Library,
  spark: Sparkles,
  file: FileText,
  sigma: Sigma,
  chart: LayoutDashboard,
  settings: Settings,
};

export function AppShell({
  role,
  name,
  items,
  badges,
  children,
}: {
  role: "HS" | "GV";
  name: string;
  items: NavItem[];
  badges?: Record<string, number>;
  children: ReactNode;
}) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const title = role === "GV" ? "Cổng giáo viên" : "Phần học sinh";

  return (
    <div className="min-h-screen bg-paper">
      <a href="#noi-dung" className="skip-link">
        Bỏ qua đến nội dung
      </a>

      <header className="sticky top-0 z-30 border-b border-line bg-canvas pt-[env(safe-area-inset-top)]">
        <div className="flex h-14 items-center gap-3 px-3 sm:px-4">
          <button
            type="button"
            data-testid="mo-sidebar"
            className="rounded-button p-1.5 text-ink hover:bg-paper lg:hidden"
            onClick={() => setOpen(true)}
            aria-label="Mở menu"
          >
            <Menu className="h-5 w-5" aria-hidden />
          </button>
          <Link href={role === "GV" ? "/gv" : "/hs"} className="flex min-w-0 items-center gap-2">
            <BrandMark />
            <span className="min-w-0">
              <span className="block truncate text-sm font-semibold text-ink">Học toán với AI</span>
              <span className="hidden text-xs text-muted sm:block">{title}</span>
            </span>
          </Link>
          <div className="ml-auto flex min-w-0 items-center gap-3">
            <p className="hidden truncate text-sm text-ink sm:block">{name}</p>
            <form action={dangXuat}>
              <button
                className="rounded-button px-2 py-1 text-sm font-medium text-primary hover:bg-paper"
                type="submit"
              >
                Thoát
              </button>
            </form>
          </div>
        </div>
      </header>

      {open ? (
        <button
          className="fixed inset-0 z-30 bg-navy/40 lg:hidden"
          aria-label="Đóng menu"
          onClick={() => setOpen(false)}
        />
      ) : null}

      <aside
        data-testid="sidebar"
        className={cn(
          "fixed bottom-0 left-0 z-40 flex w-64 flex-col border-r border-line bg-canvas transition-transform duration-200",
          "top-14 overscroll-contain",
          open ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
        )}
      >
        <button
          className="absolute right-3 top-3 text-muted hover:text-ink lg:hidden"
          onClick={() => setOpen(false)}
          aria-label="Đóng"
          data-testid="dong-sidebar"
          type="button"
        >
          <X className="h-5 w-5" aria-hidden />
        </button>
        <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 pb-3 pt-4" data-testid="sidebar-nav">
          {items.map((item) => {
            const Icon = ICONS[item.icon];
            const active = navActive(pathname, item.href);
            const count = badges?.[item.href] || 0;
            return (
              <Link
                key={item.href}
                href={item.href}
                data-testid={item.testId}
                onClick={() => setOpen(false)}
                className={cn(
                  "flex items-center gap-3 rounded-button px-3 py-2 text-sm transition-colors",
                  active
                    ? "bg-primary/10 font-semibold text-primary"
                    : "text-ink/80 hover:bg-paper hover:text-ink",
                )}
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden />
                <span className="min-w-0 flex-1 truncate">{item.label}</span>
                {count > 0 ? (
                  <span
                    className={cn(
                      "tabular rounded-md px-1.5 py-0.5 text-[11px] font-semibold",
                      active ? "bg-primary text-white" : "bg-paper text-muted",
                    )}
                  >
                    {count}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto border-t border-line px-4 py-4">
          <p className="truncate text-sm font-medium text-ink">{name}</p>
          <p className="text-xs text-muted">Tài khoản thử · dữ liệu tổng hợp</p>
        </div>
      </aside>

      <div className="lg:pl-64">
        <div id="noi-dung" className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          {children}
        </div>
      </div>
    </div>
  );
}
