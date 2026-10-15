import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SuDungAiQuanTri } from './su-dung-ai';
import { DIEU_HUONG } from '../../shared/layout/dieu-huong';
it('mục Sử dụng AI mở báo cáo đúng lớp với tên học sinh và tổng thống nhất',async()=>{
 expect(DIEU_HUONG.QT.some(i=>i.duongDan==='/qt/su-dung-ai')).toBe(true);
 TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
 const f=TestBed.createComponent(SuDungAiQuanTri);f.detectChanges();const http=TestBed.inject(HttpTestingController);
 http.expectOne('/api/qt/lop').flush([{id:'lop1',title:'12A3',subtitle:'',status:'MO',details:{}}]);
 http.expectOne('/api/qt/nguoi-dung').flush([{id:'nam',title:'Nguyễn Hoài Nam',subtitle:'',status:'STUDENT',details:{}}]);await new Promise(r=>setTimeout(r,0));TestBed.tick();
 http.expectOne('/api/qt/lop/lop1/gia-su/su-dung').flush({classId:'lop1',requests:3,inputTokens:100,outputTokens:20,estimatedTokens:20,students:[{studentId:'nam',requests:3,inputTokens:100,outputTokens:20,estimatedTokens:20}]});await f.whenStable();
 const el=f.nativeElement as HTMLElement;expect(el.textContent).toContain('Nguyễn Hoài Nam');expect(el.querySelector('[data-testid=tong-su-dung-ai]')?.textContent).toContain('3 lượt gọi · 120 token');expect(el.textContent).toContain('không cộng thêm');http.verify();
});
