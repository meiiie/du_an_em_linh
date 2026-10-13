import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from './button';

@Component({
  selector: 'app-trang-rong',
  template: `<div class="trong"><h2>{{tieuDe()}}</h2><p>{{moTa()}}</p><ng-content /></div>`,
})
export class TrangRong {
  readonly tieuDe = input('Chưa có nội dung');
  readonly moTa = input('');
}

@Component({
  selector: 'app-khung-cho',
  template: `<div class="khung-cho" role="status" aria-busy="true"><span class="sr-only">{{nhan()}}</span><i></i><i></i><i></i></div>`,
})
export class KhungCho { readonly nhan = input('Đang tải'); }

@Component({
  selector: 'app-loi-tai',
  imports: [Button],
  template: `<div class="trong" role="alert"><h2>{{chuaSanSang() ? 'Tính năng chưa sẵn sàng' : 'Chưa tải được'}}</h2><p>{{chuaSanSang() ? 'Thử lại sau để xem nội dung này.' : moTa()}}</p><button appButton variant="secondary" type="button" (click)="thuLai.emit()">Thử lại</button></div>`,
})
export class LoiTai {
  readonly loi = input<unknown>();
  readonly moTa = input('Kiểm tra kết nối rồi thử lại.');
  readonly thuLai = output<void>();
  protected readonly chuaSanSang = computed(() => {
    const e = this.loi();
    return e instanceof HttpErrorResponse && (e.status === 404 || e.status === 501);
  });
}

@Component({
  selector: 'app-so-do',
  imports: [RouterLink],
  template: `<a class="so-do" [routerLink]="duongDan()"><span>{{nhan()}}</span><strong>{{giaTri()}}</strong><span class="muted">{{moTa()}} <span aria-hidden="true">→</span></span></a>`,
})
export class SoDo {
  readonly nhan = input.required<string>();
  readonly giaTri = input.required<number>();
  readonly duongDan = input.required<string>();
  readonly moTa = input('Xem chi tiết');
}
