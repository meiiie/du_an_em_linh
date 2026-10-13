import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Phien } from './auth/phien';
import { GV, PhienGia } from './auth/phien.testing';
import { LopDangChon } from './lop-dang-chon';

function setup() {
  const phien = new PhienGia().dangNhapNhu(GV);
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: Phien, useValue: phien }] });
  const lop = TestBed.inject(LopDangChon);
  TestBed.tick();
  return { phien, lop, http: TestBed.inject(HttpTestingController) };
}

async function load(t: ReturnType<typeof setup>, id: string, pending = 2) {
  const result = t.lop.tai();
  t.http.expectOne('/api/gv/lop').flush([{ id, title: id, subtitle: '', status: '', details: {} }]);
  await Promise.resolve();
  t.http.expectOne('/api/gv/tong-quan?lopId=' + id).flush({ pending });
  await result;
}

describe('Lớp đang chọn thuộc phiên đăng nhập', () => {
  it('cùng người dùng giữ lớp đã chọn khi làm mới token', async () => {
    const t = setup();
    await load(t, 'lop-a');
    t.lop.id.set('lop-da-chon');
    t.phien.dangNhapNhu({ ...GV }, 'token-moi');
    TestBed.tick();
    await t.lop.tai();
    expect(t.lop.id()).toBe('lop-da-chon');
    expect(t.lop.soChoDuyet()).toBe(2);
    t.http.expectNone('/api/gv/lop');
    t.http.verify();
  });

  it('đăng xuất xóa lớp, lựa chọn, huy hiệu; đăng nhập lại tải mới', async () => {
    const t = setup();
    await load(t, 'lop-a');
    await t.phien.dangXuat();
    TestBed.tick();
    expect(t.lop.ds()).toEqual([]);
    expect(t.lop.id()).toBe('');
    expect(t.lop.soChoDuyet()).toBe(0);
    await t.lop.tai();
    t.http.expectNone('/api/gv/lop');
    t.phien.dangNhapNhu(GV);
    TestBed.tick();
    await load(t, 'lop-moi');
    expect(t.lop.id()).toBe('lop-moi');
    t.http.verify();
  });

  it('đổi tài khoản trực tiếp bỏ cache và tải lớp của người mới', async () => {
    const t = setup();
    await load(t, 'lop-a');
    t.phien.dangNhapNhu({ ...GV, id: 'gv-b' });
    TestBed.tick();
    expect(t.lop.ds()).toEqual([]);
    await load(t, 'lop-b', 7);
    expect(t.lop.id()).toBe('lop-b');
    expect(t.lop.soChoDuyet()).toBe(7);
    t.http.verify();
  });

  it('phản hồi lớp phiên cũ không ghi đè hoặc kết thúc lượt tải phiên mới', async () => {
    const t = setup();
    const oldLoad = t.lop.tai();
    const oldRequest = t.http.expectOne('/api/gv/lop');
    t.phien.dangNhapNhu({ ...GV, id: 'gv-b' });
    TestBed.tick();
    const newLoad = t.lop.tai();
    const newRequest = t.http.expectOne('/api/gv/lop');
    oldRequest.flush([{ id: 'lop-a', details: {} }]);
    await oldLoad;
    expect(t.lop.ds()).toEqual([]);
    expect(t.lop.dangTai()).toBe(true);
    t.http.expectNone('/api/gv/tong-quan?lopId=lop-a');
    newRequest.flush([{ id: 'lop-b', details: {} }]);
    await Promise.resolve();
    t.http.expectOne('/api/gv/tong-quan?lopId=lop-b').flush({ pending: 7 });
    await newLoad;
    expect(t.lop.id()).toBe('lop-b');
    t.http.verify();
  });

  it('huy hiệu phiên cũ không ghi lại sau khi đăng xuất', async () => {
    const t = setup();
    const result = t.lop.tai();
    t.http.expectOne('/api/gv/lop').flush([{ id: 'lop-a', details: {} }]);
    await Promise.resolve();
    const badge = t.http.expectOne('/api/gv/tong-quan?lopId=lop-a');
    await t.phien.dangXuat();
    TestBed.tick();
    badge.flush({ pending: 99 });
    await result;
    expect(t.lop.soChoDuyet()).toBe(0);
    expect(t.lop.ds()).toEqual([]);
    t.http.verify();
  });

  it('lỗi phiên cũ không bật lỗi của phiên mới; tải thất bại có thể thử lại', async () => {
    const t = setup();
    const oldLoad = t.lop.tai();
    const oldRequest = t.http.expectOne('/api/gv/lop');
    t.phien.dangNhapNhu({ ...GV, id: 'gv-b' });
    TestBed.tick();
    oldRequest.flush({}, { status: 500, statusText: 'Server Error' });
    await oldLoad;
    expect(t.lop.loi()).toBe(false);
    const failed = t.lop.tai();
    t.http.expectOne('/api/gv/lop').flush({}, { status: 500, statusText: 'Server Error' });
    await failed;
    expect(t.lop.loi()).toBe(true);
    await load(t, 'lop-b');
    expect(t.lop.loi()).toBe(false);
    t.http.verify();
  });
});
