export type NavItem = {
  href: string;
  label: string;
  icon: "home" | "book" | "calendar" | "inbox" | "bank" | "spark" | "file" | "sigma" | "chart" | "settings" | "kho" | "plug";
  testId: string;
};

export const HS_NAV: NavItem[] = [
  { href: "/hs", label: "Học", icon: "home", testId: "nav-hs-lo-trinh" },
  { href: "/hs/bai", label: "Đề bài", icon: "book", testId: "nav-hs-bai" },
  { href: "/hs/lich", label: "Lịch", icon: "calendar", testId: "nav-hs-lich" },
  { href: "/hs/kho", label: "Công thức", icon: "kho", testId: "nav-hs-kho" },
];

export const GV_NAV: NavItem[] = [
  { href: "/gv", label: "Lớp", icon: "home", testId: "nav-gv-tong-quan" },
  { href: "/gv/duyet", label: "Duyệt", icon: "inbox", testId: "nav-gv-duyet" },
  { href: "/gv/ngan-hang", label: "Đề bài", icon: "bank", testId: "nav-gv-ngan-hang" },
  { href: "/gv/sinh-bai", label: "Tạo đề", icon: "spark", testId: "nav-gv-sinh-bai" },
  { href: "/gv/tai-lieu", label: "Tài liệu", icon: "file", testId: "nav-gv-tai-lieu" },
  { href: "/gv/cong-thuc", label: "Công thức", icon: "sigma", testId: "nav-gv-cong-thuc" },
  { href: "/gv/tien-do", label: "Mức", icon: "chart", testId: "nav-gv-tien-do" },
  { href: "/gv/ket-noi-ai", label: "Gia sư", icon: "plug", testId: "nav-gv-ket-noi-ai" },
  { href: "/gv/cai-dat", label: "Cài lớp", icon: "settings", testId: "nav-gv-cai-dat" },
];

export function navActive(pathname: string, href: string) {
  if (href === "/hs" || href === "/gv") return pathname === href;
  return pathname === href || pathname.startsWith(`${href}/`);
}
