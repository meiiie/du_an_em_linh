import { TestBed } from '@angular/core/testing';
import { API_GIA_SU, SessionGiaSuV2 } from '../../../api/gia-su';
import { Phien } from '../../../core/auth/phien';
import { GiaSu } from './gia-su';
import { GiaSuMarkdown } from './gia-su-markdown';
import { NOI_LAI_GIA_SU } from './gia-su-phien';

const session: SessionGiaSuV2 = {id:'s1',studentId:'synthetic-student',assignmentId:'assignment-1',classId:'synthetic-class',tomTat:'',summaryThrough:0,contextVersion:'1:synthetic',lastSeq:0,revision:1,tokenCount:0};
const json=(body:unknown)=>new Response(JSON.stringify(body),{headers:{'Content-Type':'application/json'}});
const eventFields = (name: string) => name === 'start' ? {maBuoc:'B.DH.TXD',cap:1,contextVersion:'1:synthetic'}
  : name === 'regen' ? {attempt:1,mode:'rewrite'} : name === 'done' ? {lastSentenceIndex:1,tokens:10,durationMs:20}
  : name === 'error' ? {code:'MODEL_UNAVAILABLE'} : {};
const event=(name:string,seq:number,data:object={})=>`id: s1:${seq}\nevent: ${name}\ndata: ${JSON.stringify({v:2,...eventFields(name),sessionId:'s1',turnId:'t1',seq,...data})}\n\n`;
const settle=async()=>{await new Promise(r=>setTimeout(r,5));};

