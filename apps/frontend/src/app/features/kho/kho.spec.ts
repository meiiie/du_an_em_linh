import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Kho } from './kho';

describe('Kho', () => {
  it('lỗi HTTP 500 hiện thông báo và cho tải lại kho', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(Kho);
    fixture.detectChanges();
    http.expectOne('/api/hs/kho').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('Chưa tải được');
    expect(alert.textContent).toContain('Chưa tải được công thức và tài liệu của lớp.');
    const retry = alert.querySelector('button')!;
    expect(retry.textContent).toBe('Thử lại');
    retry.click();
    TestBed.tick();
    http.expectOne('/api/hs/kho').flush({ formulas: [], documents: [] });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Chưa có công thức');
    http.verify();
  });
});

describe('Xuất CSV công thức', () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });
  for (const prefix of ['\uFEFF', '']) {
    it(`tệp tải có đúng một BOM khi API ${prefix ? 'đã có' : 'chưa có'} BOM`, async () => {
      TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
      const http = TestBed.inject(HttpTestingController);
      const fixture = TestBed.createComponent(Kho);
      fixture.detectChanges();
      http.expectOne('/api/hs/kho').flush({ formulas: [{ id: 'f', title: 'Đạo hàm', subtitle: 'x', status: 'DAT', details: { statement: 'Công thức', tier1: '', tier2: '', version: 1 } }], documents: [] });
      await fixture.whenStable();
      let downloaded: Blob | undefined;
      const create = vi.fn((blob: Blob) => { downloaded = blob; return 'blob:test-csv'; });
      const revoke = vi.fn();
      vi.stubGlobal('URL', class extends URL { static override createObjectURL = create; static override revokeObjectURL = revoke; });
      let filename = '';
      vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function(this: HTMLAnchorElement) { filename = this.download; });
      const el = fixture.nativeElement as HTMLElement;
      [...el.querySelectorAll<HTMLButtonElement>('button')].find(b => b.textContent?.trim() === 'Xuất CSV')!.click();
      const content = 'thuật ngữ,định nghĩa\r\nĐạo hàm,Công thức\r\n';
      http.expectOne('/api/hs/flashcards/export').flush({ filename: 'cong-thuc.csv', content: prefix + content });
      await fixture.whenStable();
      expect(create).toHaveBeenCalledOnce();
      const bytes = await new Promise<ArrayBuffer>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as ArrayBuffer);
        reader.onerror = () => reject(reader.error);
        reader.readAsArrayBuffer(downloaded!);
      });
      expect([...new Uint8Array(bytes)]).toEqual([...new TextEncoder().encode('\uFEFF' + content)]);
      expect(downloaded!.type).toBe('text/csv;charset=utf-8');
      expect(filename).toBe('cong-thuc.csv');
      expect(revoke).toHaveBeenCalledWith('blob:test-csv');
      http.verify();
    });
  }
});
