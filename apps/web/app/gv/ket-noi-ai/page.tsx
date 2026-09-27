import type { Metadata } from "next";
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
import { db } from "@/lib/db";
import { classSettings } from "@/lib/db/schema";
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
          <summary className="cursor-pointer font-medium">Mô hình (để trống = mặc định)</summary>
          <input name="ai_model" data-testid={modelTestId} className={`${fieldControl} mt-2`} placeholder={modelPlaceholder} />
        </details>
      )}
      <Button type="submit" data-testid={luuTestId}>
        {doiKhoa ? "Lưu khóa mới" : "Kết nối cho cả lớp"}
      </Button>
    </form>
  );
}

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ ok?: string; oauth?: string; loi?: string }>;
}) {
  const q = await searchParams;
  const setting = (await db.select().from(classSettings).limit(1))[0];
  const provider = parseProvider(setting?.aiProvider);
  const mask = maskKey(setting?.aiApiKey);
  const chatgptReady = provider === "cloud" && Boolean(docKhoaNha("cloud", setting?.aiApiKey));
  const openrouterReady = provider === "openrouter" && Boolean(docKhoaNha("openrouter", setting?.aiApiKey));
  const zaiReady = provider === "zai" && Boolean(docKhoaNha("zai", setting?.aiApiKey));
  const daKetNoi = chatgptReady || openrouterReady || zaiReady;
  const oauthSanSang = oauthDaDangKy();
  const khung = xemKhoTheoKhung(await taiNguyenKhoLop());
  return (
    <main className="max-w-xl">
      <PageHeader title="Kết nối ChatGPT" />

      {q.loi ? (
        <p className="mb-6 bg-amber-50 px-4 py-3 text-sm text-amber-950" data-testid="ket-noi-loi">
          {q.loi}
        </p>
      ) : null}
      {q.ok ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass" data-testid="ket-noi-ok">
          Đã kết nối. Học sinh hỏi gia sư là dùng {tenNhaLop(provider)}.
        </p>
      ) : null}
      {q.oauth ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass" data-testid="ket-noi-oauth">
          Đã xác nhận tài khoản ChatGPT (tên và email). Bước còn lại: dán khóa cùng tài khoản để gọi mô hình.
        </p>
      ) : null}

      <section className="border-y border-line py-6" data-testid="ket-noi-chatgpt">
        <p className="text-sm font-medium">Lớp</p>
        <p className="mt-2 text-sm text-muted" data-testid="ket-noi-trang-thai">
          {chatgptReady
            ? `Đã kết nối${mask ? ` · ${mask}` : ""}${setting?.aiOpenaiEmail ? ` · ${setting.aiOpenaiEmail}` : ""}.`
            : setting?.aiOpenaiEmail
              ? `Đã vào ChatGPT (${setting.aiOpenaiEmail}) — chưa có khóa.`
              : provider === "offline"
                ? "Chưa kết nối. Lớp đang dùng thang gợi ý."
                : "Chưa kết nối."}
        </p>

        {chatgptReady ? (
          <p className="mt-3 text-sm">Gia sư chỉ đọc tài liệu và công thức đã mở, không đọc lời giải.</p>
        ) : (
          <ol className="mt-6 space-y-6 text-sm">
            <li className="flex gap-4">
              <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
                1
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-medium">Mở ChatGPT</p>
                <p className="mt-1 text-muted">Tạo một khóa trên ChatGPT, sao chép.</p>
                <a
                  href={OPENAI_KEYS_PAGE}
                  target="_blank"
                  rel="noreferrer"
                  className={cn(buttonClasses(), "mt-3")}
                  data-testid="mo-trang-khoa-openai"
                >
                  Mở ChatGPT để lấy khóa
                </a>
              </div>
            </li>
            <li className="flex gap-4">
              <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
                2
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-medium">Dán khóa — một lần</p>
                <FormDanKhoa
                  provider="cloud"
                  mask={chatgptReady ? mask : null}
                  modelPlaceholder="gpt-4o-mini"
                  khoaTestId="ket-noi-khoa"
                  modelTestId="ket-noi-model"
                  luuTestId="ket-noi-luu"
                />
              </div>
            </li>
          </ol>
        )}

        {chatgptReady ? (
          <details className="mt-6 text-sm">
            <summary className="cursor-pointer font-medium">Đổi khóa</summary>
            <FormDanKhoa provider="cloud" mask={mask} modelPlaceholder="gpt-4o-mini" doiKhoa />
          </details>
        ) : null}

        <div className="mt-8 border-t border-line pt-6">
          <p className="text-sm font-medium">Đăng nhập ChatGPT</p>
          <p className="mt-2 text-sm text-muted">Chỉ lấy tên và email — không thay khóa gọi mô hình.</p>
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
      </section>

      <section className="border-b border-line py-6" data-testid="ket-noi-openrouter">
        <p className="text-sm font-medium">OpenRouter</p>
        <p className="mt-2 text-sm text-muted" data-testid="ket-noi-openrouter-trang-thai">
          {openrouterReady ? `Đã kết nối${mask ? ` · ${mask}` : ""}.` : "Chưa kết nối."}
        </p>
        {openrouterReady ? (
          <>
            <p className="mt-3 text-sm">Gia sư chỉ đọc tài liệu và công thức đã mở, không đọc lời giải.</p>
            <details className="mt-6 text-sm">
              <summary className="cursor-pointer font-medium">Đổi khóa</summary>
              <FormDanKhoa provider="openrouter" mask={mask} modelPlaceholder={OPENROUTER_MODEL_MAC_DINH} doiKhoa />
            </details>
          </>
        ) : (
          <ol className="mt-6 space-y-6 text-sm">
            <li className="flex gap-4">
              <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
                1
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-medium">Mở OpenRouter</p>
                <p className="mt-1 text-muted">Tạo khóa lập trình, sao chép.</p>
                <a
                  href={OPENROUTER_KEYS_PAGE}
                  target="_blank"
                  rel="noreferrer"
                  className={cn(buttonClasses(), "mt-3")}
                  data-testid="mo-trang-khoa-openrouter"
                >
                  Mở OpenRouter để lấy khóa
                </a>
              </div>
            </li>
            <li className="flex gap-4">
              <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
                2
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-medium">Dán khóa — một lần</p>
                <FormDanKhoa
                  provider="openrouter"
                  mask={openrouterReady ? mask : null}
                  modelPlaceholder={OPENROUTER_MODEL_MAC_DINH}
                  khoaTestId="ket-noi-openrouter-khoa"
                  modelTestId="ket-noi-openrouter-model"
                  luuTestId="ket-noi-openrouter-luu"
                />
              </div>
            </li>
          </ol>
        )}
      </section>

      <section className="border-b border-line py-6" data-testid="ket-noi-zai">
        <p className="text-sm font-medium">Z.AI</p>
        <p className="mt-2 text-sm text-muted" data-testid="ket-noi-zai-trang-thai">
          {zaiReady ? `Đã kết nối${mask ? ` · ${mask}` : ""}.` : "Chưa kết nối."}
        </p>
        {zaiReady ? (
          <>
            <p className="mt-3 text-sm">Gia sư chỉ đọc tài liệu và công thức đã mở, không đọc lời giải.</p>
            <details className="mt-6 text-sm">
              <summary className="cursor-pointer font-medium">Đổi khóa</summary>
              <FormDanKhoa provider="zai" mask={mask} modelPlaceholder={ZAI_MODEL_MAC_DINH} doiKhoa />
            </details>
          </>
        ) : (
          <ol className="mt-6 space-y-6 text-sm">
            <li className="flex gap-4">
              <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
                1
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-medium">Mở Z.AI</p>
                <p className="mt-1 text-muted">Tạo khóa coding, sao chép.</p>
                <a
                  href={ZAI_KEYS_PAGE}
                  target="_blank"
                  rel="noreferrer"
                  className={cn(buttonClasses(), "mt-3")}
                  data-testid="mo-trang-khoa-zai"
                >
                  Mở Z.AI để lấy khóa
                </a>
              </div>
            </li>
            <li className="flex gap-4">
              <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
                2
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-medium">Dán khóa — một lần</p>
                <FormDanKhoa
                  provider="zai"
                  mask={mask}
                  modelPlaceholder={ZAI_MODEL_MAC_DINH}
                  khoaTestId="ket-noi-zai-khoa"
                  modelTestId="ket-noi-zai-model"
                  luuTestId="ket-noi-zai-luu"
                />
              </div>
            </li>
          </ol>
        )}
      </section>

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
