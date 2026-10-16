import { computed, inject, Injectable, InjectionToken, signal } from '@angular/core';
import { API_GIA_SU, LichSuGiaSuV2, LuotGiaSuV2, PhienGiaSuV2, TurnGiaSuV2, HoiGiaSuV2, HuyGiaSuV2, XacNhanHuyGiaSuV2, SuKienSse } from '../../../api/gia-su';
import { Phien } from '../../../core/auth/phien';
import { docSse } from './gia-su-sse';

export const NOI_LAI_GIA_SU = new InjectionToken<readonly number[]>('Thời gian nối lại gia sư', {
  factory: () => [250, 750],
});
class LoiHttp extends Error {
  constructor(readonly status: number, readonly retryAt = 0, message = '', readonly retryable = status >= 500) {
    super(message || (status === 400 ? 'Câu hỏi hoặc bước hiện tại chưa hợp lệ. Em tải lại bài rồi thử lại.' : status === 413 ? 'Nội dung quá dài. Em rút gọn rồi thử lại.' : status === 401 ? 'Phiên đăng nhập đã hết. Hãy đăng nhập lại.'
      : status === 404 ? 'Không tìm thấy bài được giao. Hãy quay lại danh sách bài.'
      : status === 403 ? 'Gia sư không mở cho bài này.' : status === 429
      ? 'Đã hết lượt hỏi lúc này. Chờ một chút rồi thử lại.'
      : 'Chưa kết nối được gia sư. Kiểm tra kết nối rồi thử lại.'));
  }
}
interface KetNoi {
  epoch: number;
  sessionId: string;
  requestId: string;
  turnId: string;
  abort: AbortController;
  terminal: boolean;
  stopping: boolean;
  cancelRequest?: Promise<void>;
  pending?: HoiGiaSuV2;
}

/** Một instance cho mỗi sidebar. Chat và token chỉ ở bộ nhớ. */
@Injectable()
export class GiaSuPhien {
  readonly luot = signal<readonly LuotGiaSuV2[]>([]);
  readonly dangTai = signal(false);
  readonly dangTaiCu = signal(false);
  readonly dangHoi = signal(false);
  readonly trangThai = signal('');
  readonly loi = signal('');
  readonly matKetNoi = signal(false);
  readonly choDung = signal(false);
  readonly biKhoa = signal(false);
  readonly retryAt = signal(0);
  readonly coLichSuCu = signal(false);
  readonly sanSang = computed(() => !!this.sessionId() && !this.dangTai() && !this.dangHoi()
    && !this.matKetNoi() && !this.choDung() && !this.biKhoa() && !this.retryAt());
  readonly thongBaoCau = signal<readonly { seq: number; text: string }[]>([]);
  private readonly auth = inject(Phien);
  private readonly backoff = inject(NOI_LAI_GIA_SU);
  private readonly sessionId = signal('');
  private readonly pageSize = 50;
  private historyOffset = 0;
  private lastSeq = 0;
  private epoch = 0;
  private active: KetNoi | null = null;
  private loadAbort: AbortController | null = null;
  private retryTimer: ReturnType<typeof setTimeout> | undefined;
  private assignment = '';
  private problem = '';

