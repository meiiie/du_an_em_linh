import { Component, computed, input, model } from '@angular/core';
import { Button } from './button';
import { DinhDangSo } from './dinh-dang';

@Component({
  selector: 'app-phan-trang',
  imports: [Button, DinhDangSo],
  template: `@if (tong() > 20) {<nav class="pagination" [attr.aria-label]="nhan()">
    <button appButton variant="secondary" type="button" [disabled]="trang() <= 1" (click)="trang.set(trang() - 1)">Trang trước</button>
    <span aria-live="polite">Trang {{trang() | soViet}} / {{soTrang() | soViet}}</span>
    <button appButton variant="secondary" type="button" [disabled]="trang() >= soTrang()" (click)="trang.set(trang() + 1)">Trang sau</button>
  </nav>}`,
})
export class PhanTrang {
  readonly tong = input.required<number>();
  readonly trang = model(1);
  readonly nhan = input('Phân trang danh sách');
  protected readonly soTrang = computed(() => Math.ceil(this.tong() / 20));
}
