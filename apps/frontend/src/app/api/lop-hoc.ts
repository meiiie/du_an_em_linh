import { HttpClient, httpResource } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Muc4 } from './hoc-sinh';
import { BankDetails, Dashboard, Item, Library, ModeDetails, RecommendationDetails, Result, ScheduleDetails, Settings, StudentDetails, StudentProfile, UserDetails } from './ux';

export type CheDoHoc = 'ON_TAP' | 'KIEM_TRA';
export type TrangQuanTri = 'nguoi-dung' | 'lop' | 'nhat-ky';
export interface GiaoBoBai {
  lopId: string;
  studentIds: readonly string[];
  problemIds: readonly string[];
  mode: CheDoHoc;
  dueAt: string | null;
  setName: string;
}
export interface BuoiHoc { title: string; startsAt: string; endsAt: string; }
export interface TepThe { filename: string; content: string; }
export interface NguoiDungMoi { displayName: string; email: string; password: string; role: string; synthetic: true; }

/** Hợp đồng U5–U14 trong docs/API_UX.md. Lỗi HTTP được giữ để màn phân biệt
 * dữ liệu rỗng với dịch vụ chưa sẵn sàng; không biến lỗi thành kết quả thành công. */
@Injectable({ providedIn: 'root' })
export class ApiLopHoc {
  private readonly http = inject(HttpClient);
  duongGv(path: string, lopId: string): string {
    return `/api/gv/${path}?lopId=${encodeURIComponent(lopId)}`;
  }
  cacLop() { return this.http.get<Item[]>('/api/gv/lop'); }
  tongQuan(lopId: string) { return this.http.get<Dashboard>(this.duongGv('tong-quan', lopId)); }
  trangTongQuan(lopId: () => string | undefined) {
    return httpResource<Dashboard>(() => { const id = lopId(); return id ? this.duongGv('tong-quan', id) : undefined; });
  }
  tienDo(lopId: () => string | undefined) {
    return httpResource<Item<StudentDetails>[]>(() => { const id = lopId(); return id ? this.duongGv('tien-do', id) : undefined; });
  }
  hocSinh(lopId: () => string, id: () => string | undefined) {
    return httpResource<StudentProfile>(() => { const ma = id(); return lopId() && ma ? this.duongGv(`hoc-sinh/${encodeURIComponent(ma)}`, lopId()) : undefined; });
  }
  nganHang(lopId: () => string | undefined) {
    return httpResource<Item<BankDetails>[]>(() => { const id = lopId(); return id ? this.duongGv('ngan-hang', id) : undefined; });
  }
  danhSachGv(trang: () => 'duyet' | 'giao' | 'cong-thuc' | undefined, lopId: () => string) {
    return httpResource<Item[]>(() => { const path = trang(); return path && lopId() ? this.duongGv(path, lopId()) : undefined; });
  }
  khoGv(lopId: () => string | undefined) {
    return httpResource<Library>(() => { const id = lopId(); return id ? this.duongGv('kho', id) : undefined; });
  }
  caiDat(lopId: () => string | undefined) {
    return httpResource<Settings>(() => { const id = lopId(); return id ? this.duongGv('cai-dat', id) : undefined; });
  }
  khoHocSinh() { return httpResource<Library>(() => '/api/hs/kho'); }
  cheDo() { return httpResource<Item<ModeDetails>[]>(() => '/api/hs/che-do'); }
  goiBai(bat: () => boolean = () => true) {
    return httpResource<Item<RecommendationDetails>[]>(() => bat() ? '/api/hs/goi-bai' : undefined);
  }
  lich() { return httpResource<Item<ScheduleDetails>[]>(() => '/api/hs/lich'); }
  nhac() { return httpResource<Item<ScheduleDetails>[]>(() => '/api/hs/nhac'); }
  quanTri(trang: () => TrangQuanTri) { return httpResource<Item[]>(() => `/api/qt/${trang()}`); }
  nguoiDung(bat: () => boolean) { return httpResource<Item<UserDetails>[]>(() => bat() ? '/api/qt/nguoi-dung' : undefined); }
  xuatThe() { return this.http.get<TepThe>('/api/hs/flashcards/export'); }
  themBuoi(body: BuoiHoc) { return this.http.post<Result>('/api/hs/lich', body); }
  xongBuoi(id: string) { return this.http.post<Result>(`/api/hs/lich/${encodeURIComponent(id)}/xong`, {}); }
  giao(body: GiaoBoBai) { return this.http.post<Result>('/api/gv/giao', body); }
  ghiMuc(body: { lopId: string; studentId: string; skillCode: string; level4: Muc4; reason: string }) {
    return this.http.post<Result>('/api/gv/muc-hieu', body);
  }
  taoNguoi(body: NguoiDungMoi) { return this.http.post<Result>('/api/qt/nguoi-dung', body); }
}
