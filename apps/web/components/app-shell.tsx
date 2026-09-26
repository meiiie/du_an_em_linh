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
      {open ? (
        <button
          className="fixed inset-0 z-30 bg-ink/50 lg:hidden"
          aria-label="Đóng menu"
          onClick={() => setOpen(false)}
        />
      ) : null}

      <aside
        data-testid="sidebar"
        className={cn(
          "fixed inset-y-0 left-0 z-40 flex w-[272px] flex-col bg-board text-chalk transition-transform",
          open ? "translate-x-0" : "-translate-x-full lg:translate-x-0",
        )}
      >
        <button
          className="absolute right-3 top-3 text-chalk/70 lg:hidden"
          onClick={() => setOpen(false)}
          aria-label="Đóng"
          data-testid="dong-sidebar"
          type="button"
        >
          <X className="h-5 w-5" />
        </button>
        <div className="px-5 pb-4 pt-5">
          <p className="font-display text-lg leading-none text-chalk">Học toán với AI</p>
          <p className="mt-1 text-[11px] uppercase tracking-[0.16em] text-chalk/55">{title}</p>
        </div>
        <nav className="flex flex-1 flex-col gap-0.5 px-3" data-testid="sidebar-nav">
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
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition",
                  active ? "bg-chalk text-ink shadow-sm" : "text-chalk/80 hover:bg-white/5 hover:text-chalk",
                )}
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden />
                <span className="flex-1 font-medium">{item.label}</span>
                {count > 0 ? (
                  <span
                    className={cn(
                      "rounded-full px-1.5 py-0.5 text-[10px] font-semibold",
                      active ? "bg-ink text-paper" : "bg-clay text-white",
                    )}
                  >
                    {count}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto border-t border-white/10 px-4 py-4">
          <p className="truncate text-sm font-medium text-chalk">{name}</p>
          <p className="text-xs text-chalk/50">Tài khoản thử · dữ liệu tổng hợp</p>
          <form action={dangXuat} className="mt-3">
            <button className="text-xs text-chalk/70 underline-offset-2 hover:text-chalk hover:underline" type="submit">
              Thoát
            </button>
          </form>
        </div>
      </aside>

      <div className="lg:pl-[272px]">
        <header className="sticky top-0 z-20 flex items-center gap-3 border-b border-line/80 bg-paper/90 px-4 py-3 backdrop-blur lg:hidden">
          <button
            type="button"
            data-testid="mo-sidebar"
            className="rounded-lg p-1.5 ring-1 ring-line"
            onClick={() => setOpen(true)}
            aria-label="Mở menu"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div>
            <p className="font-display text-base leading-none">Học toán với AI</p>
            <p className="text-[11px] text-muted">{name}</p>
          </div>
        </header>
        <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">{children}</div>
      </div>
    </div>
  );
}
