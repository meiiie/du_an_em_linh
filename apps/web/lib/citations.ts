import { tenTaiLieuNgan, thanTrich } from "./de-hoc-sinh";

type Hit = {
  trich?: string;
  cum_tu?: string[];
  document_id?: string;
  formula_id?: string;
  phien_ban?: number | string;
  ten?: string;
  noi_dung?: string;
};

function asHits(raw: unknown): Hit[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.filter((x) => x && typeof x === "object") as Hit[];
  if (typeof raw === "object") return [raw as Hit];
  return [];
}

export function moTaTrichDan(raw: unknown): string[] {
  return asHits(raw)
    .map((o) => {
      const bits: string[] = [];
      if (o.trich) bits.push(`«${thanTrich(o.trich)}»`);
      if (o.ten) bits.push(tenTaiLieuNgan(o.ten));
      else if (o.noi_dung) bits.push(thanTrich(o.noi_dung));
      return bits.join(" — ");
    })
    .filter(Boolean);
}
