import { Route, Routes } from '@angular/router';
import { chiVaiTro, chuaDangNhap } from './core/auth/vai-tro.guard';

// Đường dẫn, tiêu đề tab và heading theo bảng phụ lục của `specs/001-lat-cat-doc/spec.md` (đối chiếu v0
// `apps/web/lib/nav.ts`) để e2e đối chiếu được. Tab ngắn như docs/DESIGN.md: `Học` / `Đề bài` / `Lịch` / `Công thức`…
// `/` là trang công khai như v0, lập chỉ mục được (không `robots`).
const KHONG_LAP_CHI_MUC = { robots: 'noindex, nofollow' };

const trangCho = () => import('./shared/layout/trang-cho').then((m) => m.TrangCho);
const khung = () => import('./shared/layout/khung-trang').then((m) => m.KhungTrang);

/** Trang con chưa có màn thật: `TrangCho` với heading và câu mô tả của màn đó. */
function cho(path: string, title: string, tieuDe: string, moTa: string): Route {
  return { path, title, data: { tieuDe, moTa }, loadComponent: trangCho };
}

export const routes: Routes = [
  {
    path: 'dang-nhap',
    title: 'Đăng nhập',
    data: KHONG_LAP_CHI_MUC,
    canActivate: [chuaDangNhap],
    loadComponent: () => import('./features/dang-nhap/dang-nhap').then((m) => m.DangNhap),
  },
  {
    path: 'hs',
    data: { ...KHONG_LAP_CHI_MUC, khuVuc: 'HS' },
    canActivate: [chiVaiTro('STUDENT')],
    loadComponent: khung,
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Học',
        loadComponent: () => import('./features/hoc-sinh/trang-hoc-sinh').then((m) => m.TrangHocSinh),
      },
      { path: 'bai', title: 'Đề bài', loadComponent: () => import('./features/hoc-sinh/de-bai').then((m) => m.DeBai) },
      { path: 'luyen/:maBai', title: 'Luyện', loadComponent: () => import('./features/hoc-sinh/luyen/luyen').then((m) => m.Luyen) },
      cho('lich', 'Lịch', 'Lịch học', 'Lịch học trong tuần và việc hôm nay sẽ hiện ở đây.'),
      cho('kho', 'Công thức', 'Công thức và tài liệu', 'Bảng công thức và tài liệu của lớp sẽ hiện ở đây.'),
    ],
  },
  {
    path: 'gv',
    data: { ...KHONG_LAP_CHI_MUC, khuVuc: 'GV' },
    canActivate: [chiVaiTro('TEACHER', 'SCHOOL_ADMIN', 'ADMIN')],
    loadComponent: khung,
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Lớp',
        loadComponent: () => import('./features/giao-vien/trang-giao-vien').then((m) => m.TrangGiaoVien),
      },
      cho('duyet', 'Duyệt', 'Duyệt', 'Bài và lời gia sư chờ duyệt sẽ hiện ở đây.'),
      cho('ngan-hang', 'Đề bài', 'Đề bài', 'Đề bài của lớp sẽ hiện ở đây.'),
      cho('tai-lieu', 'Tài liệu', 'Tài liệu', 'Tài liệu của lớp và quyền dùng của từng tài liệu sẽ hiện ở đây.'),
      cho('cong-thuc', 'Công thức', 'Công thức', 'Bảng công thức của lớp và kết quả kiểm từng dòng sẽ hiện ở đây.'),
      cho('tien-do', 'Mức', 'Mức lớp', 'Mức của từng học sinh theo từng kỹ năng sẽ hiện ở đây.'),
      cho('hoc-sinh/:id', 'Học sinh', 'Học sinh', 'Bài đã nộp, lỗi từng bước và các lượt gia sư của học sinh sẽ hiện ở đây.'),
      cho('cai-dat', 'Cài lớp', 'Cài đặt lớp', 'Cách mở lời giải và gia sư của lớp sẽ hiện ở đây.'),
      // Ngoại lệ FR-033: v0 «Kết nối ChatGPT» (dán khóa); v2 hiện trạng thái nhà do máy chủ quản lý (research R3).
      cho('ket-noi-ai', 'Gia sư', 'Gia sư', 'Gia sư lớp đang dùng sẽ hiện ở đây.'),
    ],
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Học toán theo từng bước',
    loadComponent: () => import('./features/trang-chu/trang-chu').then((m) => m.TrangChu),
  },
  { path: '**', redirectTo: 'dang-nhap' },
];
