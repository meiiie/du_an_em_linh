import { Component, computed, input } from '@angular/core';
import { KyNangCuaEm, Muc4, TEN_MUC } from '../../api/hoc-sinh';

const BAC: readonly Muc4[] = ['NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO'];

/**
 * Sổ «Kỹ năng» trên trang Học, như `SoKyNang` của v0 (`apps/web/components/so-ky-nang.tsx`): mỗi kỹ năng một dòng, tên,
 * mức bằng lời và bốn ô tô tới mức hiện tại. Kỹ năng em đang kẹt có nhãn và lời nhắc hỏi gia sư hay thầy cô.
 */
@Component({
  selector: 'app-so-ky-nang',
  template: `
    @if (kyNang().length) {
      <ul class="ds">
        @for (k of kyNang(); track k.kyNang) {
          <li [attr.data-testid]="'ky-nang-' + k.kyNang" [attr.aria-label]="k.tenKyNang + ', ' + tenMuc[k.muc4] + (k.ket ? ', đang kẹt' : '')">
            <div class="dong">
              <p class="ten">{{ k.tenKyNang }}</p>
              <p class="muc">
                @if (k.ket) {
                  <span class="ket">kẹt</span>
                }
                {{ tenMuc[k.muc4] }}
              </p>
            </div>
            <div class="bac" aria-hidden="true">
              @for (b of bac; track b; let i = $index) {
                <i [class.dat]="i <= viTri(k.muc4)"></i>
              }
            </div>
          </li>
        }
      </ul>
      @if (coKet()) {<p class="nhac">Kỹ năng đang kẹt: em xem lại công thức hoặc hỏi gia sư để luyện tiếp.</p>}
    } @else {
      <div class="trong">
        <p>Chưa làm bài — làm một bài để hiện kỹ năng.</p>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
    }

    .ds {
      margin: 0;
      padding: 0;
      overflow: hidden;
      list-style: none;
      border: 1px solid var(--line);
      border-radius: var(--radius-card);
      background: var(--raise);
    }

    li {
      padding: var(--space-3) var(--space-4);
    }

    li + li {
      border-top: 1px solid var(--line);
    }

    .dong {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      justify-content: space-between;
      gap: var(--space-1) var(--space-2);
    }

    .ten,
    .muc,
    .nhac {
      margin: 0;
    }

    .ten {
      font-size: 14px;
    }

    .muc {
      flex-shrink: 0;
      color: var(--muted);
      font-size: 13px;
    }

    .ket {
      margin-right: var(--space-2);
      padding: 2px 8px;
      border-radius: 999px;
      background: color-mix(in srgb, var(--wait) 14%, var(--raise));
      color: var(--wait);
      font-weight: 600;
    }

    .bac {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 4px;
      margin-top: var(--space-2);
    }

    .bac i {
      height: 4px;
      border-radius: 999px;
      background: var(--wash);
    }

    .bac i.dat {
      background: var(--pass);
    }

    .nhac {
      margin-top: var(--space-2);
      color: var(--ink-2);
      font-size: 14px;
    }
  `,
})
export class SoKyNang {
  readonly kyNang = input.required<readonly KyNangCuaEm[]>();
  protected readonly coKet = computed(() => this.kyNang().some(k => k.ket));
  protected readonly tenMuc = TEN_MUC;
  protected readonly bac = BAC;

  protected viTri(m: Muc4): number {
    return BAC.indexOf(m);
  }
}