describe('Sidebar gia sư',()=>{
  let fetchMock: ReturnType<typeof vi.fn>;
  let response:(init:RequestInit)=>Response;
  beforeEach(()=>{
    localStorage.clear();
    response=init=>new Response(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'**Câu đã kiểm** $x^2$.'})+event('done',3,{status:'completed'}),{headers:{'Content-Type':'text/event-stream'}});
    fetchMock=vi.fn(async(url:string,init:RequestInit={})=>{
      if(url===API_GIA_SU)return json({sessionId:'s1',lastSeq:0,turnId:null,status:'idle'});
      if(url.includes('/luot?')&&!init.method)return json({session,turns:[]});
      if(url.endsWith('/cancel'))return json({sessionId:'s1',turnId:'t1',status:'cancelled',lastSeq:3});
      return response(init);
    });
    vi.stubGlobal('fetch',fetchMock);
    TestBed.configureTestingModule({providers:[{provide:Phien,useValue:{tokenTruyCap:()=> 'synthetic',lamMoi:async()=>false}},{provide:NOI_LAI_GIA_SU,useValue:[]}]});
  });
  afterEach(()=>vi.unstubAllGlobals());
  async function setup(open=true){
    const f=TestBed.createComponent(GiaSu);
    f.componentRef.setInput('maBai','BAI-THU');f.componentRef.setInput('giaoBaiId','assignment-1');f.componentRef.setInput('maBuoc','B.DH.TXD');
    f.detectChanges();await f.whenStable();
    if(open)f.componentInstance.moGiaSu();
    await settle();await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    const click=async(name:string)=>{([...el.querySelectorAll('button')].find(b=>b.textContent?.trim()===name)!).click();await settle();await f.whenStable();};
    const fill=async(q:string)=>{const t=el.querySelector('textarea')!;t.value=q;t.dispatchEvent(new Event('input'));await f.whenStable();return t;};
    return {f,el,click,fill};
  }
  it('mở mới tải lịch sử, giữ độ rộng, phím tắt và Escape trả focus',async()=>{
    localStorage.setItem('mathl-gia-su-mo','true');localStorage.setItem('mathl-gia-su-rong','460');
    const t=await setup(false);
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).not.toBeNull();
    expect(t.el.querySelector('[role="separator"]')?.getAttribute('aria-valuenow')).toBe('460');
    expect(fetchMock.mock.calls[0][0]).toBe(API_GIA_SU);
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));await t.f.whenStable();
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).toBeNull();
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'/',ctrlKey:true}));await settle();await t.f.whenStable();
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).not.toBeNull();
    expect(fetchMock.mock.calls.filter(c=>c[0]===API_GIA_SU)).toHaveLength(2);
  });
  it('390 px không tự mở sheet dù đã lưu tùy chọn mở',async()=>{
    vi.stubGlobal('matchMedia',()=>({matches:false,addEventListener:vi.fn(),removeEventListener:vi.fn()}));
    localStorage.setItem('mathl-gia-su-mo','true');
    const t=await setup(false);
    expect(t.el.querySelector('[data-testid="gia-su-panel"]')).toBeNull();expect(fetchMock).not.toHaveBeenCalled();
  });
  it('Enter gửi, Shift+Enter và IME không gửi; ngữ cảnh cấp và Markdown/KaTeX',async()=>{
    const t=await setup();const input=await t.fill('Em hỏi về đạo hàm');
    input.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',shiftKey:true,bubbles:true}));
    input.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',isComposing:true,bubbles:true}));
    expect(fetchMock).toHaveBeenCalledTimes(2);
    const cap=t.el.querySelector('select')!;cap.value='2';cap.dispatchEvent(new Event('change'));
    input.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true,cancelable:true}));await settle();await t.f.whenStable();
    expect(JSON.parse(fetchMock.mock.calls[2][1].body)).toMatchObject({maBuoc:'B.DH.TXD',cap:2,cauHoi:'Em hỏi về đạo hàm'});
    expect(t.el.querySelector('article strong')?.textContent).toBe('Câu đã kiểm');expect(t.el.querySelector('.katex')).not.toBeNull();
    expect(t.el.querySelectorAll('article')).toHaveLength(2);
  });
  it('hiện trạng thái chờ polite, regen trước delta và lời đã kiểm khi delta tới',async()=>{
    let writer!: ReadableStreamDefaultController<Uint8Array>;
    response=()=>new Response(new ReadableStream({start(c){writer=c;}}),{headers:{'Content-Type':'text/event-stream'}});
    const t=await setup();await t.fill('Em hỏi');await t.click('Gửi câu hỏi');
    const status=()=>t.el.querySelector<HTMLElement>('.typing')!;
    expect(status().textContent).toContain('Đang suy nghĩ…');
    expect(status().getAttribute('role')).toBe('status');
    expect(status().getAttribute('aria-live')).toBe('polite');
    expect(status().querySelector('span')?.getAttribute('aria-hidden')).toBe('true');
    expect(t.el.querySelector('.cau-cua-ai')).toBeNull();
    const requestId=JSON.parse(fetchMock.mock.calls[2][1].body).requestId;
    writer.enqueue(new TextEncoder().encode(event('start',1,{requestId})+event('regen',2)));
    await settle();await t.f.whenStable();
    expect(status().textContent).toContain('Đang viết lại cho đúng yêu cầu…');
    expect(t.el.querySelector('.cau-cua-ai')).toBeNull();
    writer.enqueue(new TextEncoder().encode(event('delta',3,{sentenceIndex:1,text:'Câu đã kiểm.'})));
    await settle();await t.f.whenStable();
    expect(status().textContent).toContain('Đang soạn lời gợi ý…');
    expect(t.el.querySelector('.cau-cua-ai')?.textContent).toContain('Câu đã kiểm.');
    writer.enqueue(new TextEncoder().encode(event('done',4,{status:'completed'})));
    await settle();await t.f.whenStable();
    expect(t.el.querySelector('.typing')).toBeNull();
  });
  it.each(['done','error','cancel'])('bỏ trạng thái chờ khi %s trước delta đầu tiên',async(terminal)=>{
    let writer!: ReadableStreamDefaultController<Uint8Array>;
    response=init=>new Response(new ReadableStream({start(c){writer=c;c.enqueue(new TextEncoder().encode(event('start',1,{requestId:JSON.parse(init.body as string).requestId})));}}),{headers:{'Content-Type':'text/event-stream'}});
    const t=await setup();await t.fill('Em hỏi');await t.click('Gửi câu hỏi');
    expect(t.el.querySelector('.typing')?.textContent).toContain('Đang suy nghĩ…');
    if(terminal==='cancel')await t.click('Dừng');
    else {
      writer.enqueue(new TextEncoder().encode(terminal==='done'?event('done',2,{status:'completed'}):event('error',2,{message:'Chưa có lời đã kiểm.',retryable:true})));
      await settle();await t.f.whenStable();
    }
    expect(t.el.querySelector('.typing')).toBeNull();
    expect(t.el.querySelector('.cau-cua-ai')).toBeNull();
  });
  it('Escape khi đang sinh POST cancel đúng lượt, giữ câu và đóng panel',async()=>{
    response=init=>new Response(new ReadableStream({start(c){c.enqueue(new TextEncoder().encode(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'Câu đã kiểm.'})));}}),{headers:{'Content-Type':'text/event-stream'}});
    const t=await setup();await t.fill('Em hỏi');await t.click('Gửi câu hỏi');
    expect(t.el.querySelector('article.cau-cua-ai')?.textContent).toContain('Câu đã kiểm');
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));await settle();await t.f.whenStable();
    const cancel=fetchMock.mock.calls.find(c=>String(c[0]).endsWith('/cancel'))!;
    expect(JSON.parse(cancel[1].body)).toEqual({turnId:'t1'});expect(t.el.querySelector('[data-testid="gia-su-panel"]')).toBeNull();
  });
  it('nút gửi nhận câu hỏi tới 2000 ký tự theo Ask, chặn câu dài hơn',async()=>{
    const t=await setup();
    const send=()=>[...t.el.querySelectorAll('button')].find(b=>b.textContent?.trim()==='Gửi câu hỏi')!;
    await t.fill('x'.repeat(2001));
    expect(send().disabled).toBe(true);
    await t.fill('x'.repeat(2000));
    expect(send().disabled).toBe(false);
    await t.click('Gửi câu hỏi');
    expect(JSON.parse(fetchMock.mock.calls[2][1].body)).toEqual({requestId:expect.any(String),maBuoc:'B.DH.TXD',cap:1,cauHoi:'x'.repeat(2000)});
  });
  it('Hỏi lại giữ câu để sửa, không tự POST; Thử lại không lặp câu hỏi',async()=>{
    response=init=>new Response(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('error',2,{message:'Chưa có lời đã kiểm.',retryable:true}),{headers:{'Content-Type':'text/event-stream'}});
    const t=await setup();await t.fill('Câu hỏi của em');await t.click('Gửi câu hỏi');
    await t.click('Hỏi lại');expect(t.el.querySelector('textarea')!.value).toBe('Câu hỏi của em');expect(fetchMock).toHaveBeenCalledTimes(3);
    response=init=>new Response(event('start',3,{turnId:'t2',requestId:JSON.parse(init.body as string).requestId})+event('delta',4,{turnId:'t2',sentenceIndex:1,text:'Câu mới đã kiểm.'})+event('done',5,{turnId:'t2',status:'completed'}),{headers:{'Content-Type':'text/event-stream'}});
    await t.click('Thử lại');
    expect(t.el.querySelectorAll('.cau-cua-em')).toHaveLength(1);expect(t.el.textContent).toContain('Câu mới đã kiểm.');
  });
  it('đổi độ rộng bằng bàn phím, giới hạn 360–480',async()=>{
    const t=await setup();const h=t.el.querySelector<HTMLElement>('[role="separator"]')!;
    for(let i=0;i<30;i++)h.dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowLeft',bubbles:true}));await t.f.whenStable();expect(h.getAttribute('aria-valuenow')).toBe('480');
    for(let i=0;i<30;i++)h.dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowRight',bubbles:true}));await t.f.whenStable();expect(h.getAttribute('aria-valuenow')).toBe('360');
  });
  it('em cuộn lên thì delta không kéo xuống đáy; nút Xuống tiếp tục theo dõi',async()=>{
    let writer!: ReadableStreamDefaultController<Uint8Array>;
    response=init=>new Response(new ReadableStream({start(c){writer=c;c.enqueue(new TextEncoder().encode(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'Câu đầu. '})));}}),{headers:{'Content-Type':'text/event-stream'}});
    const t=await setup();await t.fill('Em hỏi');await t.click('Gửi câu hỏi');
    const thread=t.el.querySelector<HTMLElement>('.tutor-messages')!;
    Object.defineProperties(thread,{scrollHeight:{value:1200,configurable:true},clientHeight:{value:300,configurable:true}});
    thread.scrollTop=200;thread.dispatchEvent(new Event('scroll'));await t.f.whenStable();
    writer.enqueue(new TextEncoder().encode(event('delta',3,{sentenceIndex:2,text:'Câu sau.'})));await settle();await t.f.whenStable();
    expect(thread.scrollTop).toBe(200);expect(t.el.querySelector('[data-testid="gia-su-xuong"]')).not.toBeNull();
    await t.click('Xuống');expect(thread.scrollTop).toBe(1200);
    writer.enqueue(new TextEncoder().encode(event('done',4,{status:'completed'})));await settle();await t.f.whenStable();
  });
});