  async mo(giaoBaiId: string, maBai = ''): Promise<void> {
    // Mở lại sau đóng cũng tải snapshot; không khởi tạo một lượt model mới.
    const epoch = ++this.epoch;
    const old = this.active;
    old?.abort.abort();
    this.active = null;
    this.loadAbort?.abort();
    const abort = this.loadAbort = new AbortController();
    if (this.assignment !== giaoBaiId || this.problem !== maBai) {
      this.luot.set([]);
      clearTimeout(this.retryTimer);
      this.retryAt.set(0);
    }
    this.assignment = giaoBaiId;
    this.problem = maBai;
    this.sessionId.set('');
    this.lastSeq = 0;
    this.historyOffset = 0;
    this.coLichSuCu.set(false);
    this.dangTaiCu.set(false);
    this.loi.set('');
    this.biKhoa.set(false);
    this.matKetNoi.set(false);
    this.choDung.set(false);
    this.dangHoi.set(false);
    this.thongBaoCau.set([]);
    this.dangTai.set(true);
    try {
      if (!giaoBaiId && !maBai) throw new Error('Chưa mở được phiên gia sư. Hãy tải lại bài được giao.');
      const response = await this.request(API_GIA_SU, { method: 'POST', body: JSON.stringify(giaoBaiId ? { giaoBaiId } : { maBai }), signal: abort.signal });
      const session: PhienGiaSuV2 = await response.json();
      const historyResponse = await this.request(this.path(session.sessionId, 'luot') + `?offset=0&limit=${this.pageSize}`, { signal: abort.signal });
      const history: LichSuGiaSuV2 = await historyResponse.json();
      if (epoch !== this.epoch || abort.signal.aborted) return;
      if (history.session.id !== session.sessionId) throw new Error('Chưa tải được lịch sử. Bấm Tải lại phiên.');
      this.sessionId.set(session.sessionId);
      this.luot.set(this.mapHistory(history.turns));
      this.lastSeq = history.session.lastSeq;
      this.historyOffset = history.turns.length;
      this.coLichSuCu.set(history.turns.length === this.pageSize);
      const running = history.turns.find(t => t.status === 'generating');
      if (running) {
        const active = this.connection(session.sessionId, running.requestId, running.id);
        this.active = active;
        void this.stream(active);
      }
    } catch (error) {
      if (epoch === this.epoch && !abort.signal.aborted) this.showError(error);
    } finally {
      if (epoch === this.epoch) this.dangTai.set(false);
    }
  }

  async taiCu(): Promise<void> {
    const session = this.sessionId(), epoch = this.epoch;
    if (!this.coLichSuCu() || this.dangTaiCu()) return;
    this.dangTaiCu.set(true);
    try {
      const response = await this.request(this.path(session, 'luot') + `?offset=${this.historyOffset}&limit=${this.pageSize}`);
      const page: LichSuGiaSuV2 = await response.json();
      if (epoch !== this.epoch) return;
      if (page.session.id !== session) throw new Error('Chưa tải được lịch sử. Hãy thử lại.');
      this.luot.update(turns => [...this.mapHistory(page.turns).filter(t => !turns.some(x => x.turnId === t.turnId)), ...turns]);
      this.historyOffset += page.turns.length;
      this.coLichSuCu.set(page.turns.length === this.pageSize);
    } catch (error) { if (epoch === this.epoch) this.showError(error); }
    finally { if (epoch === this.epoch) this.dangTaiCu.set(false); }
  }

  async hoi(cauHoi: string, maBuoc: string, cap: number, retry?: LuotGiaSuV2, baiLamHienTai = ''): Promise<void> {
    if (!this.sanSang()) return;
    const requestId = crypto.randomUUID();
    const turn: LuotGiaSuV2 = { requestId, turnId: '', maBuoc, cap, cauHoi, text: '', live: true, status: 'generating',
      ...(retry ? { retryOfTurnId: retry.turnId || retry.requestId } : {}) };
    // Thử lại gắn với lượt lỗi, chỉ giữ một câu hỏi trong UI; phần đã nhận vẫn ở lượt cũ.
    this.luot.update(turns => [...turns, turn]);
    const active = this.connection(this.sessionId(), requestId, '');
    this.active = active;
    this.loi.set('');
    await this.stream(active, { requestId, maBuoc, cap, cauHoi, ...(baiLamHienTai ? { baiLamHienTai } : {}) });
  }

  thuLai(turn: LuotGiaSuV2, maBuoc = turn.maBuoc, baiLamHienTai = ''): void {
    if (turn.status === 'error' && turn.error?.retryable) void this.hoi(turn.cauHoi, maBuoc, turn.cap, turn, baiLamHienTai);
  }
  noiLai(): void {
    if (this.active && !this.active.terminal && !this.dangHoi()) void this.stream(this.active, this.active.pending);
  }
  async dung(): Promise<void> {
    const active = this.active;
    if (!active || active.terminal) return;
    active.stopping = true;
    this.choDung.set(true);
    this.trangThai.set('Đang xác nhận dừng…');
    // Chờ start cấp turnId; vẫn đọc stream để hủy đúng lượt nếu em dừng trước start.
    if (active.turnId) await this.huy(active);
    else if (!this.dangHoi()) this.noiLai();
  }
  dispose(): void {
    const old = this.active;
    ++this.epoch;
    old?.abort.abort();
    this.loadAbort?.abort();
    clearTimeout(this.retryTimer);
  }

