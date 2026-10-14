import type { Page } from '@playwright/test';
import { test, expect, flat, noOverflow, tutorSession } from './fixtures';

interface StreamMock {
  calls: { url:string; method:string; body: Record<string,unknown> | null; headers:Record<string,string> }[];
  emit(name:string,seq:number,extra?:Record<string,unknown>):void;
  disconnect():void;
}

declare global { interface Window { tutorStream: StreamMock } }

async function installStream(page:Page, startSeq=0, savedTurnId='', savedSentenceIndex=0) {
  // route.fulfill buffers its body. A controlled fetch ReadableStream exercises actual
  // incremental TextDecoder/reader behavior in Chromium without running a backend.
  await page.addInitScript(({startSeq,savedTurnId,savedSentenceIndex})=>{
    const nativeFetch=window.fetch.bind(window);
    let writer:ReadableStreamDefaultController<Uint8Array>;
    let turnId=savedTurnId,requestId='',seq=startSeq,sentenceIndex=savedSentenceIndex;
    const harness:StreamMock={
      calls:[],
      emit(name,n,extra={}){
        if(name==='delta' && n>seq)sentenceIndex=Number(extra['sentenceIndex']);
        const body=name==='heartbeat' ? {sessionId:'e2e-session',turnId,lastSeq:n,...extra}
          : {v:2,sessionId:'e2e-session',turnId,seq:n,...(name==='done'?{lastSentenceIndex:sentenceIndex,tokens:10,durationMs:20}:{}),...extra};
        const raw=`${name==='heartbeat'?'':`id: e2e-session:${n}\r\n`}event: ${name}\r\ndata: ${JSON.stringify(body)}\r\n\r\n`;
        // Split every UTF-8 byte, including diacritics and CRLF.
        for(const b of new TextEncoder().encode(raw))writer.enqueue(new Uint8Array([b]));
        if(name!=='heartbeat')seq=Math.max(seq,n);
      },
      disconnect(){writer.close();},
    };
    window.tutorStream=harness;
    window.fetch=async(input,init={})=>{
      const url=String(input),method=init.method??'GET';
      const target=url.startsWith('/api/hs/gia-su/v2/phien/e2e-session/') && (url.includes('/su-kien?') || (url.endsWith('/luot') && method==='POST'));
      if(!target)return nativeFetch(input,init);
      const body=init.body?JSON.parse(String(init.body)):null;
      harness.calls.push({url,method,body,headers:Object.fromEntries(new Headers(init.headers))});
      if(body){requestId=body.requestId;turnId='turn-'+requestId;sentenceIndex=0;}
      const stream=new ReadableStream<Uint8Array>({
        start(c){writer=c;if(body)harness.emit('start',++seq,{requestId,maBuoc:body.maBuoc,cap:body.cap,contextVersion:'1:synthetic'});},
      });
      return new Response(stream,{headers:{'Content-Type':'text/event-stream'}});
    };
  },{startSeq,savedTurnId,savedSentenceIndex});
}
async function open(page:Page){
  await page.goto('/hs/luyen/E2E-01');await page.getByTestId('mo-gia-su').click();
  const panel=page.getByTestId('gia-su-panel');await expect(panel).toBeVisible();return panel;
}
async function ask(page:Page,q='Câu hỏi tổng hợp của em'){
  await page.getByTestId('gia-su-composer').fill(q);await page.getByRole('button',{name:'Gửi câu hỏi',exact:true}).click();
  await expect.poll(()=>page.evaluate(()=> window.tutorStream.calls.filter(c=>c.method==='POST').length)).toBeGreaterThan(0);
}
async function emit(page:Page,name:string,seq:number,extra:Record<string,unknown>={}){
  await page.evaluate(({name,seq,extra})=>window.tutorStream.emit(name,seq,extra),{name,seq,extra});
}