describe('Markdown gia sư',()=>{
  it('sanitize HTML, không ảnh hay URL model, toán/MathML và nguồn đúng snapshot',async()=>{
    const f=TestBed.createComponent(GiaSuMarkdown);
    f.componentRef.setInput('text','**Nhấn mạnh**\n\n- Xét $x^2$ và \\(f\\prime\\).\n- Nguồn [1].\n\n$$\\frac{1}{x}$$\n\n<img src="https://outside.invalid/a" onerror="alert(1)"><script>alert(1)</script> [liên kết](javascript:alert) ![ảnh](https://outside.invalid/b)');
    f.componentRef.setInput('references',[{index:1,text:'Dòng đã khóa của lượt cũ.',latex:'x^2'}]);
    f.detectChanges();await f.whenStable();const el=f.nativeElement as HTMLElement;
    expect(el.querySelector('strong')?.textContent).toBe('Nhấn mạnh');expect(el.querySelectorAll('li')).toHaveLength(2);
    expect(el.querySelectorAll('img,script,a,[onerror]')).toHaveLength(0);expect(el.querySelectorAll('.katex')).toHaveLength(3);expect(el.querySelector('math')).not.toBeNull();
    el.querySelector<HTMLButtonElement>('.tutor-source')!.click();await f.whenStable();
    expect(el.querySelector('[role="note"]')?.textContent).toContain('Dòng đã khóa của lượt cũ.');
  });
  it('cấu trúc chưa kín và LaTeX lỗi vẫn giữ chữ, code không bị render thành toán',async()=>{
    const f=TestBed.createComponent(GiaSuMarkdown);
    f.componentRef.setInput('text','Câu đã kiểm **chưa kín $x +\n\n`$x$`\n\n$\\frac{$');
    f.detectChanges();await f.whenStable();const el=f.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Câu đã kiểm');expect(el.querySelector('code')?.textContent).toBe('$x$');expect(el.querySelector('.katex-error')).not.toBeNull();
  });
});
