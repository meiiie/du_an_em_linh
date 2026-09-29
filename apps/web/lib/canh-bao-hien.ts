import { tenKyNangNgan } from "./de-hoc-sinh";
import { tenBuoc } from "./ket-buoc";

/** Giờ theo múi Việt Nam cho dòng cảnh báo: "15:42 · 29/09". */
export function gioCanhBao(d: Date): string {
  const f = (o: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat("en-GB", { timeZone: "Asia/Ho_Chi_Minh", ...o }).format(d);
  return `${f({ hour: "2-digit", minute: "2-digit", hour12: false })} · ${f({ day: "2-digit", month: "2-digit" })}`;
}

export type CanhBaoHien = {
  id: string;
  studentId: string;
  ten: string;
  kyNang: string;
  buoc: string | null;
  bai: { id: string; code: string } | null;
  lyDo: string;
  nho: boolean;
  luc: string;
  href: string;
};

export function dungCanhBao(
  e: { id: string; studentId: string; skillCode: string; maBuoc: string | null; problemId: string | null; reason: string; loai: string; createdAt: Date },
  ten: Map<string, string>,
  kyNang: Map<string, string>,
  bai: Map<string, { id: string; code: string }>,
): CanhBaoHien {
  const b = e.problemId ? bai.get(e.problemId) || null : null;
  return {
    id: e.id,
    studentId: e.studentId,
    ten: ten.get(e.studentId) || "Học sinh",
    kyNang: kyNang.get(e.skillCode) || tenKyNangNgan(e.skillCode, e.skillCode),
    buoc: e.maBuoc ? tenBuoc(e.maBuoc) : null,
    bai: b,
    lyDo: e.reason,
    nho: e.loai === "NHO_GV",
    luc: gioCanhBao(e.createdAt),
    href: `/gv/hoc-sinh/${e.studentId}${b ? `?bai=${b.id}` : ""}`,
  };
}