test('SSE: tải lịch sử, delta tăng dần, Markdown/KaTeX, sanitize, regen, heartbeat và seq trùng',async({page})=>{
  await installStream(page,3);
  await page.route('**/api/hs/gia-su/v2/phien/e2e-session/luot*',async route=>{
    await route.fulfill({json:{session:tutorSession(3),turns:[{requestId:'history-r',id:'history-t',sessionId:'e2e-session',ordinal:1,step:'B.DH.TXD',level:1,question:'Câu hỏi đã lưu',answer:'**Lịch sử từ máy chủ**.',status:'completed',createdAt:'2026-10-14T00:00:00Z'}]}});
  });
  const panel=await open(page);await expect(panel.locator('strong')).toContainText('Lịch sử từ máy chủ');
  await ask(page);
  await emit(page,'delta',5,{sentenceIndex:1,text:'**Câu tiếng Việt đã kiểm** với $x^2$. [1]\n\n- Một dòng đã kiểm.\n'});
  await expect(panel.locator('.cau-cua-ai')).toHaveCount(2);
  await expect(panel.locator('.katex').first()).toBeVisible();await expect(panel.locator('math').first()).toBeAttached();
  await expect(panel.getByTestId('gia-su-dung')).toBeVisible(); // Before done.
  await expect(panel.getByRole('button',{name:'Mở nguồn 1'})).toHaveCount(0);
  await emit(page,'regen',6,{attempt:1,mode:'rewrite'});await expect(panel.locator('.typing')).toContainText('Đang viết lại');
  await emit(page,'heartbeat',999,{lastSeq:999});
  await emit(page,'delta',5,{sentenceIndex:1,text:'**Câu tiếng Việt đã kiểm** với $x^2$. [1]\n\n- Một dòng đã kiểm.\n'});
  await emit(page,'delta',7,{sentenceIndex:2,text:'- Dòng thứ hai.\n\n$$\\frac{x}{2}$$\n\n<script>alert(1)</script><img src="https://outside.invalid/x"> [liên kết](https://outside.invalid)'});
  await emit(page,'done',8,{status:'completed'});
  await expect(panel.getByTestId('gia-su-dung')).toHaveCount(0);
  await expect(panel.locator('li')).toHaveCount(2);await expect(panel.locator('.tutor-math-block')).toBeVisible();
  await expect(panel.locator('.markdown img,.markdown script,.markdown a')).toHaveCount(0);
  await expect(panel.locator('.markdown strong').filter({hasText:'Câu tiếng Việt đã kiểm'})).toHaveCount(1);
  await flat(page);await noOverflow(page);
});

test('SSE: EOF nối lại lastSeq và Last-Event-ID, không POST hay lặp câu',async({page})=>{
  await installStream(page);const panel=await open(page);await ask(page);
  await emit(page,'delta',2,{sentenceIndex:1,text:'Câu đầu đã kiểm. '});
  await expect(panel.locator('.cau-cua-ai')).toContainText('Câu đầu đã kiểm');
  await page.evaluate(()=>window.tutorStream.disconnect());
  await expect.poll(()=>page.evaluate(()=>window.tutorStream.calls.filter(c=>c.method==='GET').length)).toBe(1);
  const calls=await page.evaluate(()=>window.tutorStream.calls);
  expect(calls[1].url).toContain('/su-kien?lastSeq=2');expect(calls[1].headers['last-event-id']).toBe('e2e-session:2');
  await emit(page,'delta',2,{sentenceIndex:1,text:'Câu đầu đã kiểm. '});
  await emit(page,'delta',3,{sentenceIndex:2,text:'Câu sau đã kiểm.'});await emit(page,'done',4,{status:'completed'});
  await expect(panel.locator('.cau-cua-ai')).toHaveText('Gia sư trả lờiCâu đầu đã kiểm. Câu sau đã kiểm.');
  expect(calls.filter(c=>c.method==='POST')).toHaveLength(1);
});

test('SSE: snapshot generating nối lại từ session.lastSeq và giữ answer đã lưu',async({page,api})=>{
  api.tutorSeq=2;
  api.tutorTurns=[{id:'restored-turn',sessionId:'e2e-session',requestId:'restored-request',ordinal:1,step:'B.DH.TXD',level:2,question:'Câu hỏi đang viết',answer:'**Phần đã lưu** $x^2$. ',status:'generating',createdAt:'2026-10-14T00:00:00Z'}];
  await installStream(page,2,'restored-turn',1);
  const panel=await open(page);
  await expect(panel.locator('strong')).toHaveText('Phần đã lưu');
  await expect.poll(()=>page.evaluate(()=>window.tutorStream.calls.length)).toBe(1);
  const calls=await page.evaluate(()=>window.tutorStream.calls);
  expect(calls[0].method).toBe('GET');
  expect(calls[0].url).toContain('/su-kien?lastSeq=2');
  expect(calls[0].headers['last-event-id']).toBe('e2e-session:2');
  await emit(page,'delta',3,{sentenceIndex:2,text:'Phần tiếp theo đã kiểm.'});
  await emit(page,'done',4,{status:'completed'});
  await expect(panel.locator('.cau-cua-ai')).toContainText('Phần tiếp theo đã kiểm.');
  await expect(panel.locator('strong')).toHaveCount(1);
  await expect(panel.getByTestId('gia-su-dung')).toHaveCount(0);
  expect(api.calls.filter(c=>c.path.endsWith('/luot') && c.method==='POST')).toEqual([]);
});

