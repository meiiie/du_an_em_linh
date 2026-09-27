import { Tex } from "@/components/tex";

/** Hàng phiếu: KaTeX là việc; tên chỉ chú. Không nói lại công thức bằng lời. */
export function HangCongThuc({
  title,
  latex,
}: {
  title: string;
  latex?: string | null;
  noiDung?: string | null;
  gon?: boolean;
}) {
  const ham = (latex || "").trim();
  return (
    <li className="py-6">
      {ham ? (
        <p className="max-w-[65ch] overflow-x-auto text-[1.25rem] leading-8" translate="no">
          <Tex tex={ham} />
        </p>
      ) : null}
      <p className={ham ? "mt-2 text-sm text-muted" : "text-sm font-medium"}>{title}</p>
    </li>
  );
}
