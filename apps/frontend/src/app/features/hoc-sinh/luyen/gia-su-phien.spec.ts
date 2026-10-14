import { TestBed } from '@angular/core/testing';
import { API_GIA_SU, TurnGiaSuV2, SessionGiaSuV2 } from '../../../api/gia-su';
import { Phien } from '../../../core/auth/phien';
import { GiaSuPhien, NOI_LAI_GIA_SU } from './gia-su-phien';
import { docSse } from './gia-su-sse';

const session = (id='s1', lastSeq=0): SessionGiaSuV2 => ({id,studentId:'synthetic-student',assignmentId:'assignment-1',classId:'synthetic-class',tomTat:'',summaryThrough:0,contextVersion:'1:synthetic',lastSeq,revision:1,tokenCount:0});
const base = `${API_GIA_SU}/s1`;
const json = (body: unknown, status = 200, headers: HeadersInit = {}) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type':'application/json', ...headers } });
const eventFields = (name: string) => name === 'start' ? {maBuoc:'B.DH.TXD',cap:1,contextVersion:'1:synthetic'}
  : name === 'regen' ? {attempt:1,mode:'rewrite'} : name === 'done' ? {lastSentenceIndex:1,tokens:10,durationMs:20}
  : name === 'error' ? {code:'MODEL_UNAVAILABLE'} : {};
const event = (name: string, seq: number, data: object = {}) => `id: s1:${seq}\r\nevent: ${name}\r\ndata: ${JSON.stringify({ v:2, ...eventFields(name), sessionId:'s1', turnId:'t1', seq, ...data })}\r\n\r\n`;
const sse = (text: string) => new Response(text, { headers: { 'Content-Type':'text/event-stream' } });
const settle = async () => { await new Promise(resolve => setTimeout(resolve, 5)); };
const saved: TurnGiaSuV2 = { requestId:'old-r', id:'old-t', sessionId:'s1', ordinal:1, step:'B.DH.TXD', level:1, question:'Câu hỏi trước', answer:'Lịch sử đã kiểm.', status:'completed', createdAt:'2026-10-14T00:00:00Z' };

describe('Parser SSE v2', () => {
  it('giữ frame khi cắt ở mọi vị trí CRLF, JSON, dấu tiếng Việt và LaTeX', () => {
    const raw = ': keepalive\r\n\r\n' + event('delta', 2, { sentenceIndex:1, text:'Xét $\\frac{x}{2}$ ở đây.' });
    for (let i=0; i<=raw.length; i++) {
      const first = docSse(raw.slice(0,i));
      const second = docSse(first.rest + raw.slice(i));
      expect([...first.events,...second.events]).toHaveLength(1);
      expect([...first.events,...second.events][0]).toMatchObject({event:'delta',data:{text:'Xét $\\frac{x}{2}$ ở đây.'}});
    }
  });
  it('hỗ trợ LF, CR, data nhiều dòng, comment và giữ phần cuối thiếu', () => {
    const parsed = docSse(': ping\n\nevent: heartbeat\ndata: {"sessionId":"s1",\ndata: "turnId":"t1","lastSeq":8}\n\nevent: delta\rdata: {\"v\":2,\"sessionId\":\"s1\",\"turnId\":\"t1\",\"seq\":2,\"sentenceIndex\":1,\"text\":\"Đã kiểm.\"}\r\revent: done\ndata:');
    expect(parsed.events.map(e=>e.event)).toEqual(['heartbeat','delta']);
    expect(parsed.events[0]).toMatchObject({event:'heartbeat',data:{lastSeq:8}});
    expect(parsed.rest).toBe('event: done\ndata:');
    expect(()=>docSse('event: delta\ndata: broken\n\n')).toThrow();
    expect(docSse('event: done\rdata: {}\r\r', true).events[0].event).toBe('done');
    expect(docSse('event: done\ndata: {}', true).events).toEqual([]);
  });
});