  private connection(sessionId: string, requestId: string, turnId: string): KetNoi {
    return { epoch: this.epoch, sessionId, requestId, turnId, abort: new AbortController(), terminal: false, stopping: false };
  }
  private path(session: string, suffix: string): string { return `${API_GIA_SU}/${encodeURIComponent(session)}/${suffix}`; }
  private current(a: KetNoi): boolean { return this.active === a && a.epoch === this.epoch; }
  private update(a: KetNoi, change: (t: LuotGiaSuV2) => LuotGiaSuV2): void {
    this.luot.update(turns => turns
      .map(t => (a.requestId && t.requestId === a.requestId) || (a.turnId && t.turnId === a.turnId) ? change(t) : t)
      // Lượt dở từ trang trước: giữ chữ đã nhận, đánh dấu dừng giữa chừng (nhẹ), không hiện hộp lỗi.
      .filter(t => t.live || t.status === 'generating' || t.status === 'completed' || !!t.text.trim())
      .map(t => !t.live && (t.status === 'error' || t.status === 'cancelled') ? { ...t, status: 'cancelled' as const, error: undefined } : t));
  }

  private async huy(a: KetNoi): Promise<void> {
    if (!a.turnId || a.terminal) return;
    if (a.cancelRequest) return a.cancelRequest;
    a.stopping = true;
    const cancel = async () => {
      try {
        const response = await this.request(this.path(a.sessionId, 'cancel'), { method: 'POST', body: JSON.stringify({ turnId: a.turnId } satisfies HuyGiaSuV2) });
        const ack: XacNhanHuyGiaSuV2 = await response.json();
        if (ack.sessionId !== a.sessionId || ack.turnId !== a.turnId || !['cancelled', 'completed', 'error'].includes(ack.status) || !Number.isSafeInteger(ack.lastSeq)) throw new Error('Chưa xác nhận được việc dừng. Bấm Dừng để thử lại.');
        // Stream có thể đã nhận done trong lúc chờ ACK; không mở lại lượt đã xong.
        if (!this.current(a) || a.terminal) return;
        // ACK cancelled chỉ thay được done kế tiếp. Nếu còn delta ở giữa,
        // replay từ cursor đã nhận trước khi cho phép hỏi lượt mới.
        if (ack.status !== 'cancelled' || ack.lastSeq > this.lastSeq + 1) {
          a.stopping = false;
          if (this.current(a)) {
            // Worker đã dừng/xong nhưng client có thể chưa nhận hết delta/done.
            // Nối lại từ cursor đã nhận, không nhảy thẳng tới seq trong ACK.
            a.abort.abort();
            const replay = this.connection(a.sessionId, a.requestId, a.turnId);
            this.active = replay;
            this.choDung.set(false);
            this.dangHoi.set(false);
            void this.stream(replay);
          }
          return;
        }
        a.terminal = true;
        a.abort.abort();
        if (this.current(a)) {
          this.lastSeq = Math.max(this.lastSeq, ack.lastSeq);
          this.update(a, t => ({ ...t, status: 'cancelled' }));
          this.dangHoi.set(false);
          this.matKetNoi.set(false);
          this.choDung.set(false);
          this.loi.set('');
          this.trangThai.set('');
        }
      } catch (error) {
        if (this.current(a) && !a.terminal) {
          this.choDung.set(true);
          this.trangThai.set('Đang xác nhận dừng…');
          this.showError(error);
        }
      } finally { a.cancelRequest = undefined; }
    };
    a.cancelRequest = cancel();
    return a.cancelRequest;
  }

