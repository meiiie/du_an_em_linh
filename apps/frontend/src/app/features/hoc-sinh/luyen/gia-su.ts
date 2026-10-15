import { NgTemplateOutlet } from '@angular/common';
import { afterNextRender, Component, DestroyRef, effect, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { form, FormField, maxLength } from '@angular/forms/signals';
import { GiaSuMarkdown } from './gia-su-markdown';
import { GiaSuPhien } from './gia-su-phien';
import { Button } from '../../../shared/ui/button';
export { docSse } from './gia-su-sse';
@Component({ selector: 'app-gia-su', imports: [Button, FormField, NgTemplateOutlet, GiaSuMarkdown], providers: [GiaSuPhien], templateUrl: './gia-su.html', styleUrl: './gia-su.css' })
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
        const ds = Array.from(d.querySelectorAll<HTMLElement>('button:not(:disabled),textarea,select,[tabindex="0"]')).filter(x => x.getClientRects().length);
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
    readonly giaoBaiId = input('');
    protected readonly chat = inject(GiaSuPhien);
    protected readonly oDay = signal(true);
    readonly maBuoc = input.required<string>();
    readonly tenBuoc = input('');
    readonly moChange = output<boolean>();
    protected readonly mo = signal(false);
    protected readonly rong = signal(false);
    protected readonly cap = signal(0);
    protected readonly model = signal({ cauHoi: '' });
    protected readonly f = form(this.model, p => maxLength(p.cauHoi, 2000));
    private daMo = false;
    private baiCu = '';
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
            const assignment = this.giaoBaiId();
            const problem = this.maBai();
            if (this.mo() && (!this.daMo || this.baiCu !== assignment + ':' + problem)) {
                this.baiCu = assignment + ':' + problem;
                this.model.set({ cauHoi: '' });
                this.cap.set(1);
                this.oDay.set(true);
                void this.chat.mo(assignment);
            }
            this.daMo = this.mo();
        });
        effect(() => {
            this.chat.luot();
            this.chat.dangHoi();
            const t = this.thread()?.nativeElement;
            if (t && this.oDay()) queueMicrotask(() => t.scrollTop = t.scrollHeight);
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
                // Sidebar + page padding + phiếu 480 px + gap + gia sư cần màn desktop rộng.
                const m = matchMedia('(min-width: 1280px)');
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
            this.chat.dispose();
            this.moKhoa();
        });
    }
    private readonly destroy = inject(DestroyRef);
    protected dong() {
        this.mo.set(false);
        this.dung();
        this.truoc?.focus();
    }
    protected dung() { void this.chat.dung(); }
    protected hoiLai(q: string) {
        this.model.set({ cauHoi: q });
        queueMicrotask(() => {
            this.gianO();
            this.soan()?.nativeElement.focus();
        });
    }
    protected doiCap(e: Event) { this.cap.set(Number((e.target as HTMLSelectElement).value)); }
    protected cuon() {
        const t = this.thread()?.nativeElement;
        if (t) this.oDay.set(t.scrollHeight - t.scrollTop - t.clientHeight < 48);
    }
    protected xuong() {
        const t = this.thread()?.nativeElement;
        if (t) t.scrollTop = t.scrollHeight;
        this.oDay.set(true);
    }
    protected taiLai() { void this.chat.mo(this.giaoBaiId()); }
    protected gui(e: Event) {
        e.preventDefault();
        // Đọc ô hiện tại cả khi input và Enter xảy ra trong cùng một lượt sự kiện.
        const q = (this.soan()?.nativeElement.value ?? this.model().cauHoi).trim();
        if (!q || q.length > 2000 || !this.chat.sanSang()) return;
        this.model.set({ cauHoi: '' });
        this.oDay.set(true);
        queueMicrotask(() => this.gianO());
        void this.chat.hoi(q, this.maBuoc(), Math.max(1, this.cap()));
    }
}
