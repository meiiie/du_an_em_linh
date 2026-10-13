import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { QuanTri } from './quan-tri';

describe('Danh sách người dùng', () => {
  it('mỗi trang 20 người, lọc vai trò và tìm tên đặt lại trang, giữ lựa chọn', async () => {
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
    const f=TestBed.createComponent(QuanTri); f.detectChanges();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/qt/nguoi-dung').flush(Array.from({length:45},(_,i)=>({id:String(i),title:'Người '+i,subtitle:'user'+i+'@example.test',status:i===44?'TEACHER':'STUDENT',details:{enabled:true,synthetic:true}})));
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    const rows=()=>el.querySelectorAll('tbody tr');
    const button=(name:string)=>[...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===name)!;
    expect(rows()).toHaveLength(20);
    (el.querySelector('[type=checkbox]') as HTMLInputElement).click();
    button('Trang sau').click(); await f.whenStable();
    expect(rows()[0].textContent).toContain('Người 20');
    button('Giáo viên').click(); await f.whenStable();
    expect(rows()).toHaveLength(1);
    expect(rows()[0].textContent).toContain('Người 44');
    button('Mọi vai trò').click(); await f.whenStable();
    expect((el.querySelector('[type=checkbox]') as HTMLInputElement).checked).toBe(true);
    button('Trang sau').click(); await f.whenStable();
    const search=el.querySelector('[type=search]') as HTMLInputElement;
    search.value='Người 44';search.dispatchEvent(new Event('input'));await f.whenStable();
    expect(rows()).toHaveLength(1);
    expect(rows()[0].textContent).toContain('Người 44');
    http.verify();
  });
});
