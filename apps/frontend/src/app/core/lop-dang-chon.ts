import { effect, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { HangChoDuyet } from './hang-cho';
import { Phien } from './auth/phien';
import { ApiLopHoc } from '../api/lop-hoc';
import { Item } from '../api/ux';

@Injectable({ providedIn: 'root' })
export class LopDangChon {
 readonly ds = signal<readonly Item[]>([]);
 readonly id = signal('');
 readonly loi = signal(false);
 readonly dangTai = signal(false);
 readonly soChoDuyet = inject(HangChoDuyet).so;
 private readonly api = inject(ApiLopHoc);
 private readonly phien = inject(Phien);
 private nguoiId: string | null = null;
 private lanPhien = 0;
 private daTai = false;

 constructor() {
  effect(() => this.dongBoPhien(this.phien.nguoiDung()?.id ?? null));
 }

 private dongBoPhien(id: string | null) {
  if (id === this.nguoiId) return;
  this.nguoiId = id;
  this.lanPhien++;
  this.daTai = false;
  this.ds.set([]);
  this.id.set('');
  this.loi.set(false);
  this.dangTai.set(false);
  this.soChoDuyet.set(0);
 }

 private dungPhien(lan: number) {
  // Kiểm tra cả khi effect chưa chạy sau thay đổi phiên.
  this.dongBoPhien(this.phien.nguoiDung()?.id ?? null);
  return lan === this.lanPhien;
 }

 async tai() {
  this.dongBoPhien(this.phien.nguoiDung()?.id ?? null);
  if (!this.nguoiId || this.daTai || this.dangTai()) return;
  const lan = this.lanPhien;
  this.dangTai.set(true);
  this.loi.set(false);
  try {
   const ds = await firstValueFrom(this.api.cacLop());
   if (!this.dungPhien(lan)) return;
   this.ds.set(ds);
   this.id.set(ds[0]?.id ?? '');
   this.daTai = true;
   if (ds[0]) {
    try {
     const q = await firstValueFrom(this.api.tongQuan(ds[0].id));
     if (this.dungPhien(lan)) this.soChoDuyet.set(q.pending);
    } catch { /* Huy hiệu là tùy chọn; trang có trạng thái thử lại riêng. */ }
   }
  } catch {
   if (this.dungPhien(lan)) this.loi.set(true);
  } finally {
   if (this.dungPhien(lan)) this.dangTai.set(false);
  }
 }
}
