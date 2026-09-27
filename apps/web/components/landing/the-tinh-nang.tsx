import type { ReactNode } from "react";
import { LineChart, MessageCircle, Target } from "lucide-react";

function The({
  icon,
  title,
  text,
  children,
}: {
  icon: ReactNode;
  title: string;
  text: string;
  children: ReactNode;
}) {
  return (
    <article className="flex flex-col rounded-3xl border border-[#e6e1d8] bg-white p-6 shadow-[0_10px_30px_rgba(28,28,28,0.04)]">
      <div className="flex size-10 items-center justify-center rounded-full border border-[#e6e1d8] text-[#1c1c1c]">
        {icon}
      </div>
      <h3 className="mt-4 text-lg font-semibold tracking-tight">{title}</h3>
      <p className="mt-2 text-sm leading-relaxed text-[#6d6962]">{text}</p>
      <div className="mt-5">{children}</div>
    </article>
  );
}

const MUC = [
  { ten: "Đại số", chon: false },
  { ten: "Giải tích", chon: true },
  { ten: "Hình học", chon: false },
  { ten: "Xác suất & Thống kê", chon: false },
];

export function TheTinhNang() {
  return (
    <div className="grid gap-5 lg:grid-cols-3">
      <The
        icon={<Target className="size-4" strokeWidth={1.5} aria-hidden />}
        title="Lộ trình cá nhân hóa"
        text="Học đúng trọng tâm, theo năng lực và mục tiêu của bạn."
      >
        <ul className="space-y-1 text-sm">
          {MUC.map((m) => (
            <li
              key={m.ten}
              className={m.chon ? "flex items-center gap-3 rounded-xl bg-[#e7f0fa] px-3 py-2 font-medium" : "flex items-center gap-3 px-3 py-2 text-[#6d6962]"}
            >
              <span className={m.chon ? "size-2 rounded-full bg-[#3d5a80]" : "size-2 rounded-full border border-[#c8c2b8]"} />
              {m.ten}
            </li>
          ))}
        </ul>
      </The>

      <The
        icon={<LineChart className="size-4" strokeWidth={1.5} aria-hidden />}
        title="Trực quan hóa sinh động"
        text="Đồ thị, hình học và mô phỏng tương tác giúp bạn thấy rõ bản chất."
      >
        <svg viewBox="0 0 280 150" className="w-full" role="img" aria-label="Đồ thị với tiếp tuyến ngang, f'(x) = 0">
          <line x1="24" y1="16" x2="24" y2="128" stroke="#e4dfd6" />
          <line x1="24" y1="128" x2="264" y2="128" stroke="#e4dfd6" />
          <path
            d="M28 112 C52 112 68 48 96 72 C124 96 140 108 164 78 C188 48 214 36 252 52"
            fill="none"
            stroke="#3d5a80"
            strokeWidth="2.25"
          />
          <line x1="78" y1="78" x2="168" y2="78" stroke="#3d5a80" strokeWidth="1.5" />
          <circle cx="118" cy="78" r="4" fill="#3d5a80" />
          <text x="132" y="70" fill="#3d5a80" fontSize="12" fontFamily="IBM Plex Sans, sans-serif">
            f&apos;(x) = 0
          </text>
        </svg>
      </The>

      <The
        icon={<MessageCircle className="size-4" strokeWidth={1.5} aria-hidden />}
        title="Trợ lý AI đồng hành"
        text="Giải thích từng bước, gợi ý đúng lúc, không đưa đáp án ăn trọn."
      >
        <div className="space-y-3">
          <p className="rounded-2xl bg-[#f4f1eb] px-3 py-2 text-xs leading-relaxed text-[#3c3a36]">
            Làm thế nào để xét dấu của f&apos;(x)?
          </p>
          <div className="flex items-start gap-2">
            <span className="mt-0.5 size-6 shrink-0 rounded-full bg-[#e4ddd2]" aria-hidden />
            <p className="text-xs leading-relaxed text-[#6d6962]">
              Hãy tìm các nghiệm của f&apos;(x), sau đó lập bảng xét dấu trên các khoảng...
            </p>
          </div>
        </div>
      </The>
    </div>
  );
}
