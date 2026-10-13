import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { LopDangChon } from '../../core/lop-dang-chon';
import { PhongGiaoVien } from './phong-giao-vien';

async function setup() {
  const lop = { id: signal('lop1'), ds: signal([{ id: 'lop1', title: '12A1' }, { id: 'lop2', title: '12A2' }]), dangTai: signal(false), loi: signal(false), soChoDuyet: signal(0), tai: async () => {} };
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: LopDangChon, useValue: lop }] });
  const fixture = TestBed.createComponent(PhongGiaoVien);
  fixture.componentRef.setInput('trang', 'giao');
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/gv/ngan-hang?lopId=lop1').flush(Array.from({ length: 45 }, (_, i) => ({ id: 'b' + i, title: 'Bài ' + i, subtitle: '', status: i === 44 ? 'NHAP' : 'DA_PHAT_HANH', details: { level4: 'NHAN_BIET', skillCode: 'T12.DH.03', gate: 'DAT', latex: 'x' } })));
  http.expectOne('/api/gv/tien-do?lopId=lop1').flush(Array.from({ length: 30 }, (_, i) => ({ id: 'hs' + i, title: 'Học sinh ' + i, subtitle: '', status: 'STUDENT', details: { skills: [], submitted: 0 } })));
  http.expectOne('/api/gv/giao?lopId=lop1').flush([]);
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  const lists = () => el.querySelectorAll('fieldset');
  const search = async (index: number, value: string) => { const input = lists()[index].querySelector('input[type=search]')! as HTMLInputElement; input.value = value; input.dispatchEvent(new Event('input')); await fixture.whenStable(); };
  return { fixture, el, lists, search, http, lop };
}

describe('Giao bài với ngân hàng và lớp đầy đủ', () => {
  it('chỉ hiện bài phát hành, tìm bài độc lập với 30 học sinh', async () => {
    const t = await setup();
    expect(t.lists()[0].querySelectorAll('[type=checkbox]')).toHaveLength(44);
    expect(t.lists()[1].querySelectorAll('[type=checkbox]')).toHaveLength(30);
    await t.search(0, 'Bài 43');
    expect(t.lists()[0].querySelectorAll('[type=checkbox]')).toHaveLength(1);
    expect(t.lists()[1].querySelectorAll('[type=checkbox]')).toHaveLength(30);
    await t.search(1, 'Học sinh 29');
    expect(t.lists()[1].querySelectorAll('[type=checkbox]')).toHaveLength(1);
    expect(t.lists()[0].textContent).toContain('Bài 43');
    t.http.verify();
  });
  it('giữ lựa chọn khi tìm bài khác, hiển thị hướng dẫn khi không có kết quả', async () => {
    const t = await setup();
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    await t.fixture.whenStable();
    await t.search(0, 'không tồn tại');
    expect(t.lists()[0].textContent).toContain('Chọn bài (1)');
    expect(t.lists()[0].textContent).toContain('Đổi từ khóa để tìm lại.');
    await t.search(0, '');
    expect((t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).checked).toBe(true);
    t.http.verify();
  });
  it('Enter trong ô tìm kiếm không gửi bộ bài đã đủ điều kiện giao', async () => {
    const t = await setup();
    const name = t.el.querySelector('form > label input') as HTMLInputElement;
    name.value = 'Ôn tập tuần'; name.dispatchEvent(new Event('input'));
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    await t.fixture.whenStable();
    expect((t.el.querySelector('form button[type=submit]') as HTMLButtonElement).disabled).toBe(false);
    for (const list of t.lists()) {
      const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
      list.querySelector('input[type=search]')!.dispatchEvent(event);
      expect(event.defaultPrevented).toBe(true);
    }
    t.http.expectNone(request => request.method === 'POST');
    t.http.verify();
  });
  it('đổi lớp xóa lựa chọn và tìm kiếm, tải dữ liệu đúng lớp mới', async () => {
    const t = await setup();
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    await t.search(0, 'Bài 43');
    const select = t.el.querySelector('[aria-label="Chọn lớp"]') as HTMLSelectElement;
    select.value = 'lop2'; select.dispatchEvent(new Event('change'));
    TestBed.tick();
    t.http.expectOne('/api/gv/ngan-hang?lopId=lop2').flush([]);
    t.http.expectOne('/api/gv/tien-do?lopId=lop2').flush([]);
    t.http.expectOne('/api/gv/giao?lopId=lop2').flush([]);
    await t.fixture.whenStable();
    expect(t.lists()[0].textContent).toContain('Chọn bài (0)');
    expect((t.lists()[0].querySelector('input[type=search]') as HTMLInputElement).value).toBe('');
    expect(t.lists()[0].textContent).toContain('Chưa có bài đã phát hành');
    t.http.verify();
  });
});
