import { Tex } from "@/components/tex";

/** Cảnh phòng trưng bày — chữ Galileo, trục dọc và tích phân nằm trên hình, không nung vào ảnh. */
export function HeroCanh() {
  return (
    <div className="relative min-h-[560px] overflow-hidden bg-[#e4ddd2] sm:min-h-[640px] lg:min-h-[760px]">
      <svg
        viewBox="0 0 900 1100"
        preserveAspectRatio="xMidYMid slice"
        className="absolute inset-0 h-full w-full"
        aria-hidden
      >
        <defs>
          <linearGradient id="tuong-sang" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#f7f4ee" />
            <stop offset="100%" stopColor="#e3dbd0" />
          </linearGradient>
          <linearGradient id="lo-cay" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#e7f0df" />
            <stop offset="45%" stopColor="#b7c9a6" />
            <stop offset="100%" stopColor="#8ea184" />
          </linearGradient>
          <linearGradient id="san" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#f3efe8" />
            <stop offset="100%" stopColor="#e6dfd4" />
          </linearGradient>
        </defs>
        <rect width="900" height="1100" fill="#ebe4da" />
        <path
          d="M0 40 C80 20 160 80 230 160 C310 250 340 420 330 620 C318 860 250 1000 180 1100 L0 1100 Z"
          fill="url(#lo-cay)"
        />
        <path d="M70 250 C140 180 210 200 250 280 C220 360 140 390 80 340 Z" fill="#d5e3c4" opacity="0.85" />
        <path d="M20 420 C90 360 170 400 190 500 C150 560 70 540 30 480 Z" fill="#c3d4b0" opacity="0.7" />
        <g fill="#f6f3ee">
          <rect x="48" y="760" width="210" height="16" rx="1" />
          <rect x="68" y="804" width="200" height="16" />
          <rect x="88" y="848" width="190" height="16" />
          <rect x="108" y="892" width="180" height="16" />
          <rect x="128" y="936" width="170" height="16" />
        </g>
        <path
          d="M250 0 C430 40 560 180 640 380 C730 600 790 820 860 980 L900 1000 L900 0 Z"
          fill="url(#tuong-sang)"
        />
        <path d="M0 900 C240 820 520 860 900 1020 L900 1100 L0 1100 Z" fill="url(#san)" />
        <path d="M0 980 C280 930 560 960 900 1060" fill="none" stroke="#ddd4c8" strokeWidth="1" />
      </svg>

      <blockquote className="absolute right-[18%] top-[16%] hidden max-w-[15rem] text-right sm:block">
        <p className="font-landing text-[1.35rem] italic leading-snug text-[#3c3a36]">
          “Toán học là ngôn ngữ của những quy luật đẹp nhất.”
        </p>
        <footer className="mt-3 text-xs tracking-wide text-[#8d877e]">— Galileo Galilei</footer>
      </blockquote>

      <ul className="absolute right-6 top-[22%] hidden flex-col gap-5 text-[11px] font-medium tracking-[0.28em] text-[#a39e94] lg:flex">
        {["CURIOSITY", "LOGIC", "BEAUTY", "FREEDOM"].map((w) => (
          <li key={w}>{w}</li>
        ))}
      </ul>

      <div
        className="pointer-events-none absolute bottom-[14%] left-[8%] text-[#c4bbb0] sm:left-[18%]"
        aria-hidden
      >
        <span className="block origin-left scale-x-125 text-5xl opacity-80 sm:text-6xl" style={{ transform: "perspective(400px) rotateX(58deg)" }}>
          <Tex tex="\int_{a}^{b} f(x)\,dx" />
        </span>
      </div>

      <div className="absolute bottom-8 right-6 text-right text-[10px] tracking-[0.22em] text-[#8d877e]">
        <p>HỌC SÂU HƠN</p>
        <p className="mt-1">ĐI XA HƠN</p>
        <a href="#trai-nghiem" className="mt-4 inline-block tracking-normal text-[#5c5954] underline-offset-4 hover:underline">
          Khám phá thêm ↓
        </a>
      </div>
    </div>
  );
}
