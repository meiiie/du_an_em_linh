import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiLopHoc } from './lop-hoc';

describe('ApiLopHoc', () => {
  let api: ApiLopHoc;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiLopHoc);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('tổng quan gửi đúng lớp, ký tự đặc biệt không làm thay đổi query', () => {
    api.tongQuan('lop & 12').subscribe();
    http.expectOne('/api/gv/tong-quan?lopId=lop%20%26%2012').flush({ students: 0, stuck: 0, pending: 0, assignments: 0, distribution: {}, deadlines: [] });
  });

  it('xuất thẻ giữ lỗi endpoint để màn báo lỗi, không tạo CSV rỗng', () => {
    let status = 0;
    api.xuatThe().subscribe({ next: () => { throw new Error('Không được coi lỗi là thành công'); }, error: e => { status = e.status; } });
    http.expectOne('/api/hs/flashcards/export').flush({}, { status: 501, statusText: 'Chưa sẵn sàng' });
    expect(status).toBe(501);
  });

  it('giao bộ bài giữ chế độ kiểm tra và hạn nộp trong request', () => {
    const body = { lopId: 'lop-12', studentIds: [], problemIds: ['bai-1'], mode: 'KIEM_TRA' as const, dueAt: '2026-10-14T08:00:00Z', setName: 'Kiểm tra đạo hàm' };
    api.giao(body).subscribe();
    const req = http.expectOne('/api/gv/giao');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ message: 'Đã giao bài', count: 1 });
  });
});