  private async stream(a: KetNoi, body?: HoiGiaSuV2): Promise<void> {
    if (!this.current(a) || a.terminal || this.dangHoi()) return;
    this.dangHoi.set(true);
    this.matKetNoi.set(false);
    this.trangThai.set(a.stopping ? 'Đang xác nhận dừng…' : body ? 'Đang suy nghĩ…' : 'Đang kết nối lại…');
    a.pending = body;
    let attempt = 0;
    try {
      while (this.current(a) && !a.terminal) {
        try {
          const url = body ? this.path(a.sessionId, 'luot') : this.path(a.sessionId, 'su-kien') + '?lastSeq=' + this.lastSeq;
          const response = await this.request(url, {
            method: body ? 'POST' : 'GET',
            ...(body ? { body: JSON.stringify(body) } : { headers: { 'Last-Event-ID': `${a.sessionId}:${this.lastSeq}` } }),
            signal: a.abort.signal,
          });
          if (!response.body || !response.headers.get('Content-Type')?.includes('text/event-stream')) throw new Error('Kết nối bị gián đoạn.');
          if (this.current(a)) this.loi.set('');
          body = undefined;
          a.pending = undefined;
          await this.read(response.body, a);
          if (!a.terminal) throw new Error('Kết nối bị gián đoạn.');
        } catch (error) {
          if (!this.current(a) || a.terminal || a.abort.signal.aborted) break;
          if (error instanceof LoiHttp) {
            if (error.status === 409 || error.status === 410) {
              this.loi.set('Phiên đã thay đổi. Bấm Tải lại phiên để tiếp tục.');
              this.sessionId.set('');
              break;
            }
            if (!error.retryable || attempt >= this.backoff.length) {
              a.terminal = true;
              this.update(a, t => ({ ...t, status: 'error', error: { message: error.message, retryable: error.retryable || error.status === 429 } }));
              this.showError(error);
              this.choDung.set(false);
              break;
            }
          }
          // Trước khi có phản hồi, gửi lại cùng requestId: core nhận idempotent.
          // Khi đã có SSE, body đã xóa và chỉ replay từ lastSeq.
          if (attempt >= this.backoff.length) {
            this.matKetNoi.set(true);
            this.loi.set('Kết nối bị gián đoạn. Bấm Kết nối lại để tiếp tục.');
            break;
          }
          this.trangThai.set(a.stopping ? 'Đang xác nhận dừng…' : 'Đang kết nối lại…');
          await this.pause(this.backoff[attempt++], a.abort.signal);
        }
      }
    } finally {
      if (this.current(a)) { this.dangHoi.set(false); if (!this.choDung()) this.trangThai.set(''); }
    }
  }

  private async read(body: ReadableStream<Uint8Array>, a: KetNoi): Promise<void> {
    const reader = body.getReader(), decoder = new TextDecoder();
    const abortRead = () => { void reader.cancel().catch(() => undefined); };
    a.abort.signal.addEventListener('abort', abortRead, { once: true });
    let buffer = '';
    try {
      while (this.current(a) && !a.terminal && !a.abort.signal.aborted) {
        const idle = setTimeout(() => void reader.cancel(), 45_000);
        let chunk: ReadableStreamReadResult<Uint8Array>;
        try { chunk = await reader.read(); } finally { clearTimeout(idle); }
        buffer += chunk.done ? decoder.decode() : decoder.decode(chunk.value, { stream: true });
        const parsed = docSse(buffer, chunk.done);
        buffer = parsed.rest;
        for (const event of parsed.events) {
          if (!this.current(a) || a.terminal) break;
          this.event(event, a);
        }
        if (chunk.done) break;
      }
    } finally {
      a.abort.signal.removeEventListener('abort', abortRead);
      await reader.cancel().catch(() => undefined);
      reader.releaseLock();
    }
  }

