"use client";

import Markdown from "react-markdown";
import rehypeKatex from "rehype-katex";
import remarkBreaks from "remark-breaks";
import remarkGfm from "remark-gfm";
import remarkMath from "remark-math";
import type { Components } from "react-markdown";
import { tenTaiLieuNgan, thanTrich } from "@/lib/de-hoc-sinh";
import { ganNeoTrongLoi, type TrichDanHien } from "@/lib/kien-thuc";
import { soTuChuThe } from "@/lib/trich-dan-ui";
import { chuanHoaLatexGiaSu } from "@/lib/loi-gia-su";
import { cn } from "@/lib/cn";
import type { MouseEvent, ReactNode } from "react";
import "katex/dist/katex.min.css";

const nhan = "font-semibold leading-snug";

function mdGoc(opts: { trichDan: TrichDanHien[]; onChonSo?: (so: number) => void }): Components {
  return {
    h1: ({ children }) => <p className={nhan}>{children}</p>,
    h2: ({ children }) => <p className={nhan}>{children}</p>,
    h3: ({ children }) => <p className={nhan}>{children}</p>,
    h4: ({ children }) => <p className={nhan}>{children}</p>,
    h5: ({ children }) => <p className={nhan}>{children}</p>,
    h6: ({ children }) => <p className={nhan}>{children}</p>,
    p: ({ children }) => <p className="leading-relaxed">{children}</p>,
    ul: ({ children }) => <ul className="list-disc space-y-1 pl-6">{children}</ul>,
    ol: ({ children }) => <ol className="list-decimal space-y-1 pl-6">{children}</ol>,
    li: ({ children }) => <li className="leading-relaxed [&>p]:my-0">{children}</li>,
    strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
    em: ({ children }) => <em>{children}</em>,
    blockquote: ({ children }) => (
      <blockquote className="border-l-2 border-ink pl-3 leading-relaxed">{children}</blockquote>
    ),
    hr: () => <hr className="border-line" />,
    a: ({ href, children }) => {
      const so = soTuChuThe(children as ReactNode);
      const kho = Boolean(href && href.startsWith("/hs/kho"));
      if (kho && so != null) {
        const t = opts.trichDan.find((x) => x.so === so);
        return (
          <a
            href={href}
            data-testid="tutor-so-trich"
            data-so={so}
            aria-label={t ? `Đã đọc ${so}: ${tenTaiLieuNgan(t.ten)}` : `Đã đọc ${so}`}
            title={t?.trich ? thanTrich(t.trich) : undefined}
            className="ml-0.5 inline-flex h-4 min-w-4 -translate-y-0.5 items-center justify-center bg-wash px-0.5 align-super text-[10px] font-medium leading-none no-underline"
            onClick={(e: MouseEvent<HTMLAnchorElement>) => {
              if (!opts.onChonSo) return;
              e.preventDefault();
              opts.onChonSo(so);
            }}
          >
            {so}
          </a>
        );
      }
      if (href && /^https?:\/\//i.test(href)) {
        return (
          <a href={href} className="underline underline-offset-2" target="_blank" rel="noreferrer">
            {children}
          </a>
        );
      }
      return <span>{children}</span>;
    },
    img: () => null,
    code: ({ children, className }) =>
      className ? (
        <code className="font-mono text-[13px]">{children}</code>
      ) : (
        <code className="rounded-button bg-wash px-1 font-mono text-[13px]">{children}</code>
      ),
    pre: ({ children }) => (
      <pre className="overflow-x-auto bg-wash px-3 py-2 font-mono text-[13px] leading-5">{children}</pre>
    ),
    table: ({ children }) => (
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-[13px]">{children}</table>
      </div>
    ),
    th: ({ children }) => <th className="border-b border-line px-2 py-1 text-left font-semibold">{children}</th>,
    td: ({ children }) => <td className="border-b border-line px-2 py-1">{children}</td>,
  };
}

const katex = { throwOnError: false, strict: false as const, errorColor: "#5C5F66" };

export function LoiGiaSu({
  text,
  trichDan,
  onChonSo,
  className,
}: {
  text: string;
  trichDan?: TrichDanHien[];
  onChonSo?: (so: number) => void;
  className?: string;
}) {
  const dan = trichDan || [];
  const mdText = ganNeoTrongLoi(chuanHoaLatexGiaSu(text), dan);
  return (
    <div
      data-testid="tutor-md"
      translate="no"
      className={cn(
        "space-y-2 text-sm [overflow-wrap:anywhere]",
        "[&_.katex-display]:my-2 [&_.katex-display]:overflow-x-auto [&_.katex-display]:overflow-y-hidden [&_.katex-display]:text-left",
        className,
      )}
    >
      <Markdown
        remarkPlugins={[remarkGfm, remarkMath, remarkBreaks]}
        rehypePlugins={[[rehypeKatex, katex]]}
        disallowedElements={["img", "iframe", "script"]}
        components={mdGoc({ trichDan: dan, onChonSo })}
      >
        {mdText}
      </Markdown>
    </div>
  );
}
