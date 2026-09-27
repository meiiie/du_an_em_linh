import { Tex } from "@/components/tex";

export function HangCongThuc({
  title,
  latex,
  noiDung,
}: {
  title: string;
  latex?: string | null;
  noiDung?: string | null;
}) {
  const ham = (latex || "").trim();
  return (
    <li className="py-4">
      <p className="text-sm font-medium">{title}</p>
      {ham ? (
        <div className="mt-2 overflow-x-auto border-y border-line bg-wash px-3 py-2" translate="no">
          <Tex tex={ham} block />
        </div>
      ) : null}
      {noiDung ? <p className="mt-2 text-sm leading-relaxed text-muted">{noiDung}</p> : null}
    </li>
  );
}
