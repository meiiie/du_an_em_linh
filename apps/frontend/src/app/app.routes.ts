import { Route, Routes } from '@angular/router';
import { chiVaiTro, chuaDangNhap } from './core/auth/vai-tro.guard';

// Đường dẫn, tiêu đề tab và heading theo bảng phụ lục của `specs/001-lat-cat-doc/spec.md` (đối chiếu v0
// `apps/web/lib/nav.ts`) để e2e đối chiếu được. Tab ngắn như docs/DESIGN.md: `Học` / `Đề bài` / `Lịch` / `Công thức`…
// `/` là trang công khai như v0, lập chỉ mục được (không `robots`).
const KHONG_LAP_CHI_MUC = { robots: 'noindex, nofollow' };

const khung = () => import('./shared/layout/khung-trang').then(m => m.KhungTrang);
const phong = () => import('./features/giao-vien/phong-giao-vien').then(m => m.PhongGiaoVien);
const quanTri = () => import('./features/quan-tri/quan-tri').then(m => m.QuanTri);
function gv(path:string,title:string,trang:string):Route{return {path,title,data:{trang},loadComponent:phong};}

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
      {path:'lich',title:'Lịch',loadComponent:()=>import('./features/lich/lich').then(m=>m.Lich)},
      {path:'kho',title:'Công thức',loadComponent:()=>import('./features/kho/kho').then(m=>m.Kho)},
    ],
  },
  {
    path: 'gv',
    data: { ...KHONG_LAP_CHI_MUC, khuVuc: 'GV' },
    canActivate: [chiVaiTro('TEACHER', 'SCHOOL_ADMIN', 'ADMIN')],
    loadComponent: khung,
    children: [
      gv('', 'Tổng quan', 'tong-quan'),
      gv('duyet', 'Duyệt', 'duyet'),
      gv('ngan-hang', 'Đề bài', 'ngan-hang'),
      gv('tai-lieu', 'Tài liệu', 'tai-lieu'),
      gv('cong-thuc', 'Công thức', 'cong-thuc'),
      gv('tien-do', 'Mức', 'tien-do'),
      gv('hoc-sinh/:id', 'Học sinh', 'hoc-sinh'),
      gv('giao', 'Giao bài', 'giao'),
      gv('cai-dat', 'Cài lớp', 'cai-dat'),
      gv('ket-noi-ai', 'Gia sư', 'ket-noi-ai'),
    ],
  },
  {
    path:'qt',data:{...KHONG_LAP_CHI_MUC,khuVuc:'QT'},canActivate:[chiVaiTro('ADMIN','SCHOOL_ADMIN')],loadComponent:khung,
    children:[
      {path:'',title:'Người dùng',data:{trang:'nguoi-dung'},loadComponent:quanTri},
      {path:'lop',title:'Lớp học',data:{trang:'lop'},loadComponent:quanTri},
      {path:'nhat-ky',title:'Nhật ký',data:{trang:'nhat-ky'},loadComponent:quanTri},
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
