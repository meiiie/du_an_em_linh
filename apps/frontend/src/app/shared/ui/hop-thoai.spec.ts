import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HopThoai } from './hop-thoai';

@Component({ imports: [HopThoai], template: `<app-hop-thoai [mo]="mo()" tieuDe="Ôn công thức"><button disabled>Thẻ trước</button><button id="lat-the">Lật thẻ</button><button hidden>Ẩn</button><button id="the-tiep">Thẻ tiếp</button></app-hop-thoai>` })
class TrangThu { readonly mo = signal(false); }

describe('HopThoai', () => {
  afterEach(() => vi.restoreAllMocks());
  it('Tab cuối và Shift+Tab đầu quay lại trong hộp thoại, bỏ qua nút vô hiệu và ẩn', async () => {
    // jsdom không cung cấp hành vi modal hay kích thước bố cục của trình duyệt.
    vi.spyOn(HTMLElement.prototype, 'getClientRects').mockImplementation(function (this: HTMLElement) {
      return (this.closest('[hidden]') ? [] : [{}]) as unknown as DOMRectList;
    });
    const fixture = TestBed.createComponent(TrangThu);
    await fixture.whenStable();
    const dialog = fixture.nativeElement.querySelector('dialog') as HTMLDialogElement;
    dialog.showModal = () => { dialog.open = true; };
    dialog.close = () => { dialog.open = false; };
    fixture.componentInstance.mo.set(true);
    await fixture.whenStable();
    const first = dialog.querySelector('button')!;
    const last = dialog.querySelector<HTMLButtonElement>('#the-tiep')!;
    last.focus();
    const tab = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    last.dispatchEvent(tab);
    expect(tab.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(first);
    const back = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true });
    first.dispatchEvent(back);
    expect(back.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(last);
    fixture.destroy();
    vi.restoreAllMocks();
  });
});
