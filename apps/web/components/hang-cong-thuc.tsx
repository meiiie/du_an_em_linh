import { Tex } from "@/components/tex";
import { thanTrich } from "@/lib/de-hoc-sinh";

export function HangCongThuc({
  title,
  latex,
  noiDung,
  gon = false,
}: {
  title: string;
  latex?: string | null;
  noiDung?: string | null;
  gon?: boolean;
}) {
  const ham = (latex || "").trim();
  const giai = gon ? thanTrich(noiDung || "") : (noiDung || "").trim();
  return (
    <li className="py-4">
      <p className="text-xs text-muted">{title}</p>
      {ham ? (
        <div className="mt-1 overflow-x-auto" translate="no">
          <Tex tex={ham} block />
        </div>
      ) : null}
      {giai ? <p className="mt-2 text-sm leading-relaxed text-muted">{giai}</p> : null}
    </li>
  );
}
