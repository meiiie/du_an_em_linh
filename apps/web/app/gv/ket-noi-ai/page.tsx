import type { Metadata } from "next";
import type { ReactNode } from "react";
import { ketNoiBangKhoa, ngatKetNoiAi } from "@/lib/actions/gv";
import {
  OPENROUTER_KEYS_PAGE,
  OPENROUTER_MODEL_MAC_DINH,
  ZAI_KEYS_PAGE,
  ZAI_MODEL_MAC_DINH,
  maskKey,
  parseProvider,
  tenNhaLop,
} from "@/lib/ai-catalog";
import { docKhoaNha } from "@/lib/ai-harness";
import { taiNguyenKhoLop } from "@/lib/kho-lop";
import { xemKhoTheoKhung } from "@/lib/kien-thuc";
import { oauthDaDangKy, OPENAI_KEYS_PAGE } from "@/lib/openai-oauth";
import { KhoTheoBuoc } from "@/components/kho-theo-buoc";
import { Button, buttonClasses } from "@/components/ui/button";
import { fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
import { requireRole } from "@/lib/auth";
import { caiDatLopCuaGv } from "@/lib/lop";
import { cn } from "@/lib/cn";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "Gia sư",
};

function FormDanKhoa({
  provider,
  mask,
  modelPlaceholder,
  khoaTestId,
  modelTestId,
  luuTestId,
  doiKhoa,
}: {
  provider: "cloud" | "openrouter" | "zai";
  mask: string | null;
  modelPlaceholder: string;
  khoaTestId?: string;
  modelTestId?: string;
  luuTestId?: string;
  doiKhoa?: boolean;
}) {
  return (
    <form action={ketNoiBangKhoa} className="mt-3 space-y-4">
      <input type="hidden" name="ai_provider" value={provider} />
      <label className="block">
        <span className="sr-only">Khóa API</span>
        <input
          name="ai_api_key"
          type="password"
          autoComplete="off"
          required
          data-testid={khoaTestId}
          className={fieldControl}
          placeholder={mask ? `${mask} — dán khóa mới` : "Dán khóa vừa sao chép"}
        />
      </label>
      {doiKhoa ? null : (
        <details className="text-sm">
          <summary className="cursor-pointer font-medium">Mô hình</summary>
          <input name="ai_model" data-testid={modelTestId} className={`${fieldControl} mt-2`} placeholder={modelPlaceholder} />
        </details>
      )}
      <Button type="submit" data-testid={luuTestId}>
        {doiKhoa ? "Lưu khóa mới" : "Kết nối cho cả lớp"}
      </Button>
    </form>
  );
}

