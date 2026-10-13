import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Lich } from './lich';

async function setup(reminders: unknown[] = [], sessions: unknown[] = []) {
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(Lich);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/hs/lich').flush(sessions);
  http.expectOne('/api/hs/nhac').flush(reminders);
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  for (const dialog of el.querySelectorAll<HTMLDialogElement>('dialog')) {
    dialog.showModal = () => { dialog.open = true; };
    dialog.close = () => { dialog.open = false; };
  }
  const button = (name: string) => [...el.querySelectorAll<HTMLButtonElement>('button')].find(b => b.textContent?.trim() === name)!;
  return { fixture, http, el, button };
}

function setInput(el: HTMLElement, selector: string, value: string) {
  const input = el.querySelector<HTMLInputElement>(selector)!;
  input.value = value;
  input.dispatchEvent(new Event('input', { bubbles: true }));
}

describe('Lịch theo giờ Việt Nam', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    // Chủ nhật UTC, đã sang thứ hai ở trường.
    vi.setSystemTime(new Date('2026-10-11T18:00:00Z'));
  });
  afterEach(() => vi.useRealTimers());

  it('nhắc ON_TAP và KIEM_TRA dùng dueAt; bỏ qua hạn thiếu hoặc không hợp lệ', async () => {
    const t = await setup([
      { id: 'on', title: 'Ôn đạo hàm', status: 'ON_TAP', details: { dueAt: '2026-10-12T03:00:00Z' } },
      { id: 'kt', title: 'Kiểm tra cực trị', status: 'KIEM_TRA', details: { dueAt: '2026-10-13T03:00:00Z' } },
      { id: 'null', title: 'Chưa đặt hạn', status: 'ON_TAP', details: { dueAt: null } },
      { id: 'bad', title: 'Hạn lỗi', status: 'KIEM_TRA', details: { dueAt: 'không phải ngày' } },
      { id: 'session', title: 'Nhắc buổi học', status: 'CHUA_XONG', details: { startsAt: '2026-10-12T03:00:00Z' } },
    ]);
    const days = t.el.querySelectorAll('.week-grid section');
    expect(days[0].textContent).toContain('Ôn đạo hàm');
    expect(days[1].textContent).toContain('Kiểm tra cực trị');
    expect(t.el.querySelectorAll('.schedule-item')).toHaveLength(2);
    t.http.verify();
  });

  it('tuần, ngày, giờ buổi học và hạn qua nửa đêm đều dùng Asia/Ho_Chi_Minh', async () => {
    const t = await setup([
      { id: 'on', title: 'Hạn qua nửa đêm', status: 'ON_TAP', details: { dueAt: '2026-10-11T18:30:00Z' } },
    ], [
      { id: 'b', title: 'Học đầu tuần', status: 'CHUA_XONG', details: { startsAt: '2026-10-11T18:00:00Z', endsAt: '2026-10-11T19:00:00Z' } },
    ]);
    const monday = t.el.querySelector('.week-grid section')!;
    expect(t.el.querySelector('.toolbar strong')?.textContent).toBe('12/10 – 18/10');
    expect(monday.textContent).toContain('Thứ hai');
    expect(monday.textContent).toContain('01:00–02:00');
    expect(monday.textContent).toContain('Hạn qua nửa đêm');
    expect(monday.textContent).toContain('01:30');
    t.http.verify();
  });

  it('chuyển tuần qua năm mới và trở về tuần trước theo ngày ở trường', async () => {
    vi.setSystemTime(new Date('2026-12-31T18:00:00Z'));
    const t = await setup();
    expect(t.el.querySelector('.toolbar strong')?.textContent).toBe('28/12 – 03/01');
    t.button('Tuần sau →').click();
    await t.fixture.whenStable();
    expect(t.el.querySelector('.toolbar strong')?.textContent).toBe('04/01 – 10/01');
    t.button('← Tuần trước').click();
    await t.fixture.whenStable();
    expect(t.el.querySelector('.toolbar strong')?.textContent).toBe('28/12 – 03/01');
    t.http.verify();
  });

  it('datetime-local là +07:00 khi gửi API, kể cả khi UTC thuộc ngày trước', async () => {
    const t = await setup();
    t.button('Thêm buổi học').click();
    await t.fixture.whenStable();
    setInput(t.el, 'form input:not([type=datetime-local])', 'Ôn tập đầu ngày');
    setInput(t.el, 'input[type=datetime-local]', '2026-10-12T00:30');
    const times = t.el.querySelectorAll<HTMLInputElement>('input[type=datetime-local]');
    times[1].value = '2026-10-12T01:30';
    times[1].dispatchEvent(new Event('input'));
    await t.fixture.whenStable();
    t.el.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    const request = t.http.expectOne(r => r.url === '/api/hs/lich' && r.method === 'POST');
    expect(request.request.body).toEqual({ title: 'Ôn tập đầu ngày', startsAt: '2026-10-11T17:30:00.000Z', endsAt: '2026-10-11T18:30:00.000Z' });
    request.flush({ message: 'Đã lưu', count: 1 });
    await Promise.resolve();
    TestBed.tick();
    t.http.expectOne('/api/hs/lich').flush([]);
    await t.fixture.whenStable();
    t.http.verify();
  });

  it.each(['2026-10-12T00:30', '2026-10-12T01:30'])('không gửi khi giờ kết thúc %s trước hoặc bằng giờ bắt đầu', async endsAt => {
    const t = await setup();
    t.button('Thêm buổi học').click();
    await t.fixture.whenStable();
    setInput(t.el, 'form input:not([type=datetime-local])', 'Ôn tập');
    const times = t.el.querySelectorAll<HTMLInputElement>('input[type=datetime-local]');
    for (const [index, value] of ['2026-10-12T01:30', endsAt].entries()) {
      times[index].value = value;
      times[index].dispatchEvent(new Event('input'));
    }
    await t.fixture.whenStable();
    t.el.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await t.fixture.whenStable();
    expect(t.el.querySelector('[role=alert]')?.textContent).toContain('Giờ kết thúc phải sau giờ bắt đầu.');
    t.http.expectNone(r => r.method === 'POST');
    t.http.verify();
  });
});
