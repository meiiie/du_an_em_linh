import { Tex } from "@/components/tex";

/** Hàng phiếu: KaTeX là việc; tên chỉ chú. Không nói lại công thức bằng lời. */
export function HangCongThuc({
  id,
  title,
  latex,
}: {
  id?: string;
  title: string;
  latex?: string | null;
  noiDung?: string | null;
  gon?: boolean;
}) {
  const ham = (latex || "").trim();
  return (
    <li id={id ? `ct-${id}` : undefined} className="scroll-mt-6 py-6">
      {ham ? (
        <p className="max-w-[65ch] overflow-x-auto text-[1.25rem] leading-8" translate="no">
          <Tex tex={ham} />
        </p>
      ) : null}
      <p className={ham ? "mt-2 text-sm text-muted" : "text-sm font-medium"}>{title}</p>
    </li>
  );
}
