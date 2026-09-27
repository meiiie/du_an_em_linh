"use client";

import katex from "katex";
import "katex/dist/katex.min.css";
import { cn } from "@/lib/cn";

export function Tex({ tex, block = false, className }: { tex: string; block?: boolean; className?: string }) {
  const html = katex.renderToString(tex, { throwOnError: false, displayMode: block });
  return (
    <span
      className={cn(block && "my-1 block overflow-x-auto", className)}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