function KhoiNha({
  testId,
  ten,
  trangThaiTestId,
  trangThai,
  ready,
  moHref,
  moTestId,
  moNhan,
  moPhu,
  nutMo,
  provider,
  mask,
  modelPlaceholder,
  khoaTestId,
  modelTestId,
  luuTestId,
  extra,
}: {
  testId: string;
  ten: string;
  trangThaiTestId: string;
  trangThai: string;
  ready: boolean;
  moHref: string;
  moTestId: string;
  moNhan: string;
  moPhu: string;
  nutMo: string;
  provider: "cloud" | "openrouter" | "zai";
  mask: string | null;
  modelPlaceholder: string;
  khoaTestId: string;
  modelTestId: string;
  luuTestId: string;
  extra?: ReactNode;
}) {
  return (
    <section className="border-b border-line py-6" data-testid={testId}>
      <p className="text-sm font-medium">{ten}</p>
      <p className="mt-2 text-sm text-muted" data-testid={trangThaiTestId}>
        {trangThai}
      </p>
      {ready ? (
        <details className="mt-6 text-sm">
          <summary className="cursor-pointer font-medium">Đổi khóa</summary>
          <FormDanKhoa provider={provider} mask={mask} modelPlaceholder={modelPlaceholder} doiKhoa />
        </details>
      ) : (
        <ol className="mt-6 space-y-6 text-sm">
          <li className="flex gap-4">
            <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
              1
            </span>
            <div className="min-w-0 flex-1">
              <p className="font-medium">{moNhan}</p>
              <p className="mt-1 text-muted">{moPhu}</p>
              <a
                href={moHref}
                target="_blank"
                rel="noreferrer"
                className={cn(buttonClasses(), "mt-3")}
                data-testid={moTestId}
              >
                {nutMo}
              </a>
            </div>
          </li>
          <li className="flex gap-4">
            <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
              2
            </span>
            <div className="min-w-0 flex-1">
              <p className="font-medium">Dán khóa</p>
              <FormDanKhoa
                provider={provider}
                mask={null}
                modelPlaceholder={modelPlaceholder}
                khoaTestId={khoaTestId}
                modelTestId={modelTestId}
                luuTestId={luuTestId}
              />
            </div>
          </li>
        </ol>
      )}
      {extra}
    </section>
  );
}

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ ok?: string; oauth?: string; loi?: string }>;
}) {
  const q = await searchParams;
  // F-08: cài đặt AI của lớp GV này dạy
  const u = await requireRole("GV");
  const { setting } = await caiDatLopCuaGv(u);
  const provider = parseProvider(setting?.aiProvider);
  const mask = maskKey(setting?.aiApiKey);
  const chatgptReady = Boolean(docKhoaNha("cloud", setting?.aiApiKey, provider));
  const openrouterReady = Boolean(docKhoaNha("openrouter", setting?.aiApiKey, provider));
  const zaiReady = Boolean(docKhoaNha("zai", setting?.aiApiKey, provider));
  const daKetNoi = chatgptReady || openrouterReady || zaiReady;
  const oauthSanSang = oauthDaDangKy();
  const khung = xemKhoTheoKhung(await taiNguyenKhoLop());
  return (
    <main className="max-w-xl">
      <PageHeader title="Kết nối ChatGPT" />
      <p className="mb-6 text-sm text-muted">Chỉ đọc tài liệu và công thức đã mở — không đọc lời giải.</p>

      {q.loi ? (
        <p className="mb-6 bg-amber-50 px-4 py-3 text-sm text-amber-950 motion-safe:animate-[phieu-vao_180ms_ease-out]" data-testid="ket-noi-loi">
          {q.loi}
        </p>
      ) : null}
      {q.ok ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass motion-safe:animate-[phieu-vao_180ms_ease-out]" data-testid="ket-noi-ok">
          Đã kết nối. Học sinh hỏi gia sư là dùng {tenNhaLop(provider)}.
        </p>
      ) : null}
      {q.oauth ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass motion-safe:animate-[phieu-vao_180ms_ease-out]" data-testid="ket-noi-oauth">
          Đã xác nhận tài khoản ChatGPT. Còn dán khóa cùng tài khoản.
        </p>
      ) : null}

      <div className="border-t border-line">
        <KhoiNha
          testId="ket-noi-chatgpt"
          ten="ChatGPT"
          trangThaiTestId="ket-noi-trang-thai"
          trangThai={
            chatgptReady
              ? `Đã kết nối${mask ? ` · ${mask}` : ""}${setting?.aiOpenaiEmail ? ` · ${setting.aiOpenaiEmail}` : ""}.`
              : setting?.aiOpenaiEmail
                ? `Đã vào ChatGPT (${setting.aiOpenaiEmail}) — chưa có khóa.`
                : provider === "offline"
                  ? "Chưa kết nối. Lớp đang dùng thang gợi ý."
                  : "Chưa kết nối."
          }
          ready={chatgptReady}
          moHref={OPENAI_KEYS_PAGE}
          moTestId="mo-trang-khoa-openai"
          moNhan="Mở ChatGPT"
          moPhu="Tạo một khóa, sao chép."
          nutMo="Mở ChatGPT để lấy khóa"
          provider="cloud"
          mask={mask}
          modelPlaceholder="gpt-4o-mini"
          khoaTestId="ket-noi-khoa"
          modelTestId="ket-noi-model"
          luuTestId="ket-noi-luu"
          extra={
            <div className="mt-8 border-t border-line pt-6">
              <p className="text-sm font-medium">Đăng nhập ChatGPT</p>
              <p className="mt-2 text-sm text-muted">Chỉ lấy tên và email.</p>
              {oauthSanSang ? (
                <a href="/gv/ket-noi-ai/oauth" className={cn(buttonClasses({ variant: "secondary" }), "mt-3")} data-testid="oauth-chatgpt">
                  Tiếp tục với ChatGPT
                </a>
              ) : (
                <p className="mt-3 text-sm text-muted" data-testid="oauth-chua-dk">
                  Trường chưa có <span className="font-mono">OPENAI_OAUTH_CLIENT_ID</span>. Dùng hai bước trên.
                </p>
              )}
            </div>
          }
        />

        <KhoiNha
          testId="ket-noi-openrouter"
          ten="OpenRouter"
          trangThaiTestId="ket-noi-openrouter-trang-thai"
          trangThai={openrouterReady ? `Đã kết nối${mask ? ` · ${mask}` : ""}.` : "Chưa kết nối."}
          ready={openrouterReady}
          moHref={OPENROUTER_KEYS_PAGE}
          moTestId="mo-trang-khoa-openrouter"
          moNhan="Mở OpenRouter"
          moPhu="Tạo một khóa, sao chép."
          nutMo="Mở OpenRouter để lấy khóa"
          provider="openrouter"
          mask={mask}
          modelPlaceholder={OPENROUTER_MODEL_MAC_DINH}
          khoaTestId="ket-noi-openrouter-khoa"
          modelTestId="ket-noi-openrouter-model"
          luuTestId="ket-noi-openrouter-luu"
        />

        <KhoiNha
          testId="ket-noi-zai"
          ten="Z.AI"
          trangThaiTestId="ket-noi-zai-trang-thai"
          trangThai={zaiReady ? `Đã kết nối${mask ? ` · ${mask}` : ""}.` : "Chưa kết nối."}
          ready={zaiReady}
          moHref={ZAI_KEYS_PAGE}
          moTestId="mo-trang-khoa-zai"
          moNhan="Mở Z.AI"
          moPhu="Tạo một khóa, sao chép."
          nutMo="Mở Z.AI để lấy khóa"
          provider="zai"
          mask={mask}
          modelPlaceholder={ZAI_MODEL_MAC_DINH}
          khoaTestId="ket-noi-zai-khoa"
          modelTestId="ket-noi-zai-model"
          luuTestId="ket-noi-zai-luu"
        />
      </div>

      {daKetNoi || setting?.aiOpenaiEmail ? (
        <form action={ngatKetNoiAi} className="mt-6">
          <Button type="submit" variant="ghost" data-testid="ngat-ket-noi">
            Ngắt kết nối
          </Button>
        </form>
      ) : null}

      <section className="sr-only">
        <KhoTheoBuoc khung={khung} />
      </section>
    </main>
  );
}