test('SSE: lỗi giữ phần đã kiểm; Thử lại tạo lượt liên kết và Hỏi lại chỉ sửa câu',async({page})=>{
  await installStream(page);const panel=await open(page);await ask(page,'Câu hỏi cần giữ lại');
  await emit(page,'delta',2,{sentenceIndex:1,text:'Phần đã kiểm còn giữ.'});await emit(page,'error',3,{code:'MODEL_TIMEOUT',message:'Chưa hoàn tất. Hãy thử lại.',retryable:true});
  await expect(panel.getByRole('alert')).toContainText('chưa hoàn tất');
  await panel.getByRole('button',{name:'Hỏi lại',exact:true}).click();await expect(page.getByTestId('gia-su-composer')).toHaveValue('Câu hỏi cần giữ lại');
  expect(await page.evaluate(()=>window.tutorStream.calls.length)).toBe(1);
  await panel.getByRole('button',{name:'Thử lại',exact:true}).click();
  await expect.poll(()=>page.evaluate(()=>window.tutorStream.calls.length)).toBe(2);
  const calls=await page.evaluate(()=>window.tutorStream.calls);
  expect(calls[1].body).toEqual({requestId:expect.any(String),maBuoc:calls[0].body?.['maBuoc'],cap:calls[0].body?.['cap'],cauHoi:'Câu hỏi cần giữ lại'});
  expect(calls[1].body?.['requestId']).not.toBe(calls[0].body?.['requestId']);
  await emit(page,'delta',5,{sentenceIndex:1,text:'Lượt mới đã kiểm.'});await emit(page,'done',6,{status:'completed'});
  await expect(panel.locator('.cau-cua-em')).toHaveCount(1);await expect(panel.locator('.cau-cua-ai')).toHaveCount(2);
});

test('SSE: cancel phải có ACK; Escape đóng và trả focus; sheet không mở lại khi tải trang',async({page,api})=>{
  await installStream(page);
  let release!:(()=>void);const wait=new Promise<void>(r=>release=r);
  await page.route('**/api/hs/gia-su/v2/phien/e2e-session/cancel',async route=>{
    api.calls.push({method:'POST',path:'/api/hs/gia-su/v2/phien/e2e-session/cancel',body:route.request().postDataJSON()});
    await wait;await route.fulfill({json:{sessionId:'e2e-session',turnId:route.request().postDataJSON().turnId,status:'cancelled',lastSeq:3}});
  });
  const panel=await open(page);await ask(page);await emit(page,'delta',2,{sentenceIndex:1,text:'Câu đã kiểm trước dừng.'});
  await panel.getByTestId('gia-su-dung').click();await expect(panel.locator('.typing')).toContainText('Đang xác nhận dừng');
  await expect(panel.getByText('Đã dừng lượt này.',{exact:true})).toHaveCount(0);
  release();await expect(panel.getByText('Đã dừng lượt này.',{exact:true})).toBeVisible();
  expect(api.calls.find(c=>c.path.endsWith('/cancel'))!.body.turnId).toBe('turn-'+(await page.evaluate(()=>window.tutorStream.calls[0].body?.['requestId'])));
  await expect(panel.locator('.cau-cua-ai')).toContainText('Câu đã kiểm trước dừng');
  await page.keyboard.press('Escape');await expect(panel).toHaveCount(0);await expect(page.getByTestId('mo-gia-su')).toBeFocused();
  await page.getByTestId('mo-gia-su').click();await expect(panel).toBeVisible();
  await page.reload();
  if(page.viewportSize()!.width===390)await expect(panel).toHaveCount(0);
  else await expect(panel).toBeVisible();
  await flat(page);await noOverflow(page);
});
