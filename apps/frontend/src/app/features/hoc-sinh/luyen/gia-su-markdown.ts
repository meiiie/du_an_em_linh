import { afterRenderEffect, Component, ElementRef, inject, input, signal, viewChild, ViewEncapsulation } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { SecurityContext } from '@angular/core';
import { Marked, Token } from 'marked';
import katex from 'katex';
import { NguonGiaSu } from '../../../api/gia-su';
import { Katex } from '../../../shared/toan/katex';

type ToanToken = Token & { latex: string; block: boolean };
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
    app-gia-su-markdown .tutor-source{min-width:44px;min-height:44px;border:1px solid var(--line-strong);border-radius:8px;background:var(--wash);color:var(--ink);font:inherit;cursor:pointer}
    app-gia-su-markdown :focus-visible{outline:2px solid var(--focus);outline-offset:2px}
  `,
})
export class GiaSuMarkdown {
  readonly text = input.required<string>();
  readonly references = input<readonly NguonGiaSu[]>([]);
  protected readonly nguonDangMo = signal<NguonGiaSu | null>(null);
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');
  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    afterRenderEffect(() => {
      const math: { latex: string; block: boolean }[] = [];
      const refs = this.references();
      const marked = new Marked({
        gfm: true, breaks: false,
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
            if (!m) return undefined;
            return { type: 'tutorMath', raw: m[0], latex: m[2] ?? m[3] ?? m[4] ?? m[5], block: !!(m[2] ?? m[3]) };
          },
          renderer: token => {
            const t = token as ToanToken;
            math.push({ latex: t.latex, block: t.block });
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
            // Chỉ dòng trong snapshot của lượt được bấm; không có URL từ lời model.
            return refs.some(r => r.index === n)
              ? `<span class="tutor-source source-${n}">[${n}]</span>` : `[${n}]`;
          },
        }],
      });
      const html = marked.parse(this.text(), { async: false });
      const root = this.content().nativeElement;
      root.innerHTML = this.sanitizer.sanitize(SecurityContext.HTML, html) ?? '';
      root.querySelectorAll<HTMLElement>('.tutor-source').forEach(node => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = node.className;
        button.textContent = node.textContent;
        button.setAttribute('aria-label', 'Mở nguồn ' + node.textContent?.replace(/[\[\]]/g, ''));
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
