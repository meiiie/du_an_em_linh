export type NavItem = {
  href: string;
  label: string;
  icon: "home" | "book" | "calendar" | "inbox" | "bank" | "spark" | "file" | "sigma" | "chart" | "settings";
  testId: string;
};

export const HS_NAV: NavItem[] = [
  { href: "/hs", label: "Lộ trình", icon: "home", testId: "nav-hs-lo-trinh" },
  { href: "/hs/bai", label: "Ngân bài", icon: "book", testId: "nav-hs-bai" },
  { href: "/hs/lich", label: "Thời gian biểu", icon: "calendar", testId: "nav-hs-lich" },
];

export const GV_NAV: NavItem[] = [
  { href: "/gv", label: "Tổng quan", icon: "home", testId: "nav-gv-tong-quan" },
  { href: "/gv/duyet", label: "Hàng đợi", icon: "inbox", testId: "nav-gv-duyet" },
  { href: "/gv/ngan-hang", label: "Ngân hàng", icon: "bank", testId: "nav-gv-ngan-hang" },
  { href: "/gv/sinh-bai", label: "Sinh biến thể", icon: "spark", testId: "nav-gv-sinh-bai" },
  { href: "/gv/tai-lieu", label: "Tài liệu", icon: "file", testId: "nav-gv-tai-lieu" },
  { href: "/gv/cong-thuc", label: "Công thức", icon: "sigma", testId: "nav-gv-cong-thuc" },
  { href: "/gv/tien-do", label: "Tiến độ", icon: "chart", testId: "nav-gv-tien-do" },
  { href: "/gv/cai-dat", label: "Cài đặt lớp", icon: "settings", testId: "nav-gv-cai-dat" },
];

export function navActive(pathname: string, href: string) {
  if (href === "/hs" || href === "/gv") return pathname === href;
  return pathname === href || pathname.startsWith(`${href}/`);
}
