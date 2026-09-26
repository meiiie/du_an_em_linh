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
      if (o.trich) bits.push(`«${o.trich}»`);
      if (o.noi_dung && !o.trich) bits.push(o.noi_dung);
      if (o.ten) bits.push(o.ten);
      if (o.cum_tu?.length) bits.push(`khớp: ${o.cum_tu.join(", ")}`);
      if (o.phien_ban != null) bits.push(`phiên bản ${o.phien_ban}`);
      return bits.join(" · ");
    })
    .filter(Boolean);
}
