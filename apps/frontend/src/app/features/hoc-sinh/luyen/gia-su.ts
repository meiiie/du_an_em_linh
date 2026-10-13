import { NgTemplateOutlet } from '@angular/common';
import { afterNextRender, Component, DestroyRef, effect, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { form, FormField, maxLength } from '@angular/forms/signals';
import { Phien } from '../../../core/auth/phien';
import { Katex } from '../../../shared/toan/katex';
import { Button } from '../../../shared/ui/button';
export interface SuKienSse {
    event: string;
    data: {
        status?: string;
        message?: string;
    };
}
/** Parse complete SSE events only; incomplete tail stays buffered across network chunks. */
export function docSse(raw: string): {
    events: SuKienSse[];
    rest: string;
} {
    const s = raw.replace(/\r\n/g, '\n');
    const parts = s.split('\n\n');
    const rest = parts.pop() ?? '';
    const events: SuKienSse[] = [];
    for (const p of parts) {
        let event = 'message';
        const data: string[] = [];
        for (const line of p.split('\n')) {
            if (line.startsWith('event:'))
                event = line.slice(6).trim();
            if (line.startsWith('data:'))
                data.push(line.slice(5).trimStart());
        }
        if (data.length) {
            try {
                events.push({ event, data: JSON.parse(data.join('\n')) });
            }
            catch {
                throw new Error('Lời gợi ý bị gián đoạn.');
            }
        }
    }
    return { events, rest };
}
export function tachCongThuc(text: string): {
    text: string;
    math: boolean;
}[] {
    const re = /(\$\$[\s\S]*?\$\$|\\\[[\s\S]*?\\\]|\\\([\s\S]*?\\\)|\$[^$\n]+\$)/g;
    const out: {
        text: string;
        math: boolean;
    }[] = [];
    let start = 0;
    for (const m of text.matchAll(re)) {
        if (m.index > start)
            out.push({ text: text.slice(start, m.index), math: false });
        const double = m[0].startsWith('$$') || m[0].startsWith('\\');
        out.push({ text: m[0].slice(double ? 2 : 1, double ? -2 : -1), math: true });
        start = m.index + m[0].length;
    }
    if (start < text.length)
        out.push({ text: text.slice(start), math: false });
    return out;
}
@Component({ selector: 'app-gia-su', imports: [Button, FormField, NgTemplateOutlet, Katex], templateUrl: './gia-su.html', styleUrl: './gia-su.css' })
export class GiaSu {
    readonly anNut = input(false);
    moGiaSu() {
        if (this.mo()) {
            this.dong();
        }
        else {
            this.truoc = document.activeElement as HTMLElement;
            this.mo.set(true);
        }
    }
    readonly rongChange = output<boolean>();
    readonly rongGiaSu = output<number>();
    protected readonly hop = viewChild<ElementRef<HTMLDialogElement>>('hop');
    protected readonly soan = viewChild<ElementRef<HTMLTextAreaElement>>('soan');
    protected readonly thread = viewChild<ElementRef<HTMLElement>>('thread');
    private truoc: HTMLElement | null = null;
    private cuonCu = '';
    private khoa = false;
    private readonly width = signal(400);
    private hydrated = false;
    private nho(k: string, v: string) {
        try {
            localStorage.setItem(k, v);
        }
        catch {
        }
    }
    protected keo(e: PointerEvent) {
        const el = e.currentTarget as HTMLElement;
        el.setPointerCapture(e.pointerId);
        const x = e.clientX, w = this.width();
        const move = (ev: PointerEvent) => this.doiRong(w + x - ev.clientX);
        const end = () => {
            el.removeEventListener('pointermove', move);
            el.removeEventListener('pointerup', end);
            el.removeEventListener('pointercancel', end);
        };
        el.addEventListener('pointermove', move);
        el.addEventListener('pointerup', end);
        el.addEventListener('pointercancel', end);
    }
    protected phimRong(e: KeyboardEvent) {
        if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
            e.preventDefault();
            this.doiRong(this.width() + (e.key === 'ArrowLeft' ? 8 : -8));
        }
    }
    private doiRong(w: number) {
        this.width.set(Math.max(360, Math.min(480, w)));
        this.rongGiaSu.emit(this.width());
        this.nho('mathl-gia-su-rong', String(this.width()));
    }
    protected readonly chieuRong = this.width.asReadonly();
    protected goChip(q: string) {
        const c = q === 'Gợi ý bước tiếp' ? Math.min(3, this.cap() + 1) : Math.max(1, this.cap());
        this.cap.set(c);
        void this.hoi(q, c);
    }
    protected goPhim(e: KeyboardEvent) {
        if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
            e.preventDefault();
            this.gui(e);
        }
    }
    protected gianO() {
        const o = this.soan()?.nativeElement;
        if (o) {
            o.style.height = 'auto';
            o.style.height = Math.min(160, o.scrollHeight) + 'px';
        }
    }
    protected giuFocus(e: KeyboardEvent) {
        if (e.key !== 'Tab')
            return;
        const d = this.hop()?.nativeElement;
        if (!d)
            return;
        const ds = Array.from(d.querySelectorAll<HTMLElement>('button:not(:disabled),textarea')).filter(x => x.getClientRects().length);
        const first = ds[0], last = ds.at(-1);
        if (e.shiftKey && document.activeElement === first) {
            e.preventDefault();
            last?.focus();
        }
        else if (!e.shiftKey && document.activeElement === last) {
            e.preventDefault();
            first?.focus();
        }
    }
    private moKhoa() {
        if (this.khoa) {
            document.body.style.overflow = this.cuonCu;
            this.khoa = false;
            this.truoc?.focus();
        }
    }
    readonly maBai = input.required<string>();
    readonly maBuoc = input.required<string>();
    readonly tenBuoc = input('');
    readonly moChange = output<boolean>();
    protected readonly mo = signal(false);
    protected readonly rong = signal(false);
    protected readonly cap = signal(0);
    protected readonly dangHoi = signal(false);
    protected readonly trangThai = signal('Đang nghĩ…');
    protected readonly loi = signal('');
    protected readonly luot = signal<readonly {
        vai: 'em' | 'ai';
        text: string;
    }[]>([]);
    protected readonly model = signal({ cauHoi: '' });
    protected readonly f = form(this.model, p => maxLength(p.cauHoi, 1000));
    protected readonly tach = tachCongThuc;
    private readonly phien = inject(Phien);
    private abort: AbortController | null = null;
    private cauCu = '';
    constructor() {
        effect(() => {
            this.moChange.emit(this.mo());
            this.rongChange.emit(this.rong());
            if (this.hydrated)
                this.nho('mathl-gia-su-mo', String(this.mo()));
        });
        effect(() => {
            const d = this.hop()?.nativeElement;
            if (d && this.mo() && !this.rong() && !d.open) {
                this.truoc = document.activeElement as HTMLElement;
                this.cuonCu = document.body.style.overflow;
                document.body.style.overflow = 'hidden';
                this.khoa = true;
                d.showModal();
            }
            else if (!this.mo() || this.rong()) {
                if (d?.open)
                    d.close();
                this.moKhoa();
            }
        });
        effect(() => {
            this.luot();
            this.dangHoi();
            const t = this.thread()?.nativeElement;
            if (t)
                queueMicrotask(() => {
                    t.scrollTop = t.scrollHeight;
                });
        });
        afterNextRender(() => {
            let daMo = false;
            try {
                const w = Number(localStorage.getItem('mathl-gia-su-rong'));
                this.doiRong(w >= 360 && w <= 480 ? w : 400);
                daMo = localStorage.getItem('mathl-gia-su-mo') === 'true';
            }
            catch {
            }
            this.hydrated = true;
            if (typeof matchMedia !== 'function') {
                this.rong.set(true);
            }
            else {
                const m = matchMedia('(min-width: 1025px)');
                const change = () => this.rong.set(m.matches);
                change();
                m.addEventListener('change', change);
                this.destroy.onDestroy(() => m.removeEventListener('change', change));
            }
            // Chỉ mở lại thanh bên ở màn rộng; trên điện thoại gia sư là hộp thoại toàn màn, không tự bật khi tải trang.
            if (daMo && this.rong())
                this.mo.set(true);
            const key = (e: KeyboardEvent) => {
                if ((e.ctrlKey || e.metaKey) && e.key === '/') {
                    e.preventDefault();
                    this.moGiaSu();
                }
                else if (e.key === 'Escape' && this.mo()) {
                    e.preventDefault();
                    this.dong();
                }
            };
            document.addEventListener('keydown', key);
            this.destroy.onDestroy(() => document.removeEventListener('keydown', key));
        });
        this.destroy.onDestroy(() => {
            this.abort?.abort();
            this.moKhoa();
        });
    }
    private readonly destroy = inject(DestroyRef);
    protected dong() {
        this.mo.set(false);
        this.dung();
        this.truoc?.focus();
    }
    protected dung() {
        this.abort?.abort();
        this.dangHoi.set(false);
        this.trangThai.set('Đã dừng.');
    }
    protected hoiLai() {
        this.model.set({ cauHoi: this.cauCu });
        this.loi.set('');
        queueMicrotask(() => {
            this.gianO();
            this.soan()?.nativeElement.focus();
        });
    }
    protected gui(e: Event) {
        e.preventDefault();
        const q = this.model().cauHoi.trim();
        if (!q || q.length > 1000)
            return;
        void this.hoi(q, Math.max(1, this.cap()));
    }
    private async hoi(q: string, cap: number) {
        if (this.dangHoi())
            return;
        this.cauCu = q;
        this.model.set({ cauHoi: '' });
        queueMicrotask(() => this.gianO());
        this.loi.set('');
        this.dangHoi.set(true);
        this.luot.update(l => [...l, { vai: 'em', text: q }]);
        this.abort = new AbortController();
        const abort = this.abort;
        try {
            const req = () => fetch('/api/hs/gia-su', { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + this.phien.tokenTruyCap() }, body: JSON.stringify({ maBai: this.maBai(), maBuoc: this.maBuoc(), cap, cauHoi: q }), signal: abort.signal });
            let res = await req();
            if (res.status === 401 && await this.phien.lamMoi())
                res = await req();
            if (!res.ok || !res.body) {
                if (res.status === 403)
                    throw new Error('Gia sư không mở cho bài kiểm tra này.');
                throw new Error('Chưa hỏi được gia sư. Kiểm tra kết nối rồi hỏi lại.');
            }
            const reader = res.body.getReader();
            const decoder = new TextDecoder();
            let buf = '';
            let xong = false;
            for (;;) {
                const chunk = await reader.read();
                if (chunk.done)
                    break;
                buf += decoder.decode(chunk.value, { stream: true });
                const parsed = docSse(buf);
                buf = parsed.rest;
                for (const event of parsed.events) {
                    if (event.event === 'xong') {
                        xong = true;
                        this.luot.update(l => [...l, { vai: 'ai', text: event.data.message ?? '' }]);
                    }
                    else if (event.event === 'loi')
                        throw new Error(event.data.message ?? 'Chưa có gợi ý đã kiểm.');
                    else
                        this.trangThai.set(({ kho: 'Đang xem kho lớp…', goi: 'Đang nghĩ…', loc: 'Đang kiểm lời gợi ý…' } as Record<string, string>)[event.event] ?? 'Đang nghĩ…');
                }
            }
            if (!xong && !abort.signal.aborted)
                throw new Error('Lời gợi ý bị gián đoạn. Bấm Hỏi lại để sửa câu hỏi.');
        }
        catch (e) {
            if (!abort.signal.aborted)
                this.loi.set(e instanceof Error ? e.message : 'Chưa hỏi được gia sư.');
        }
        finally {
            if (this.abort === abort) {
                this.dangHoi.set(false);
                this.abort = null;
            }
        }
    }
}