  private event(event: SuKienSse, a: KetNoi): void {
    if (event.data.sessionId !== a.sessionId) return;
    if (event.event === 'heartbeat') return; // lastSeq của heartbeat không phải cursor đã nhận.
    const d = event.data;
    if (d.v !== 2 || !Number.isSafeInteger(d.seq) || event.id !== `${a.sessionId}:${d.seq}`) throw new Error('Kết nối bị gián đoạn.');
    const seq = d.seq;
    if (seq <= this.lastSeq) return;
    if (seq !== this.lastSeq + 1) throw new Error('Kết nối bị gián đoạn.');
    if (!a.turnId && event.event === 'start' && event.data.requestId === a.requestId) {
      a.turnId = d.turnId;
      this.historyOffset++;
    }
    if (d.turnId !== a.turnId) throw new Error('Phiên đã thay đổi.');
    if (event.event === 'delta' && typeof event.data.text !== 'string') throw new Error('Kết nối bị gián đoạn.');
    if (event.event === 'done' && event.data.status !== 'completed' && event.data.status !== 'cancelled') throw new Error('Kết nối bị gián đoạn.');
    this.lastSeq = seq;
    switch (event.event) {
      case 'start':
        this.update(a, t => ({ ...t, turnId: d.turnId, references: event.data.references ?? t.references ?? [] }));
        if (a.stopping) void this.huy(a);
        break;
      case 'status':
        if (event.data.phase !== 'thinking') throw new Error('Kết nối bị gián đoạn.');
        if (!a.stopping && !this.luot().find(t => t.requestId === a.requestId)?.text) this.trangThai.set('Gia sư đang nghĩ…');
        break;
      case 'delta': {
        const { text } = event.data;
        this.update(a, t => ({ ...t, text: t.text + text }));
        this.thongBaoCau.update(cau => [...cau, { seq, text }]);
        if (!a.stopping) this.trangThai.set('Đang soạn lời gợi ý…');
        break;
      }
      case 'done':
        a.terminal = true;
        this.update(a, t => ({ ...t, status: event.data.status === 'cancelled' ? 'cancelled' : 'completed' }));
        this.choDung.set(false);
        break;
      case 'error': {
        const { message, retryable } = event.data;
        a.terminal = true;
        this.update(a, t => ({ ...t, status: 'error', error: { message, retryable: retryable === true } }));
        this.choDung.set(false);
        break;
      }
    }
  }

  private mapHistory(turns: readonly TurnGiaSuV2[]): LuotGiaSuV2[] {
    return [...turns].reverse()
      .filter(t => t.status === 'generating' || t.status === 'completed' || !!t.answer.trim())
      .map(t => ({
        requestId: t.requestId, turnId: t.id, maBuoc: t.step, cap: t.level, cauHoi: t.question, status: t.status === 'generating' || t.status === 'completed' ? t.status : 'cancelled',
        // Snapshot đã ghép delta đến session.lastSeq; chỉ nối chữ sau cursor này.
        text: t.answer, references: t.references ?? [],
      }));
  }

  private async request(url: string, init: RequestInit = {}): Promise<Response> {
    const req = () => fetch(url, { ...init, credentials: 'same-origin', headers: {
      'Content-Type': 'application/json', Authorization: 'Bearer ' + this.auth.tokenTruyCap(), ...init.headers,
    } });
    const token = this.auth.tokenTruyCap();
    if (!token) await this.auth.lamMoi();
    let response = await req();
    let authRetried = false;
    if (response.status === 401 && !init.signal?.aborted) {
      // Một request HttpClient khác có thể vừa làm mới trong lúc fetch còn dùng token cũ.
      if (this.auth.tokenTruyCap() !== token || await this.auth.lamMoi()) {
        authRetried = true;
        await response.body?.cancel();
        response = await req();
      }
    }
    if (!response.ok) {
      const after = response.headers.get('Retry-After');
      const delay = after && /^\d+$/.test(after) ? Number(after) * 1000 : after ? Date.parse(after) - Date.now() : 0;
      const detail = await response.json().catch(() => null);
      throw new LoiHttp(response.status, response.status === 429 ? Date.now() + Math.max(1000, delay || 1000) : 0, [400, 413, 422].includes(response.status) && typeof detail?.detail === 'string' ? detail.detail : '', response.status >= 500 || (response.status === 401 && authRetried));
    }
    return response;
  }
  private showError(error: unknown): void {
    this.loi.set(error instanceof Error ? error.message : 'Chưa kết nối được gia sư. Hãy thử lại.');
    if (error instanceof LoiHttp) {
      if ([401, 403, 404].includes(error.status)) this.biKhoa.set(true);
      if (error.retryAt) {
        clearTimeout(this.retryTimer);
        this.retryAt.set(error.retryAt);
        this.retryTimer = setTimeout(() => this.retryAt.set(0), Math.max(0, error.retryAt - Date.now()));
      }
    }
  }
  private pause(ms: number, signal: AbortSignal): Promise<void> {
    return new Promise(resolve => {
      const finish = () => { clearTimeout(timer); signal.removeEventListener('abort', finish); resolve(); };
      const timer = setTimeout(finish, ms);
      signal.addEventListener('abort', finish, { once: true });
      if (signal.aborted) finish();
    });
  }
}