describe('Phiên gia sư v2', () => {
  let service: GiaSuPhien;
  let handler: (url: string, init: RequestInit) => Response | Promise<Response>;
  let fetchMock: ReturnType<typeof vi.fn>;
  let refresh: ReturnType<typeof vi.fn>;
  let token: string;
  beforeEach(() => {
    token='first-token';
    refresh=vi.fn(async()=>{ token='new-token'; return true; });
    TestBed.configureTestingModule({ providers:[GiaSuPhien, {provide:NOI_LAI_GIA_SU,useValue:[0,0]}, {provide:Phien,useValue:{tokenTruyCap:()=>token,lamMoi:refresh}}] });
    service=TestBed.inject(GiaSuPhien);
    handler=()=>{ throw new Error('Chưa có mock'); };
    fetchMock=vi.fn(async(url: string, init: RequestInit = {})=>{
      if(url===API_GIA_SU) return json({sessionId:'s1',lastSeq:0,turnId:null,status:'idle'});
      if(url===`${base}/luot?offset=0&limit=50` && !init.method) return json({session:session('s1',0),turns:[]});
      return handler(url,init);
    });
    vi.stubGlobal('fetch',fetchMock);
  });
  afterEach(()=>{service.dispose(); vi.unstubAllGlobals();});

  it('ánh xạ history theo session.id/lastSeq, đảo thứ tự và phân trang offset/limit', async()=>{
    const recent = Array.from({length:50}, (_, i) => ({...saved, id:'t-'+(50-i), requestId:'r-'+(50-i), ordinal:50-i}));
    fetchMock.mockImplementation(async(url:string, init:RequestInit = {})=>{
      if(url===API_GIA_SU) {expect(JSON.parse(init.body as string)).toEqual({giaoBaiId:'assignment-1'});return json({sessionId:'s1',lastSeq:90,turnId:'t-50',status:'completed'});}
      if(url===`${base}/luot?offset=0&limit=50`) return json({session:session('s1',4),turns:recent});
      return json({session:session('s1',999),turns:[saved]});
    });
    await service.mo('assignment-1');
    expect(service.luot()[0]).toMatchObject({turnId:'t-1',maBuoc:saved.step,cap:saved.level,cauHoi:saved.question,sentences:[{text:saved.answer}],status:'completed'});
    expect(service.luot().at(-1)?.turnId).toBe('t-50');
    await service.taiCu();
    expect(service.luot()[0].turnId).toBe('old-t');
    expect(fetchMock.mock.calls[2][0]).toBe(`${base}/luot?offset=50&limit=50`);
    expect(service.coLichSuCu()).toBe(false);
    handler=(_url,init)=>init.method==='POST'
      ? sse(event('start',5,{requestId:JSON.parse(init.body as string).requestId})+event('done',6,{status:'completed'}))
      : sse('');
    fetchMock.mockImplementation(handler);
    await service.hoi('Em hỏi','B.DH.TXD',1);
    expect(service.luot().at(-1)?.status).toBe('completed');
  });

  it('delta hiện trước done; UTF-8 cắt từng byte, regen/heartbeat không chèn lời, loại seq trùng', async()=>{
    let writer!: ReadableStreamDefaultController<Uint8Array>;
    handler=(_url,init)=>{
      const req=JSON.parse(init.body as string);
      return new Response(new ReadableStream({start(c){writer=c;c.enqueue(new TextEncoder().encode(event('start',1,{requestId:req.requestId})));}}),{headers:{'Content-Type':'text/event-stream'}});
    };
    await service.mo('assignment-1');
    const done=service.hoi('Em cần giúp', 'B.DH.TXD',1);
    await settle();
    const raw=event('delta',2,{sentenceIndex:1,text:'Câu đã kiểm $x^2$.'});
    for(const byte of new TextEncoder().encode(raw)) writer.enqueue(new Uint8Array([byte]));
    await settle();
    expect(service.luot()[0].sentences[0].text).toBe('Câu đã kiểm $x^2$.');
    expect(service.dangHoi()).toBe(true);
    writer.enqueue(new TextEncoder().encode(raw + event('regen',3) + 'event: heartbeat\ndata: {"sessionId":"s1","turnId":"t1","lastSeq":99}\n\n'));
    await settle();
    expect(service.trangThai()).toContain('viết lại');
    expect(service.luot()[0].sentences).toHaveLength(1);
    writer.enqueue(new TextEncoder().encode(event('done',4,{status:'completed',lastSentenceIndex:1,tokens:10,durationMs:20})));
    await done;
    expect(service.luot()[0].status).toBe('completed');
    expect(service.luot()[0].sentences).toHaveLength(1);
  });

  it('EOF nối lại GET từ last seq, loại replay trùng và không tạo lượt model mới', async()=>{
    handler=(url,init)=>{
      if(init.method==='POST') return sse(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'Câu một. '}));
      expect(url).toBe(`${base}/su-kien?lastSeq=2`);
      expect((init.headers as Record<string,string>)['Last-Event-ID']).toBe('s1:2');
      return sse(event('delta',2,{sentenceIndex:1,text:'Câu một. '})+event('delta',3,{sentenceIndex:2,text:'Câu hai.'})+event('done',4,{status:'completed'}));
    };
    await service.mo('assignment-1');
    await service.hoi('Em hỏi', 'B.DH.TXD',1);
    expect(service.luot()[0].sentences.map(s=>s.text)).toEqual(['Câu một. ','Câu hai.']);
    expect(fetchMock.mock.calls.filter(c=>c[0]===`${base}/luot` && c[1].method==='POST')).toHaveLength(1);
  });

  it('seq bị thiếu không được commit; hết backoff có Kết nối lại, giữ câu đã kiểm', async()=>{
    handler=(_url,init)=>init.method==='POST'
      ? sse(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'Đã kiểm.'})+event('done',4,{status:'completed'}))
      : sse(event('done',4,{status:'completed'}));
    await service.mo('assignment-1');
    await service.hoi('Em hỏi','B.DH.TXD',1);
    expect(service.matKetNoi()).toBe(true);
    expect(service.sanSang()).toBe(false);
    expect(service.luot()[0].status).toBe('generating');
    expect(service.luot()[0].sentences).toHaveLength(1);
    handler=()=>sse(event('done',3,{status:'completed'}));
    service.noiLai();await settle();
    expect(service.matKetNoi()).toBe(false);
    expect(service.sanSang()).toBe(true);
  });

  it('POST cancel riêng, giữ delta và không đánh dấu đã dừng trước ACK; ACK mất mạng thử lại đúng turn', async()=>{
    let ack!: (r:Response)=>void;
    let attempts=0;
    handler=(url,init)=>{
      if(url.endsWith('/cancel')) {
        expect(JSON.parse(init.body as string)).toEqual({turnId:'t1'});
        if(++attempts===1) return Promise.reject(new Error('Mất mạng'));
        return new Promise(resolve=>ack=resolve);
      }
      const req=JSON.parse(init.body as string);
      return new Response(new ReadableStream({start(c){c.enqueue(new TextEncoder().encode(event('start',1,{requestId:req.requestId})+event('delta',2,{sentenceIndex:1,text:'Đã kiểm.'})));}}),{headers:{'Content-Type':'text/event-stream'}});
    };
    await service.mo('assignment-1');
    const streaming=service.hoi('Em hỏi','B.DH.TXD',1);await settle();
    await service.dung();
    expect(service.choDung()).toBe(true);
    expect(service.luot()[0].status).toBe('generating');
    const stopping=service.dung();await settle();
    expect(service.trangThai()).toContain('xác nhận');
    ack(json({sessionId:'s1',turnId:'t1',status:'cancelled',lastSeq:3}));await stopping;await streaming;
    expect(service.luot()[0].status).toBe('cancelled');
    expect(service.luot()[0].sentences).toHaveLength(1);
    expect(service.sanSang()).toBe(true);
  });

  it('Dừng trước start đợi turn id, không hủy lượt khác', async()=>{
    let writer!: ReadableStreamDefaultController<Uint8Array>;
    let requestId='';
    handler=(url,init)=>{
      if(url.endsWith('/cancel')) return json({sessionId:'s1',turnId:'t1',status:'cancelled',lastSeq:2});
      requestId=JSON.parse(init.body as string).requestId;
      return new Response(new ReadableStream({start(c){writer=c;}}),{headers:{'Content-Type':'text/event-stream'}});
    };
    await service.mo('assignment-1');
    const streaming=service.hoi('Em hỏi','B.DH.TXD',1);await settle();
    await service.dung();
    expect(fetchMock.mock.calls.filter(c=>String(c[0]).endsWith('/cancel'))).toHaveLength(0);
    writer.enqueue(new TextEncoder().encode(event('start',1,{requestId})));await streaming;
    expect(service.luot()[0].status).toBe('cancelled');
  });

  it('cancel thua lượt đã hoàn tất thì replay phần thiếu, không coi ACK là toàn bộ lời trả lời', async()=>{
    handler=(url,init)=>{
      if(url.endsWith('/cancel')) return json({sessionId:'s1',turnId:'t1',status:'completed',lastSeq:4});
      if(init.method==='GET') {
        expect(url).toBe(`${base}/su-kien?lastSeq=2`);
        return sse(event('delta',3,{sentenceIndex:2,text:' Câu cuối đã kiểm.'})+event('done',4,{status:'completed'}));
      }
      return new Response(new ReadableStream({start(c){c.enqueue(new TextEncoder().encode(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'Câu đầu.'})));}}),{headers:{'Content-Type':'text/event-stream'}});
    };
    await service.mo('assignment-1');
    const streaming=service.hoi('Em hỏi','B.DH.TXD',1);await settle();await service.dung();await streaming;await settle();
    expect(service.luot()[0].status).toBe('completed');
    expect(service.luot()[0].sentences.map(s=>s.text).join('')).toBe('Câu đầu. Câu cuối đã kiểm.');
    expect(service.choDung()).toBe(false);
  });

  it.each([409,410])('cursor lỗi HTTP %i giữ lịch sử và yêu cầu tải lại snapshot', async(status)=>{
    handler=(_url,init)=>init.method==='POST'
      ? sse(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'Phần đã kiểm.'}))
      : json({},status);
    await service.mo('assignment-1');await service.hoi('Em hỏi','B.DH.TXD',1);
    expect(service.loi()).toContain('Tải lại phiên');expect(service.sanSang()).toBe(false);
    expect(service.luot()[0].sentences[0].text).toBe('Phần đã kiểm.');
  });

  it('error giữ phần đã kiểm; thử lại liên kết lượt lỗi, không tự gửi hay lặp câu hỏi', async()=>{
    let attempt=0;
    handler=(_url,init)=>{
      const req=JSON.parse(init.body as string);
      if(++attempt===1) return sse(event('start',1,{requestId:req.requestId})+event('delta',2,{sentenceIndex:1,text:'Phần đã kiểm.'})+event('error',3,{message:'Chưa hoàn tất. Hãy thử lại.',retryable:true}));
      expect(req).toEqual({requestId:expect.any(String),maBuoc:'B.DH.TXD',cap:2,cauHoi:'Em hỏi'});
      return sse(event('start',4,{requestId:req.requestId})+event('done',5,{status:'completed'}));
    };
    await service.mo('assignment-1');await service.hoi('Em hỏi','B.DH.TXD',2);
    expect(attempt).toBe(1);expect(service.luot()[0].sentences).toHaveLength(1);
    service.thuLai(service.luot()[0]);await settle();
    expect(service.luot()[1].retryOfTurnId).toBe('t1');
    expect(service.luot()[1].cap).toBe(2);
  });

  it('401 làm mới token trong Phien, 403 khóa composer; 429 tôn trọng Retry-After', async()=>{
    let count=0;
    handler=(_url,init)=>{
      if(++count===1) return json({},401);
      expect((init.headers as Record<string,string>)['Authorization']).toBe('Bearer new-token');
      return json({},429,{'Retry-After':'1'});
    };
    await service.mo('assignment-1');await service.hoi('Em hỏi','B.DH.TXD',1);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(service.retryAt()).toBeGreaterThan(Date.now());
    expect(service.sanSang()).toBe(false);
    await service.mo('assignment-2');
    handler=()=>json({},403);
    await service.hoi('Em hỏi','B.DH.TXD',1);
    expect(service.biKhoa()).toBe(true);
    expect(service.sanSang()).toBe(false);
  });

  it('snapshot đang sinh giữ answer và resume sau session.lastSeq, không lặp phần đã lưu', async()=>{
    fetchMock.mockImplementation(async(url:string,init:RequestInit={})=>{
      if(url===API_GIA_SU) return json({sessionId:'s1',lastSeq:0,turnId:null,status:'idle'});
      if(url.includes('/luot?')) return json({session:session('s1',2),turns:[{...saved,id:'t1',status:'generating',answer:'**Phần đã lưu**. '}]});
      expect(url).toBe(`${base}/su-kien?lastSeq=2`);
      expect((init.headers as Record<string,string>)['Last-Event-ID']).toBe('s1:2');
      return sse(event('delta',3,{sentenceIndex:2,text:'Phần tiếp theo $x^2$.'})+event('done',4,{status:'completed',lastSentenceIndex:2}));
    });
    await service.mo('assignment-1');await settle();
    expect(service.luot()[0].sentences.map(s=>s.text).join('')).toBe('**Phần đã lưu**. Phần tiếp theo $x^2$.');
    expect(service.luot()[0].status).toBe('completed');
    expect(fetchMock.mock.calls.some(c=>c[1].method==='POST' && String(c[0]).endsWith('/luot'))).toBe(false);
  });

  it('CancelAck error replay lời đã kiểm và lỗi gốc; ACK sai lượt không dừng lượt hiện tại', async()=>{
    let mismatched=true;
    handler=(url,init)=>{
      if(url.endsWith('/cancel')) return json({sessionId:'s1',turnId:mismatched?'other-turn':'t1',status:'error',lastSeq:4});
      if(init.method==='GET') return sse(event('delta',3,{sentenceIndex:2,text:' Phần cuối đã kiểm.'})+event('error',4,{code:'MODEL_UNAVAILABLE',message:'Lỗi từ máy chủ.',retryable:true}));
      return new Response(new ReadableStream({start(c){c.enqueue(new TextEncoder().encode(event('start',1,{requestId:JSON.parse(init.body as string).requestId})+event('delta',2,{sentenceIndex:1,text:'Phần đầu.'})));}}),{headers:{'Content-Type':'text/event-stream'}});
    };
    await service.mo('assignment-1');
    const streaming=service.hoi('Em hỏi','B.DH.TXD',1);await settle();await service.dung();
    expect(service.luot()[0].status).toBe('generating');expect(service.choDung()).toBe(true);
    mismatched=false;await service.dung();await streaming;await settle();
    expect(service.luot()[0].error).toEqual({message:'Lỗi từ máy chủ.',retryable:true});
    expect(service.luot()[0].sentences.map(s=>s.text).join('')).toBe('Phần đầu. Phần cuối đã kiểm.');
    expect(service.sanSang()).toBe(true);
  });

  it('khôi phục snapshot có lượt generating rồi resume; phản hồi bài cũ không chèn vào bài mới', async()=>{
    let resolveOld!: (r:Response)=>void;
    fetchMock.mockImplementation(async(url:string,init:RequestInit={})=>{
      if(url===API_GIA_SU) return json({sessionId:JSON.parse(init.body as string).giaoBaiId==='a1'?'s1':'s2',lastSeq:2,turnId:'t1',status:'generating'});
      if(url===`${base}/luot?offset=0&limit=50`) return json({session:session('s1',2),turns:[{...saved,requestId:'r1',id:'t1',status:'generating'}]});
      if(url===`${base}/su-kien?lastSeq=2`) return new Promise(resolve=>resolveOld=resolve);
      if(url===`${base}/cancel`) return json({sessionId:'s1',turnId:'t1',status:'cancelled',lastSeq:3});
      return json({session:session('s2',0),turns:[]});
    });
    await service.mo('a1');await settle();expect(service.dangHoi()).toBe(true);
    await service.mo('a2');
    resolveOld(sse(event('delta',3,{sentenceIndex:2,text:'Không được chèn.'})+event('done',4,{status:'completed'})));
    await settle();expect(service.luot()).toEqual([]);
  });
});
