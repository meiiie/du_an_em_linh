import { Component, computed, input, model, output, signal } from '@angular/core';
import { ViTriSai } from '../../../api/hoc-sinh';
import { HANG_DAU, HANG_MUI, HANG_X, Mui, XET_DAU } from './phieu';

export interface SuKienO {
  readonly hang: string;
  readonly k: number;
  readonly giaTriCu: string | null;
  readonly giaTriMoi: string;
}

/** Giá trị gửi đi (ASCII như v0) → chữ trên nút và tên đọc màn hình. */
const DAU: Record<string, { chu: string; ten: string }> = {
  '+': { chu: '+', ten: 'cộng (+)' },
  '-': { chu: '−', ten: 'trừ (−)' },
  '0': { chu: '0', ten: 'bằng 0' },
  '||': { chu: '||', ten: 'không xác định (||)' },
};
const MUI: { gia: Mui; chu: string; ten: string }[] = [
  { gia: 'TANG', chu: '↗', ten: 'tăng (đồng biến)' },
  { gia: 'GIAM', chu: '↘', ten: 'giảm (nghịch biến)' },
];

/**
 * Bảng xét dấu trên tấm bảng 3b1b, cùng cách nhập với v0 (`apps/web/components/solve-client.tsx`): học sinh thêm mốc theo
 * thứ tự mình chọn (máy không tự sắp, sai thứ tự do máy chủ báo), chọn dấu y′ ở từng khoảng và từng mốc, chọn mũi tên ở
 * từng khoảng. Ô theo chỉ số `k` của `docs/chi-so-o-bang.md`. Không tự kiểm đúng / sai: chỉ tô ô máy chủ báo sai.
 */
