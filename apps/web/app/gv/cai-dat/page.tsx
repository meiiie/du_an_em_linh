import type { Metadata } from "next";
import { eq } from "drizzle-orm";
import { luuCaiDatLop } from "@/lib/actions/gv";
import { maskKey, NHA, parseProvider, type AiProviderId } from "@/lib/ai-catalog";
import { docKhoaCloud } from "@/lib/ai-harness";
import { KiemTraAi } from "@/components/kiem-tra-ai";
import Link from "next/link";
import { Button, buttonClasses } from "@/components/ui/button";
import { fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
import { cn } from "@/lib/cn";
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
  const hasEnv = Boolean((process.env.LLM_API_KEY || "").trim());
  const mask = maskKey(setting?.aiApiKey);
  const cloudReady = Boolean(docKhoaCloud(setting?.aiApiKey));
  return (
    <main className="max-w-xl">
      <PageHeader title="Cài đặt lớp" />
      <p className="mb-6 text-sm">
        <Link href="/gv/ket-noi-ai" className={cn(buttonClasses({ variant: "secondary" }))} data-testid="toi-ket-noi-ai">
          Kết nối ChatGPT
        </Link>
      </p>
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
          <span>
            <span className="font-medium">Mở lời giải sau khi nộp xong năm bước</span>
          </span>
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
                <span className="mt-1 block text-muted">
                  {id === "offline" && "Chạy hết không cần khóa."}
                  {id === "cloud" && "Khóa API chính thức — LLM_API_KEY hoặc ô dưới."}
                  {id === "ollama" && "Chỉ máy này, cổng 11434."}
                  {id === "lmstudio" && "Chỉ máy này, cổng 1234."}
                </span>
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
            placeholder="gpt-4o-mini · llama3.2 · local-model"
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
          <span>
            <span className="font-medium">Cho học sinh dùng Ollama / LM Studio trên máy mình</span>
          </span>
        </label>

        <label className="block text-sm">
          <span className="mb-2 block font-medium">Khóa API lớp</span>
          <input
            name="ai_api_key"
            type="password"
            autoComplete="off"
            data-testid="ai-api-key"
            className={fieldControl}
            placeholder={mask ? `${mask} — để trống để giữ` : "sk-… không bao giờ hiện lại đủ"}
          />
          <span className="mt-2 block text-muted">
            {hasEnv
              ? "Máy chủ đang có LLM_API_KEY — dùng khóa đó."
              : cloudReady
                ? "Đang dùng khóa lớp đã lưu."
                : "Chưa có khóa."}
          </span>
        </label>
        <label className="flex items-start gap-3 text-sm">
          <input name="xoa_ai_api_key" type="checkbox" className="mt-1 size-6 accent-ink" data-testid="xoa-ai-api-key" />
          <span>Xóa khóa lớp đã lưu</span>
        </label>

        <Button type="submit">Lưu cài đặt</Button>
      </form>
      <KiemTraAi macDinh={provider} />
    </main>
  );
}
