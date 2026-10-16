import { afterRenderEffect, effect, untracked, DestroyRef, Component, ElementRef, inject, input, signal, viewChild, ViewEncapsulation } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { SecurityContext } from '@angular/core';
import { Marked, Token } from 'marked';
import katex from 'katex';
import { NguonGiaSu } from '../../../api/gia-su';
import { Katex } from '../../../shared/toan/katex';

type ToanToken = Token & { latex?: string; block?: boolean };
const escapeHtml = (s: string) => s.replace(/[&<>"']/g, c => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' })[c]!);

/** Sanitize Markdown trước khi gắn DOM. Chỉ KaTeX tin cậy vẽ HTML/MathML sau đó. */
@Component({
  selector: 'app-gia-su-markdown',
  encapsulation: ViewEncapsulation.None,
  imports: [Katex],
  template: `<div #content class="markdown" (click)="chonNguon($event)"></div>
    @if (nguonDangMo(); as n) {<div class="source" role="note"><strong>Nguồn [{{n.index}}]</strong><p>{{n.text}}</p>@if(n.latex){<app-katex [latex]="n.latex" khoi />}</div>}`,
  styles: `
    app-gia-su-markdown{display:block;min-width:0;overflow-wrap:anywhere}
    app-gia-su-markdown .source{padding:8px;margin-top:8px;border:1px solid var(--line);border-radius:8px;background:var(--wash)}
    app-gia-su-markdown .source p{white-space:pre-wrap;margin:8px 0}
    app-gia-su-markdown .markdown p{white-space:pre-wrap;margin:0 0 8px}
    app-gia-su-markdown .markdown>:last-child{margin-bottom:0}
    app-gia-su-markdown .markdown ul,app-gia-su-markdown .markdown ol{padding-inline-start:24px}
    app-gia-su-markdown .markdown pre,app-gia-su-markdown .markdown table{display:block;max-width:100%;overflow:auto}
    app-gia-su-markdown .tutor-math-block{display:block;max-width:100%;overflow-x:auto;overflow-y:hidden}
    app-gia-su-markdown .tutor-source{position:relative;display:inline-flex;align-items:center;justify-content:center;min-width:26px;height:22px;padding:0 5px;margin:0 1px;vertical-align:1px;border:1px solid var(--line-strong);border-radius:6px;background:var(--wash);color:var(--ink);font:inherit;font-size:.85em;line-height:1;cursor:pointer}app-gia-su-markdown .tutor-source::after{content:'';position:absolute;inset:-11px -9px}
    app-gia-su-markdown :focus-visible{outline:2px solid var(--focus);outline-offset:2px}
  `,
})
export class GiaSuMarkdown {
  readonly text = input.required<string>();
  readonly animate = input(false);
  private readonly revealed = signal('');
  private frame = 0;
  readonly references = input<readonly NguonGiaSu[]>([]);
  protected readonly nguonDangMo = signal<NguonGiaSu | null>(null);
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');
  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    effect(() => {
      const target = this.text();
      cancelAnimationFrame(this.frame);
      if (!this.animate() || (typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches)) {
        this.revealed.set(target);
        return;
      }
      const previous = untracked(this.revealed);
      if (!target.startsWith(previous)) this.revealed.set('');
      // Nhịp đều theo frame để chữ chảy dần cả khi nhà cung cấp đổ cả câu một lúc.
      // Theo thời gian thật: ~170 ký tự/giây, backlog dài bắt kịp trong ~1 giây, không phụ thuộc tần số màn hình.
      let last = 0, carry = 0;
      const drain = (now = performance.now()) => {
        const current = this.revealed();
        const dt = last ? Math.min(100, now - last) : 16;
        last = now;
        const backlog = target.length - current.length;
        carry += dt * Math.max(0.17, backlog / 400);
        const step = Math.floor(carry);
        carry -= step;
        let end = Math.min(target.length, current.length + step);
        if (end < target.length && /[\uD800-\uDBFF]/.test(target[end - 1])) end++;
        this.revealed.set(target.slice(0, end));
        if (end < target.length) this.frame = requestAnimationFrame(drain);
      };
      untracked(drain);
    });
    inject(DestroyRef).onDestroy(() => cancelAnimationFrame(this.frame));
    afterRenderEffect(() => {
      const references = this.references();
      const math: { latex: string; block: boolean }[] = [];
      const marked = new Marked({
        gfm: true, breaks: true,
        renderer: {
          html: token => escapeHtml(token.text),
          image: () => '',
          link: function(token) { return this.parser.parseInline(token.tokens); },
        },
        extensions: [{
          name: 'tutorMath', level: 'inline',
          start: src => src.search(/\$|\\\(|\\\[/),
          tokenizer: src => {
            const m = /^(\$\$([\s\S]+?)\$\$|\\\[([\s\S]+?)\\\]|\\\(([\s\S]+?)\\\)|\$([^$\n]+?)\$)/.exec(src);
            if (!m) {
              // Token thô có thể kết thúc giữa công thức. Giữ delimiter/LaTeX
              // nguyên văn, không để Markdown nuốt dấu gạch chéo hay dấu nhấn.
              const partial = /^(\$\$[\s\S]*|\\\[[\s\S]*|\\\([^\n]*|\$[^$\n]*)/.exec(src);
              return partial ? { type: 'tutorMath', raw: partial[0] } : undefined;
            }
            return { type: 'tutorMath', raw: m[0], latex: m[2] ?? m[3] ?? m[4] ?? m[5], block: !!(m[2] ?? m[3]) };
          },
          renderer: token => {
            const t = token as ToanToken;
            if (t.latex === undefined) return escapeHtml(t.raw);
            math.push({ latex: t.latex, block: !!t.block });
            return `<span class="tutor-math${t.block ? ' tutor-math-block' : ''}"></span>`;
          },
        }, {
          name: 'tutorSource', level: 'inline',
          start: src => src.search(/\[\d+\]/),
          tokenizer: src => {
            const m = /^\[(\d+)\]/.exec(src);
            return m ? { type: 'tutorSource', raw: m[0], index: Number(m[1]) } : undefined;
          },
          renderer: token => {
            const n = Number(token['index']);
            if (!references.some(r => r.index === n)) return '';
            return `<span class="tutor-source source-${n}">[${n}]</span>`;
          },
        }],
      });
      const html = marked.parse(this.revealed(), { async: false });
      const root = this.content().nativeElement;
      root.innerHTML = this.sanitizer.sanitize(SecurityContext.HTML, html) ?? '';
      root.querySelectorAll<HTMLElement>('.tutor-source').forEach(node => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = node.className;
        button.textContent = node.textContent;
        const index = Number(node.textContent?.replace(/[\[\]]/g, ''));
        const source = references.find(r => r.index === index);
        button.title = source?.text ?? '';
        button.addEventListener('focus', () => this.nguonDangMo.set(source ?? null));
        button.addEventListener('mouseenter', () => this.nguonDangMo.set(source ?? null));
        button.setAttribute('aria-label', 'Mở công thức ' + node.textContent?.replace(/[\[\]]/g, ''));
        node.replaceWith(button);
      });
      root.querySelectorAll<HTMLElement>('.tutor-math').forEach((node, index) => {
        const t = math[index];
        if (!t) return;
        try {
          katex.render(t.latex, node, { displayMode: t.block, throwOnError: false, trust: false,
            strict: 'ignore', output: 'htmlAndMathml', errorColor: 'var(--muted)', maxExpand: 1000 });
        } catch { node.textContent = t.latex; }
        if (t.block && node.scrollWidth > node.clientWidth) {
          node.tabIndex = 0;
          node.setAttribute('role', 'group');
          node.setAttribute('aria-label', 'Công thức, cuộn ngang');
        }
      });
    });
  }

  protected chonNguon(event: MouseEvent): void {
    const button = (event.target as HTMLElement).closest<HTMLButtonElement>('.tutor-source');
    if (!button) return;
    const cls = [...button.classList].find(c => /^source-\d+$/.test(c));
    const index = Number(cls?.slice(7));
    this.nguonDangMo.set(this.references().find(r => r.index === index) ?? null);
  }
}