@Component({
  selector: 'app-bang-xet-dau',
  template: `
    <div class="them">
      <input
        class="o-moc"
        data-testid="moc-nhap"
        aria-label="Mốc x"
        placeholder="mốc x…"
        maxlength="60"
        [value]="nhapMoc()"
        (input)="nhapMoc.set($any($event.target).value)"
        (keydown.enter)="themMoc()"
      />
      <button type="button" class="nut-them" data-testid="moc-them" (click)="themMoc()">Thêm mốc</button>
    </div>
    <div class="cuon" data-testid="bang-xet-dau">
      <table>
        <tbody>
          <tr data-testid="hang-x">
            <th scope="row">x</th>
            @if (!moc().length) {
              <td>−∞</td>
              <td>+∞</td>
            } @else {
              @for (k of cot(); track k) {
                @if (k % 2) {
                  <td [attr.data-testid]="'x-' + (k - 1) / 2" [class.sai]="sai(hangX, (k - 1) / 2)">
                    <span class="moc">{{ moc()[(k - 1) / 2] }}</span>
                    <button type="button" class="xoa" [attr.aria-label]="'Xóa mốc ' + moc()[(k - 1) / 2]" (click)="xoaMoc((k - 1) / 2)">×</button>
                  </td>
                } @else {
                  <td class="vo-cuc">{{ k === 0 ? '−∞' : k === cot().length - 1 ? '+∞' : '' }}</td>
                }
              }
            }
          </tr>
          <tr>
            <th scope="row">y′</th>
            @for (k of cotDau(); track k) {
              <td
                [attr.colspan]="moc().length ? null : 2"
                [attr.data-testid]="'o-dau-' + k"
                [class.sai]="sai(hangDau, k)"
                role="group"
                [attr.aria-label]="'Dấu y′ ' + viTri(k) + (dau()[k] ? ': ' + tenDau(dau()[k]) : '')"
              >
                <span class="chon">
                  @for (v of luaChonDau(k); track v) {
                    <button
                      type="button"
                      [class]="'nut-o dau-' + tenLop(v)"
                      [attr.aria-label]="tenDau(v)"
                      [attr.aria-pressed]="dau()[k] === v"
                      [attr.data-testid]="'dau-' + k + '-' + v"
                      (click)="chonDau(k, v)"
                    >
                      {{ chuDau(v) }}
                    </button>
                  }
                </span>
              </td>
            }
          </tr>
          <tr>
            <th scope="row">y</th>
            @for (k of cotDau(); track k) {
              @if (k % 2) {
                <td></td>
              } @else {
                <td
                  [attr.colspan]="moc().length ? null : 2"
                  [attr.data-testid]="'o-mui-' + k"
                  [class.sai]="sai(hangMui, k)"
                  role="group"
                  [attr.aria-label]="'Chiều biến thiên ' + viTri(k)"
                >
                  <span class="chon">
                    @for (m of muiLuaChon; track m.gia) {
                      <button
                        type="button"
                        [class]="'nut-o mui-' + m.gia.toLowerCase()"
                        [attr.aria-label]="m.ten"
                        [attr.aria-pressed]="mui()[k] === m.gia"
                        [attr.data-testid]="'mui-' + k + '-' + m.gia"
                        (click)="chonMui(k, m.gia)"
                      >
                        {{ m.chu }}
                      </button>
                    }
                  </span>
                </td>
              }
            }
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styleUrl: './bang-xet-dau.css',
})
export class BangXetDau {
  readonly moc = model<string[]>([]);
  readonly dau = model<Record<number, string>>({});
  readonly mui = model<Record<number, Mui>>({});
  /** Ô máy chủ báo sai ở lần chấm mới nhất (chỉ các vị trí thuộc bước xét dấu được dùng). */
  readonly oSai = input<readonly ViTriSai[]>([]);
  readonly suKien = output<SuKienO>();

  protected readonly nhapMoc = signal('');
  protected readonly hangX = HANG_X;
  protected readonly hangDau = HANG_DAU;
  protected readonly hangMui = HANG_MUI;
  protected readonly muiLuaChon = MUI;
  /** Cột 0 … 2n (n mốc): chẵn là khoảng, lẻ là tại mốc. */
  protected readonly cot = computed(() => Array.from({ length: this.moc().length * 2 + 1 }, (_, k) => k));
  /** Chưa có mốc: một ô khoảng gộp hai cột (−∞; +∞), như v0 UXT-04-e. */
  protected readonly cotDau = computed(() => (this.moc().length ? this.cot() : [0]));

  protected sai(hang: string, k: number): boolean {
    return this.oSai().some((o) => o.maBuoc === XET_DAU && o.hang === hang && o.k === k);
  }

  protected viTri(k: number): string {
    const m = this.moc();
    if (k % 2 === 1) return `tại x = ${m[(k - 1) / 2] ?? ''}`;
    const j = k / 2;
    return `trên khoảng (${j === 0 ? '−∞' : m[j - 1]}; ${j === m.length ? '+∞' : m[j]})`;
  }

  /** Ô khoảng hiện +, − trước; ô tại mốc hiện 0, || trước (v0 UXT-10-b); ô nào cũng chọn được đủ bốn giá trị. */
  protected luaChonDau(k: number): string[] {
    return k % 2 ? ['0', '||', '+', '-'] : ['+', '-', '0', '||'];
  }

  protected chuDau(v: string): string {
    return DAU[v]?.chu ?? v;
  }

  protected tenDau(v: string): string {
    return DAU[v]?.ten ?? v;
  }

  protected tenLop(v: string): string {
    return v === '+' ? 'cong' : v === '-' ? 'tru' : v === '0' ? 'khong' : 'kxd';
  }

  protected themMoc(): void {
    const v = this.nhapMoc().trim();
    if (!v) return;
    this.suKien.emit({ hang: HANG_X, k: this.moc().length, giaTriCu: null, giaTriMoi: v });
    this.moc.update((m) => [...m, v]);
    this.nhapMoc.set('');
  }

  /** Bỏ một mốc làm lệch mọi chỉ số `k` sau nó, nên xóa luôn dấu và mũi tên đã chọn (như v0). */
  protected xoaMoc(i: number): void {
    this.moc.update((m) => m.filter((_, j) => j !== i));
    this.dau.set({});
    this.mui.set({});
  }

  protected chonDau(k: number, v: string): void {
    this.suKien.emit({ hang: HANG_DAU, k, giaTriCu: this.dau()[k] ?? null, giaTriMoi: v });
    this.dau.update((d) => ({ ...d, [k]: v }));
  }

  protected chonMui(k: number, v: Mui): void {
    this.suKien.emit({ hang: HANG_MUI, k, giaTriCu: this.mui()[k] ?? null, giaTriMoi: v });
    this.mui.update((d) => ({ ...d, [k]: v }));
  }
}
