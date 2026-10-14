import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Lich } from './lich';

async function setup(reminders: unknown[] = [], sessions: unknown[] = [], plan: {sessions:unknown[];loiKhuyen:string} = {sessions:[],loiKhuyen:""}) {
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(Lich);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/hs/lich').flush(sessions);
  http.expectOne('/api/hs/nhac').flush(reminders);
  for (const request of http.match('/api/hs/ke-hoach-tuan?tuan=0')) request.flush(plan);
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
  it('hiện lịch gợi ý mặc định và lời khuyên bắt đầu khi chưa làm bài', async () => {
    const t = await setup([], [], {loiKhuyen:'Bắt đầu bằng một bài Nhận biết.',sessions:[{id:'goi-y',title:'Ôn công thức · Tập xác định',subtitle:'Buổi học gợi ý',status:'GOI_Y',details:{startsAt:'2026-10-12T12:30:00Z',endsAt:'2026-10-12T12:50:00Z'}}]});
    expect(t.el.querySelector('[data-testid=loi-khuyen]')?.textContent).toContain('Bắt đầu');
    expect(t.el.querySelector('[data-testid=lich-tuan]')?.textContent).toContain('Tập xác định');
    expect(t.el.querySelector('.schedule-item')?.textContent).toContain('19:30–19:50');
    expect(t.el.querySelector('.schedule-item button')).toBeNull();
    t.http.verify();
  });

  it('kỹ năng yếu đang kẹt: hiện lời khuyên đúng dạng và chưa tăng độ khó', async () => {
    const t = await setup([], [], {loiKhuyen:'Ôn lại Xét dấu đạo hàm: Nhầm dấu; em đang kẹt, chưa tăng độ khó.',sessions:[]});
    expect(t.el.querySelector('[data-testid=loi-khuyen]')?.textContent).toContain('Xét dấu đạo hàm');
    expect(t.el.querySelector('[data-testid=loi-khuyen]')?.textContent).toContain('chưa tăng độ khó');
    t.http.verify();
  });

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
    TestBed.tick();
    t.http.expectOne('/api/hs/ke-hoach-tuan?tuan=1').flush({sessions:[],loiKhuyen:''});
    await t.fixture.whenStable();
    expect(t.el.querySelector('.toolbar strong')?.textContent).toBe('04/01 – 10/01');
    t.button('← Tuần trước').click();
    TestBed.tick();
    t.http.expectOne('/api/hs/ke-hoach-tuan?tuan=0').flush({sessions:[],loiKhuyen:''});
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


describe('Điều khiển lịch trên màn hẹp',()=>{
 it('nút thông báo có kiểu nút và tuần trước/sau cùng nhóm, không ghép phần rỗng',async()=>{
  const t=await setup([{id:'r',title:'Ôn xen kẽ — công thức các chương · ',subtitle:'',status:'',details:{}}]);
  const push=t.button('Bật thông báo trình duyệt');
  expect(push.classList.contains('btn-secondary')).toBe(true);
  expect(push.classList.contains('nut-thong-bao')).toBe(true);
  expect(push.getAttribute('aria-pressed')).toBe('false');
  expect(t.el.querySelector('.chon-tuan')?.querySelectorAll('button')).toHaveLength(2);
  expect(t.el.textContent).toContain('Ôn xen kẽ — công thức các chương');
  expect(t.el.textContent).not.toContain('các chương ·');
  t.http.verify();
 });
});
