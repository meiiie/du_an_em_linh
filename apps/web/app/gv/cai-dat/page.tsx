import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { luuCaiDatLop } from "@/lib/actions/gv";
import { laNhaKhoa, NHA, parseProvider, type AiProviderId } from "@/lib/ai-catalog";
import { docKhoaNha } from "@/lib/ai-harness";
import { KiemTraAi } from "@/components/kiem-tra-ai";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
import { chamDoChinhXac } from "@/lib/do-chinh-xac";
import { db } from "@/lib/db";
import { classSettings, classes } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Cài lớp",
};

export default async function Page() {
  const lop = (await db.select().from(classes))[0];
  const setting = lop ? (await db.select().from(classSettings).where(eq(classSettings.classId, lop.id)))[0] : null;
  const provider = parseProvider(setting?.aiProvider);
  const hasEnv = laNhaKhoa(provider) && Boolean(docKhoaNha(provider, null));
  const khoaLop = Boolean((setting?.aiApiKey || "").trim());
  const the = chamDoChinhXac();
  const coKhoa = hasEnv || khoaLop;
  return (
    <main className="max-w-xl">
      <PageHeader title="Cài đặt lớp" />
      <form action={luuCaiDatLop} className="space-y-4 border-y border-line py-6">
        <p className="text-sm text-muted">Lớp {lop?.name || "—"}</p>
        <label className="flex items-start gap-3 text-sm">
          <input
            name="mo_loi_giai"
            type="checkbox"
            defaultChecked={setting?.moLoiGiaiSauKhiNop === true}
            className="mt-1 size-6 accent-ink"
            data-testid="mo-loi-giai"
          />
          <span className="font-medium">Mở lời giải sau khi nộp xong năm bước</span>
        </label>

        <fieldset className="space-y-2" data-testid="ai-provider">
          <legend className="mb-2 text-sm font-medium">Nhà gia sư</legend>
          {(Object.keys(NHA) as AiProviderId[]).map((id) => (
            <label key={id} className="flex items-start gap-3 text-sm">
              <input
                type="radio"
                name="ai_provider"
                value={id}
                defaultChecked={provider === id}
                className="mt-1 size-6 accent-ink"
                data-testid={`ai-provider-${id}`}
              />
              <span>
                <span className="font-medium">{NHA[id].ten}</span>
                <span className="mt-1 block text-muted">{NHA[id].moTa}</span>
              </span>
            </label>
          ))}
        </fieldset>

        <label className="block text-sm">
          <span className="mb-2 block font-medium">Mô hình</span>
          <input
            name="ai_model"
            data-testid="ai-model"
            defaultValue={setting?.aiModel || ""}
            className={fieldControl}
            placeholder="gpt-4o-mini · qwen/qwen3-coder · glm-5.3-flashx · llama3.2"
          />
        </label>

        <label className="flex items-start gap-3 text-sm">
          <input
            name="ai_allow_local"
            type="checkbox"
            defaultChecked={setting?.aiAllowLocal !== false}
            className="mt-1 size-6 accent-ink"
            data-testid="ai-allow-local"
          />
          <span className="font-medium">Cho học sinh dùng Ollama / LM Studio trên máy mình</span>
        </label>

        <p className="text-sm">
          {coKhoa ? "Đã có khóa." : "Chưa có khóa."}{" "}
          <Link href="/gv/ket-noi-ai" className="underline underline-offset-2" data-testid="toi-ket-noi-ai">
            Dán khóa
          </Link>
        </p>

        <Button type="submit">Lưu cài đặt</Button>
      </form>
      <KiemTraAi macDinh={provider} />
      <p className="mt-6 text-sm text-muted" data-testid="do-chinh-xac-tom-tat">
        Luật gia sư {the.diem}/{the.toiDa} · nguồn {the.sota.diem}/{the.sota.toiDa}
      </p>
    </main>
  );
}
