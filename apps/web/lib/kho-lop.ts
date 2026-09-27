import { desc, eq } from "drizzle-orm";
import { db } from "./db";
import { documents, formulaSheets, formulas } from "./db/schema";
import { chonKho, type MauCongThuc, type MauTaiLieu } from "./kien-thuc";

export async function taiNguyenKhoLop(): Promise<{ taiLieu: MauTaiLieu[]; congThuc: MauCongThuc[] }> {
  const docs = await db.select().from(documents);
  const sheets = await db.select().from(formulaSheets).orderBy(desc(formulaSheets.version));
  const latest = sheets[0];
  const cts = latest ? await db.select().from(formulas).where(eq(formulas.formulaSheetId, latest.id)) : [];
  return {
    taiLieu: docs.map((d) => ({
      id: d.id,
      title: d.title,
      text: d.textContent,
      licenseStatus: d.licenseStatus,
      version: d.version,
    })),
    congThuc: cts.map((c) => ({
      id: c.id,
      title: c.title,
      latex: c.latex,
      noiDung: c.noiDung,
    })),
  };
}

export async function goiKhoChoBuoc(maBuoc: string, cauHoi?: string) {
  const nguon = await taiNguyenKhoLop();
  return chonKho({ ...nguon, maBuoc, cauHoi });
}
