"use client";

import { useState } from "react";
import { cn } from "@/lib/cn";
import { Tex } from "./tex";

const X0 = -4.2;
const X1 = 4.2;
const Y0 = -1.4;
const Y1 = 1.4;
const W = 360;
const H = 280;

function sx(x: number) {
  return ((x - X0) / (X1 - X0)) * W;
}
function sy(y: number) {
  return H - ((y - Y0) / (Y1 - Y0)) * H;
}
// SP-10: hàm minh hoạ KHÔNG có trong ngân hàng bài (không phải đa thức), để trang công khai không lộ bài được giao.
function f(x: number) {
  return (2 * x) / (x * x + 1);
}
function fp(x: number) {
  return (2 * (1 - x * x)) / ((x * x + 1) * (x * x + 1));
}
function so(n: number) {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(n);
}

/** Hình minh họa trên trang chủ — y = 2x/(x²+1), không thuộc ngân hàng bài; không gắn nhãn cực trị. */
export function MinhHoaDaoHam({ className }: { className?: string }) {
  const [x, setX] = useState(2);
  const y = f(x);
  const yp = fp(x);
  const pts: string[] = [];
  for (let i = 0; i <= 80; i++) {
    const t = X0 + ((X1 - X0) * i) / 80;
    pts.push(`${sx(t).toFixed(1)},${sy(f(t)).toFixed(1)}`);
  }
  const span = 1;
  const x1 = x - span;
  const x2 = x + span;

  return (
    <figure id="hinh" className={cn("min-w-0", className)}>
      <div className="cong-thuc overflow-x-auto text-[1.75rem] leading-tight sm:text-[2.5rem]" translate="no">
        <Tex tex="y = \dfrac{2x}{x^{2} + 1}" />
      </div>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="mt-6 w-full"
        role="img"
        aria-label={`Đồ thị y = 2x/(x² + 1) tại x = ${so(x)}, đạo hàm ${so(yp)}`}
      >
        <line x1={sx(X0)} y1={sy(0)} x2={sx(X1)} y2={sy(0)} stroke="#E2E3E6" strokeWidth="1" />
        <line x1={sx(0)} y1={sy(Y0)} x2={sx(0)} y2={sy(Y1)} stroke="#E2E3E6" strokeWidth="1" />
        <polyline fill="none" stroke="#17181C" strokeWidth="1.75" points={pts.join(" ")} />
        <line
          x1={sx(x1)}
          y1={sy(y + yp * (x1 - x))}
          x2={sx(x2)}
          y2={sy(y + yp * (x2 - x))}
          stroke="#17181C"
          strokeWidth="2.75"
        />
        <circle cx={sx(x)} cy={sy(y)} r="4.5" fill="#FFFFFF" stroke="#17181C" strokeWidth="2" />
      </svg>
      <div className="mt-2 flex flex-wrap items-baseline gap-x-6 gap-y-1 text-sm">
        <p>
          <span className="text-muted">x = </span>
          <span className="font-mono tabular">{so(x)}</span>
        </p>
        <p>
          <span className="text-muted">y′ = </span>
          <span className="font-mono tabular">{so(yp)}</span>
        </p>
      </div>
      <label className="mt-3 block">
        <span className="sr-only">Vị trí x trên đường cong</span>
        <input
          type="range"
          min={-4}
          max={4}
          step={0.1}
          value={x}
          onChange={(e) => setX(Number(e.target.value))}
          className="thanh-x w-full"
        />
      </label>
      <figcaption className="mt-4 max-w-[42ch] text-sm leading-relaxed">
        Đạo hàm là hệ số góc của tiếp tuyến tại điểm đó.
      </figcaption>
    </figure>
  );
}
