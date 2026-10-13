import { TestBed } from '@angular/core/testing';
import { Phien } from '../../../core/auth/phien';
import { GiaSu, docSse } from './gia-su';

describe('GiaSu', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.unstubAllGlobals());

  async function setup(open = true) {
    TestBed.configureTestingModule({ providers: [{ provide: Phien, useValue: { tokenTruyCap: () => 'synthetic-token', lamMoi: async () => false } }] });
    const f = TestBed.createComponent(GiaSu);
    f.componentRef.setInput('maBai', 'BAI-THU');
    f.componentRef.setInput('maBuoc', 'B.DH.TXD');
    f.detectChanges();
    await f.whenStable();
    const el = f.nativeElement as HTMLElement;
    if (open) { f.componentInstance.moGiaSu(); await f.whenStable(); }
    const settle = async () => { await new Promise(r => setTimeout(r, 0)); await f.whenStable(); };
    const click = async (name: string) => {
      const b = [...el.querySelectorAll('button')].find(x => x.textContent?.trim() === name)!;
      b.click(); await settle();
    };
    return { f, el, click, settle };
  }
  const reply = (message: string) => new Response(`event: xong\ndata: ${JSON.stringify({ message })}\n\n`, { headers: { 'Content-Type': 'text/event-stream' } });

  it('khôi phục trạng thái mở và độ rộng, phím tắt đóng/mở và Esc', async () => {
    localStorage.setItem('mathl-gia-su-mo', 'true');
    localStorage.setItem('mathl-gia-su-rong', '460');
    const t = await setup(false);
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).not.toBeNull();
    expect(t.el.querySelector('[role="separator"]')?.getAttribute('aria-valuenow')).toBe('460');
    document.dispatchEvent(new KeyboardEvent('keydown', { key: '/', ctrlKey: true }));
    await t.settle();
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).toBeNull();
    expect(localStorage.getItem('mathl-gia-su-mo')).toBe('false');
    document.dispatchEvent(new KeyboardEvent('keydown', { key: '/', metaKey: true }));
    await t.settle();
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).not.toBeNull();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await t.settle();
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).toBeNull();
  });

  it('ba chips gửi ngữ cảnh bước và chỉ tăng gợi ý tới cấp ba; render KaTeX', async () => {
    const fetchMock = vi.fn(async () => reply('Hãy xét $x^2$ trước.'));
    vi.stubGlobal('fetch', fetchMock);
    const t = await setup();
    for (let i = 0; i < 4; i++) await t.click('Gợi ý bước tiếp');
    await t.click('Giải thích công thức');
    await t.click('Em sai ở đâu?');
    expect(fetchMock.mock.calls.map(call => JSON.parse((call as unknown as [string, RequestInit])[1].body as string).cap)).toEqual([1,2,3,3,3,3]);
    const payload = JSON.parse((fetchMock.mock.calls[0] as unknown as [string, RequestInit])[1].body as string);
    expect(payload).toMatchObject({ maBai:'BAI-THU', maBuoc:'B.DH.TXD', cauHoi:'Gợi ý bước tiếp' });
    expect(t.el.querySelectorAll('article')).toHaveLength(12);
    expect(t.el.querySelector('article .katex')).not.toBeNull();
    expect(t.el.querySelector('[role="log"]')?.getAttribute('aria-live')).toBe('polite');
  });

  it('lỗi giữ câu hỏi để Hỏi lại sửa và gửi, không tự gọi API lại', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response('', { status:503 })).mockResolvedValueOnce(reply('Xem điều kiện xác định.'));
    vi.stubGlobal('fetch', fetchMock);
    const t = await setup();
    await t.click('Giải thích công thức');
    expect(t.el.querySelector('[role="alert"]')?.textContent).toContain('Kiểm tra kết nối');
    await t.click('Hỏi lại');
    const o = t.el.querySelector<HTMLTextAreaElement>('textarea')!;
    expect(o.value).toBe('Giải thích công thức');
    expect(fetchMock).toHaveBeenCalledTimes(1);
    await t.click('Gửi câu hỏi');
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(t.el.querySelector('[role="alert"]')).toBeNull();
  });

  it('Enter gửi, Shift+Enter và phím IME giữ nội dung; chờ có trạng thái và chặn gửi trùng', async () => {
    let resolve!: (r:Response) => void;
    const fetchMock = vi.fn(() => new Promise<Response>(r => resolve = r));
    vi.stubGlobal('fetch', fetchMock);
    const t = await setup();
    const o = t.el.querySelector<HTMLTextAreaElement>('textarea')!;
    o.value='Em hỏi về $x^2$';o.dispatchEvent(new Event('input'));await t.settle();
    o.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',shiftKey:true,bubbles:true}));
    o.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',isComposing:true,bubbles:true}));
    expect(fetchMock).not.toHaveBeenCalled();
    o.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true,cancelable:true}));await t.settle();
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(t.el.querySelector('.typing')?.textContent).toContain('Đang nghĩ');
    expect([...t.el.querySelectorAll<HTMLButtonElement>('[data-testid="gia-su-chip"]')].every(b=>b.disabled)).toBe(true);
    resolve(reply('Đọc công thức trước.'));await t.settle();
    expect(t.el.querySelector('.typing')).toBeNull();
  });

  it('độ rộng có thể đổi bằng bàn phím và giới hạn ở 360–480', async () => {
    const t = await setup();
    const handle = t.el.querySelector<HTMLElement>('[role="separator"]')!;
    for(let i=0;i<30;i++) handle.dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowLeft',bubbles:true}));
    await t.settle();expect(handle.getAttribute('aria-valuenow')).toBe('480');
    for(let i=0;i<30;i++) handle.dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowRight',bubbles:true}));
    await t.settle();expect(localStorage.getItem('mathl-gia-su-rong')).toBe('360');
  });

  it('SSE chỉ hiện lời gợi ý sau sự kiện xong hoàn chỉnh', () => {
    const chunk = 'event: xong\ndata: {"message":"Gợi ý';
    const first = docSse(chunk);
    expect(first.events).toEqual([]);
    expect(docSse(first.rest + ' đã kiểm."}\n\n').events[0].data.message).toBe('Gợi ý đã kiểm.');
  });
});
