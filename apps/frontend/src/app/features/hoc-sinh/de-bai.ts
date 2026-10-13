import { DinhDangSo, ngayViet } from '../../shared/ui/dinh-dang';
import { ApiLopHoc } from '../../api/lop-hoc';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { API_HS, BaiCuaHocSinh } from '../../api/hoc-sinh';
import { KhungCho, LoiTai, TrangRong } from '../../shared/ui/trang-thai';
import { Button } from '../../shared/ui/button';
import { HangBai } from './hang-bai';
import { nhomBai, locTrangThai } from './nhom-bai';

/** `/hs/bai`: mọi bài được giao (`GET /api/hs/bai`), nhóm theo bộ bài/chủ đề; mỗi hàng dẫn tới phiếu làm bài. */
@Component({
  selector: 'app-de-bai',
  imports: [KhungCho, LoiTai, TrangRong, Button, HangBai, DinhDangSo],
  template: `
    <header class="dau-trang"><h1>Đề bài</h1><p class="muted">Tìm bài được giao và lọc theo trạng thái để luyện tập.</p></header><div class="toolbar"><label>Tìm đề bài<input type="search" [value]="tim()" (input)="tim.set($any($event.target).value)" placeholder="Kỹ năng, mã bài, đề bài" /></label></div>
    <div class="toolbar" role="group" aria-label="Lọc bài">@for(m of locs;track m.id){<button appButton variant="secondary" type="button" [attr.aria-pressed]="loc()===m.id" (click)="loc.set(m.id)">{{m.ten}}</button>}</div>
    <div class="toolbar" role="group" aria-label="Lọc trạng thái">@for(m of trangThais;track m.id){<button appButton variant="secondary" type="button" [attr.aria-pressed]="trangThai()===m.id" (click)="trangThai.set(m.id)">{{m.ten}}</button>}</div>
    @if(cheDo.error() && loc()!=='giao'){<app-loi-tai [loi]="cheDo.error()" (thuLai)="cheDo.reload()" />}
    @if (ds.isLoading() && !ds.hasValue()) {
      <app-khung-cho nhan="Đang tải đề bài" />
    } @else if (ds.error()) {
      <app-loi-tai [loi]="ds.error()" (thuLai)="ds.reload()" />
    } @else if (!nhom().length) {
      <app-trang-rong tieuDe="Chưa có bài phù hợp" moTa="Đổi từ khóa hoặc bộ lọc để tìm lại. Bài mới thầy cô giao sẽ hiện ở đây." />
    } @else {
      @for (n of nhom(); track n.id; let dau = $first) {
        <details class="nhom-chu-de" [open]="dau">
          <summary><span>{{ n.ten }}</span><span class="list-summary">{{n.hang.length | soViet}} bài · {{n.daNop | soViet}}/{{n.tong | soViet}} đã nộp · Hạn: {{ngay(n.han)}}</span></summary>
          <ul class="danh-sach">
            @for (h of n.hang; track h.bai.maBai) {
              <li><app-hang-bai [bai]="h.bai" [so]="h.so" [kiemTra]="laKiemTra(h.bai.maBai)" /></li>
            }
          </ul>
        </details>
      }
    }
  `,
  styles: `
    summary { min-height: 44px; padding: 8px 0; cursor: pointer; font-weight: 600; }
    summary .list-summary { margin-inline-start: 16px; font-weight: 400; }
    .nhom-chu-de { min-width: 0; }

    :host {
      display: grid;
      gap: var(--space-6);
    }
    h1 {
      margin: 0;
    }
    .dang-tai {
      height: 220px;
      border-radius: var(--radius-card);
      background: var(--wash);
    }
    .trong .btn {
      margin-top: var(--space-3);
    }
    .danh-sach {
      margin: 0;
      padding: 0;
      overflow: hidden;
      list-style: none;
      border: 1px solid var(--line);
      border-radius: var(--radius-card);
      background: var(--raise);
    }
    .danh-sach li + li {
      border-top: 1px solid var(--line);
    }
  `,
})
export class DeBai {
 private readonly api=inject(ApiLopHoc);

  protected readonly ngay = ngayViet;
  protected readonly ds = httpResource<BaiCuaHocSinh[]>(() => API_HS.bai);
  protected readonly cheDo=this.api.cheDo();
  protected readonly laKiemTra = (ma: string) => this.cheDo.hasValue() && this.cheDo.value().find(m => m.id === ma)?.status === 'KIEM_TRA';
  protected readonly loc=signal('giao');
  protected readonly tim=signal('');
  protected readonly trangThai=signal('tat-ca');
  protected readonly trangThais=[{id:'tat-ca',ten:'Tất cả'},{id:'chua-lam',ten:'Chưa làm'},{id:'chua-dat',ten:'Chưa đạt'},{id:'dat',ten:'Đạt'}];
  protected readonly locs=[{id:'giao',ten:'Được giao'},{id:'on',ten:'Ôn tập'},{id:'kiem',ten:'Kiểm tra'}];
  protected readonly nhom = computed(() => {
    const modes=this.cheDo.hasValue()?this.cheDo.value():[];
    const q=this.tim().trim().toLocaleLowerCase('vi');
    return nhomBai(this.ds.value() ?? []).map(n => ({
      ...n, tong: n.hang.length,
      hang: n.hang.filter(({bai:b}) =>
        (b.maBai+' '+b.tenKyNang+' '+b.deBai+' '+n.ten).toLocaleLowerCase('vi').includes(q)
        && locTrangThai(b,this.trangThai())
        && (this.loc()==='giao'||(this.loc()==='kiem'?modes.find(m=>m.id===b.maBai)?.status==='KIEM_TRA':modes.find(m=>m.id===b.maBai)?.status==='ON_TAP'))),
    })).filter(n => n.hang.length);
  });
}
