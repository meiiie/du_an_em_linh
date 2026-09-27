"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import {
  BookMarked,
  BookOpen,
  CalendarDays,
  FileText,
  Home,
  Inbox,
  LayoutDashboard,
  Library,
  Menu,
  Plug,
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
  kho: BookMarked,
  plug: Plug,
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
  const title = role === "GV" ? "Giáo viên" : "Học sinh";

  return (
    <div className="min-h-screen bg-canvas">
      <a href="#noi-dung" className="skip-link">
        Bỏ qua đến nội dung
      </a>

      <header className="sticky top-0 z-30 border-b border-line bg-canvas pt-[env(safe-area-inset-top)] lg:hidden">
        <div className="flex h-12 items-center gap-2 px-2">
          <button
            type="button"
            data-testid="mo-sidebar"
            className="inline-flex size-11 items-center justify-center rounded-button text-ink hover:bg-wash"
            onClick={() => setOpen(true)}
            aria-label="Mở menu"
          >
            <Menu className="h-5 w-5" aria-hidden />
          </button>
          <Link href={role === "GV" ? "/gv" : "/hs"} className="flex min-w-0 items-center gap-2">
            <BrandMark />
            <span className="truncate text-sm font-medium">Học toán với AI</span>
          </Link>
          <form action={dangXuat} className="ml-auto">
            <button className="inline-flex min-h-11 items-center px-3 text-sm text-muted hover:text-ink" type="submit">
              Thoát
            </button>
          </form>
        </div>
      </header>

      {open ? (
        <button
          className="fixed inset-0 z-30 bg-ink/40 lg:hidden"
          aria-label="Đóng menu"
          onClick={() => setOpen(false)}
        />
      ) : null}

      <aside
        data-testid="sidebar"
        className={cn(
          "fixed bottom-0 left-0 z-40 flex w-[220px] flex-col bg-ink text-chalk transition-transform duration-200",
          "overscroll-contain lg:top-0",
          open ? "top-0 translate-x-0" : "top-12 -translate-x-full max-lg:invisible lg:translate-x-0",
        )}
      >
        <div className="flex h-12 items-center justify-end px-2 lg:hidden">
          <button
            className="inline-flex size-11 items-center justify-center text-chalk/60 hover:text-chalk"
            onClick={() => setOpen(false)}
            aria-label="Đóng"
            data-testid="dong-sidebar"
            type="button"
          >
            <X className="h-5 w-5" aria-hidden />
          </button>
        </div>
        <div className="hidden items-center gap-2 px-4 pb-4 pt-5 lg:flex">
          <BrandMark invert />
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">Học toán với AI</p>
            <p className="truncate text-xs text-chalk/55">{title}</p>
          </div>
        </div>
        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-2 pb-3 pt-4 lg:pt-1" data-testid="sidebar-nav">
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
                  "flex min-h-11 items-center gap-3 rounded-button px-3 text-sm transition-colors",
                  active ? "bg-white/10 font-medium text-chalk" : "text-chalk/70 hover:bg-white/5 hover:text-chalk",
                )}
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden />
                <span className="min-w-0 flex-1 truncate">{item.label}</span>
                {count > 0 ? (
                  <span className="tabular rounded px-2 py-0.5 text-[11px] font-medium bg-white/10 text-chalk">
                    {count}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto border-t border-white/10 px-4 py-4">
          <p className="truncate text-sm font-medium">{name}</p>
          <form action={dangXuat} className="mt-2 hidden lg:block">
            <button className="inline-flex min-h-11 items-center text-sm text-chalk/70 hover:text-chalk" type="submit">
              Thoát
            </button>
          </form>
        </div>
      </aside>

      <div className="lg:pl-[220px]">
        <div id="noi-dung" className="mx-auto max-w-5xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          {children}
        </div>
      </div>
    </div>
  );
}
