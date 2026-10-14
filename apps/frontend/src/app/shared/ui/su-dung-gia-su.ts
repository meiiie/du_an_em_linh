import { httpResource } from '@angular/common/http';
import { Component, computed, input } from '@angular/core';
import { BaoCaoGiaSu } from '../../api/su-dung-gia-su';
import { Item } from '../../api/ux';
import { Button } from './button';
import { DinhDangSo } from './dinh-dang';
import { KhungCho, LoiTai, TrangRong } from './trang-thai';

@Component({
  selector: 'app-su-dung-gia-su',
  imports: [Button, DinhDangSo, KhungCho, LoiTai, TrangRong],
  template: `
    <section class="surface" aria-label="Sử dụng gia sư AI">
      <div class="toolbar"><h2>Sử dụng gia sư AI</h2>
        <button appButton variant="secondary" type="button" [disabled]="baoCao.isLoading()" (click)="baoCao.reload()">Cập nhật</button>
      </div>
      @if (baoCao.isLoading()) { <app-khung-cho kieu="bang" nhan="Đang tải sử dụng gia sư AI" /> }
      @else if (baoCao.error()) { <app-loi-tai [loi]="baoCao.error()" (thuLai)="baoCao.reload()" /> }
      @else if (baoCao.hasValue()) {
        @let bc = baoCao.value();
        <p data-testid="tong-su-dung-ai"><strong>Tổng lớp:</strong> {{bc.requests | soViet}} lượt gọi · {{bc.inputTokens + bc.outputTokens | soViet}} token</p>
        <p class="muted">Đầu vào: {{bc.inputTokens | soViet}} token · Đầu ra: {{bc.outputTokens | soViet}} token · Ước tính: {{bc.estimatedTokens | soViet}} token.</p>
        <p class="muted">Token ước tính đã nằm trong tổng. Số liệu tích lũy; chưa có dữ liệu giới hạn sử dụng.</p>
        @if (bc.students.length) {
          <div class="table-scroll" role="region" aria-label="Sử dụng AI theo học sinh" tabindex="0">
            <table class="data-table">
              <caption>Sử dụng gia sư theo học sinh</caption>
              <thead><tr><th scope="col">Học sinh</th><th scope="col">Lượt gọi</th><th scope="col">Token đầu vào</th><th scope="col">Token đầu ra</th><th scope="col">Token ước tính</th><th scope="col">Tổng token</th><th scope="col">Giới hạn</th></tr></thead>
              <tbody>@for (hs of bc.students; track hs.studentId) {
                <tr><th scope="row">{{tenHocSinh().get(hs.studentId) || 'Học sinh ' + hs.studentId}}</th>
                  <td data-label="Lượt gọi">{{hs.requests | soViet}}</td><td data-label="Token đầu vào">{{hs.inputTokens | soViet}}</td>
                  <td data-label="Token đầu ra">{{hs.outputTokens | soViet}}</td><td data-label="Token ước tính">{{hs.estimatedTokens | soViet}}</td>
                  <td data-label="Tổng token">{{hs.inputTokens + hs.outputTokens | soViet}}</td><td data-label="Giới hạn">Chưa có dữ liệu</td></tr>
              }</tbody>
            </table>
          </div>
        } @else { <app-trang-rong tieuDe="Chưa có lượt sử dụng AI" moTa="Khi học sinh dùng gia sư AI, số liệu sẽ xuất hiện ở đây." /> }
      }
    </section>`,
  styles: `:host{display:block;min-width:0}th{overflow-wrap:anywhere}.toolbar h2{margin:0}`,
})
export class SuDungGiaSu {
  readonly lopId = input.required<string>();
  readonly vai = input.required<'gv' | 'qt'>();
  readonly hocSinh = input<readonly Pick<Item, 'id' | 'title'>[]>([]);
  protected readonly tenHocSinh = computed(() => new Map(this.hocSinh().map(h => [h.id, h.title])));
  protected readonly baoCao = httpResource<BaoCaoGiaSu>(() => this.lopId()
    ? `/api/${this.vai()}/lop/${encodeURIComponent(this.lopId())}/gia-su/su-dung` : undefined);
}
