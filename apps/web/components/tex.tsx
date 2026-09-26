"use client";

import katex from "katex";
import "katex/dist/katex.min.css";

export function Tex({ tex, block = false }: { tex: string; block?: boolean }) {
  const html = katex.renderToString(tex, { throwOnError: false, displayMode: block });
  return <span className={block ? "my-1 block overflow-x-auto" : ""} dangerouslySetInnerHTML={{ __html: html }} />;
}
