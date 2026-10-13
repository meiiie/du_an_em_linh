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
    http.expectOne('/api/hs/bai').flush([
      {maBai:'1',kyNang:'HS',tenKyNang:'Hàm số',muc4:'NHAN_BIET',deBai:'Đề',deBaiLatex:'x',han:null,trangThai:'CHUA_LAM',ketQua:null,soBuoc:5,soBuocDat:0},
      {maBai:'2',kyNang:'HS',tenKyNang:'Hàm số',muc4:'VAN_DUNG',deBai:'Đề',deBaiLatex:'x',han:null,trangThai:'DA_NOP',ketQua:'DAT',soBuoc:5,soBuocDat:5},
      {maBai:'3',kyNang:'HS',tenKyNang:'Hàm số',muc4:'VAN_DUNG',deBai:'Đề',deBaiLatex:'x',han:null,trangThai:'DA_NOP',ketQua:'SAI',soBuoc:5,soBuocDat:1},
    ]);
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    expect(el.querySelectorAll('details')).toHaveLength(1);
    expect(el.querySelector('summary')?.textContent).toContain('1/3 đạt');
    const details=el.querySelector('details')!;
    details.querySelector('summary')!.click();
    expect(details.open).toBe(false);
    const button=(label:string)=>[...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===label)!;
    for(const [label,id] of [['Chưa làm','1'],['Chưa đạt','3'],['Đạt','2']]) {
      button(label).click();await f.whenStable();
      expect(el.querySelectorAll('app-hang-bai')).toHaveLength(1);
      expect(el.querySelector(`[data-testid="bai-${id}"]`)).not.toBeNull();
      expect(el.querySelector('summary')?.textContent).toContain('1/3 đạt');
    }
    http.verify();
  });
});
