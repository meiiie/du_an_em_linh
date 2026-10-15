import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DeBai } from './de-bai';

describe('Đề bài theo chủ đề', () => {
  it('gộp các mức cùng chủ đề, thu gọn và lọc trạng thái mà giữ tiến độ toàn nhóm', async () => {
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});
    const f=TestBed.createComponent(DeBai);f.detectChanges();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/hs/che-do').flush([]);
    http.expectOne('/api/hs/chu-de').flush([]);
    http.expectOne('/api/hs/goi-bai').flush([]);
    http.expectOne('/api/hs/bai').flush([
      {tenBo:'Ôn tập tuần 7',maBai:'1',kyNang:'HS',tenKyNang:'Hàm số',muc4:'NHAN_BIET',deBai:'Đề',deBaiLatex:'x',han:"2026-10-20T13:00:00Z",trangThai:'CHUA_LAM',ketQua:null,soBuoc:5,soBuocDat:0},
      {tenBo:'Ôn tập tuần 7',maBai:'2',kyNang:'HS',tenKyNang:'Hàm số',muc4:'VAN_DUNG',deBai:'Đề',deBaiLatex:'x',han:"2026-10-20T13:00:00Z",trangThai:'DA_NOP',ketQua:'DAT',soBuoc:5,soBuocDat:5},
      {tenBo:'Ôn tập tuần 7',maBai:'3',kyNang:'HS',tenKyNang:'Hàm số',muc4:'VAN_DUNG',deBai:'Đề',deBaiLatex:'x',han:"2026-10-20T13:00:00Z",trangThai:'DA_NOP',ketQua:'SAI',soBuoc:5,soBuocDat:1},
    ]);
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.nhom-chu-de')).toHaveLength(1);
    expect(el.querySelector('.nhom-chu-de summary')?.textContent).toContain('Ôn tập tuần 7');
    expect(el.querySelector('.nhom-chu-de summary')?.textContent).toContain('Hạn: Thứ Ba, 20/10');
    expect(el.querySelector('.nhom-chu-de summary')?.textContent).toContain('2/3 đã nộp');
    const details=el.querySelector<HTMLDetailsElement>('.nhom-chu-de')!;
    details.querySelector('summary')!.click();
    expect(details.open).toBe(false);
    const button=(label:string)=>[...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===label)!;
    for(const [label,id] of [['Chưa làm','1'],['Chưa đạt','3'],['Đạt','2']]) {
      button(label).click();await f.whenStable();
      expect(el.querySelectorAll('app-hang-bai')).toHaveLength(1);
      expect(el.querySelector(`[data-testid="bai-${id}"]`)).not.toBeNull();
      expect(el.querySelector('.nhom-chu-de summary')?.textContent).toContain('2/3 đã nộp');
    }
    http.verify();
  });
  it('tự học render công thức, văn xuôi trong hàng bài giữ dạng chữ', async () => {
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]});
    const f=TestBed.createComponent(DeBai);f.detectChanges();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/hs/che-do').flush([]);http.expectOne('/api/hs/goi-bai').flush([]);
    http.expectOne('/api/hs/chu-de').flush([{id:'TP12',title:'Tích phân',subtitle:'',status:'',details:{}}]);
    http.expectOne('/api/hs/bai').flush([{maBai:'1',tenKyNang:'Tích phân',muc4:'NHAN_BIET',deBai:'Tính',deBaiLatex:'Tính tích phân $x^2$',trangThai:'CHUA_LAM',soBuoc:5,soBuocDat:0}]);
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    const select=el.querySelector('select')!;select.value='TP12';select.dispatchEvent(new Event('change'));TestBed.tick();
    http.expectOne('/api/hs/tu-hoc?chuDe=TP12').flush([{id:'A',title:'Tích phân',subtitle:'Tính $x^2$',status:'NHAN_BIET',details:{}}]);
    await f.whenStable();
    expect(el.querySelectorAll('.katex')).toHaveLength(2);
    expect([...el.querySelectorAll('annotation')].map(n=>n.textContent)).toEqual(['x^2','x^2']);
    expect(el.querySelector('app-hang-bai')?.textContent).toContain('Tính tích phân');
    http.verify();
  });

});
