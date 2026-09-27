"use client";

import { useState } from "react";
import { Tex } from "./tex";

const X0 = -2.2;
const X1 = 2.2;
const Y0 = -3;
const Y1 = 3;
const W = 360;
const H = 220;

function sx(x: number) {
  return ((x - X0) / (X1 - X0)) * W;
}
function sy(y: number) {
  return H - ((y - Y0) / (Y1 - Y0)) * H;
}
function f(x: number) {
  return x * x * x - 3 * x;
}
function fp(x: number) {
  return 3 * x * x - 3;
}
function so(n: number) {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(n);
}

/** Hình minh họa trên trang chủ — hàm khác bài đang chấm, không gắn nhãn cực trị. */
export function MinhHoaDaoHam() {
  const [x, setX] = useState(1);
  const y = f(x);
  const yp = fp(x);
  const pts: string[] = [];
  for (let i = 0; i <= 80; i++) {
    const t = X0 + ((X1 - X0) * i) / 80;
    pts.push(`${sx(t).toFixed(1)},${sy(f(t)).toFixed(1)}`);
  }
  const span = 0.65;
  const x1 = x - span;
  const x2 = x + span;

  return (
    <figure className="mt-12 max-w-xl">
      <div className="cong-thuc overflow-x-auto text-[1.75rem] leading-tight sm:text-[2.25rem]" translate="no">
        <Tex tex="y = x^{3} - 3x" />
      </div>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="mt-6 w-full max-w-md"
        role="img"
        aria-label={`Đồ thị y = x^3 − 3x tại x = ${so(x)}, đạo hàm ${so(yp)}`}
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
      <label className="mt-3 block max-w-md">
        <span className="sr-only">Vị trí x trên đường cong</span>
        <input
          type="range"
          min={-2}
          max={2}
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
