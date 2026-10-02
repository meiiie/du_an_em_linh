import { Routes } from '@angular/router';
import { chiVaiTro, chuaDangNhap } from './core/auth/vai-tro.guard';

// Đường dẫn và tiêu đề tab giữ như v0 (`/dang-nhap`, `/hs` «Học», `/gv` «Lớp») để e2e đối chiếu được.
// `/` là trang công khai ở v0; v2 chưa có nên tạm chuyển về đăng nhập (đã đăng nhập thì guard chuyển tiếp về trang chủ).
const KHONG_LAP_CHI_MUC = { robots: 'noindex, nofollow' };

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
    title: 'Học',
    data: KHONG_LAP_CHI_MUC,
    canActivate: [chiVaiTro('STUDENT')],
    loadComponent: () => import('./features/hoc-sinh/trang-hoc-sinh').then((m) => m.TrangHocSinh),
  },
  {
    path: 'gv',
    title: 'Lớp',
    data: KHONG_LAP_CHI_MUC,
    canActivate: [chiVaiTro('TEACHER', 'SCHOOL_ADMIN', 'ADMIN')],
    loadComponent: () => import('./features/giao-vien/trang-giao-vien').then((m) => m.TrangGiaoVien),
  },
  { path: '', pathMatch: 'full', redirectTo: 'dang-nhap' },
  { path: '**', redirectTo: 'dang-nhap' },
];
