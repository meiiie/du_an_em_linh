import Link from "next/link";
import { ketNoiBangKhoa, ngatKetNoiAi } from "@/lib/actions/gv";
import { maskKey } from "@/lib/ai-catalog";
import { docKhoaCloud } from "@/lib/ai-harness";
import { oauthDaDangKy, OPENAI_KEYS_PAGE } from "@/lib/openai-oauth";
import { Button, buttonClasses } from "@/components/ui/button";
import { fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
import { db } from "@/lib/db";
import { classSettings } from "@/lib/db/schema";
import { cn } from "@/lib/cn";

export const dynamic = "force-dynamic";

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ ok?: string; oauth?: string; loi?: string }>;
}) {
  const q = await searchParams;
  const setting = (await db.select().from(classSettings).limit(1))[0];
  const ready = Boolean(docKhoaCloud(setting?.aiApiKey));
  const mask = maskKey(setting?.aiApiKey);
  const oauthSanSang = oauthDaDangKy();
  return (
    <main className="max-w-xl">
      <PageHeader
        kicker="Gia sư"
        title="Kết nối ChatGPT"
        description="Thầy cô kết nối một lần. Học sinh không thấy khóa, không làm việc kỹ thuật."
      />

      {q.loi ? (
        <p className="mb-6 bg-amber-50 px-4 py-3 text-sm text-amber-950" data-testid="ket-noi-loi">
          {q.loi}
        </p>
      ) : null}
      {q.ok ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass" data-testid="ket-noi-ok">
          Đã kết nối khóa API. Gia sư lớp dùng nhà cloud. Vẫn lọc lộ đáp án.
        </p>
      ) : null}
      {q.oauth ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass" data-testid="ket-noi-oauth">
          Đã xác nhận tài khoản ChatGPT (định danh). Bước còn lại: dán khóa API cùng tài khoản để gọi mô hình.
        </p>
      ) : null}

      <section className="border-y border-line py-6" data-testid="ket-noi-chatgpt">
        <p className="text-sm font-medium">Trạng thái lớp</p>
        <p className="mt-2 text-sm text-muted" data-testid="ket-noi-trang-thai">
          {ready
            ? `Đã kết nối khóa chính thức${mask ? ` · ${mask}` : ""}${setting?.aiOpenaiEmail ? ` · ${setting.aiOpenaiEmail}` : ""}.`
            : setting?.aiOpenaiEmail
              ? `Đã đăng nhập ChatGPT (${setting.aiOpenaiEmail}) — chưa có khóa gọi mô hình.`
              : "Chưa kết nối. Lớp đang dùng thang gợi ý đã kiểm."}
        </p>

        <ol className="mt-6 space-y-4 text-sm">
          <li>
            <p className="font-medium">1. Mở trang khóa OpenAI</p>
            <p className="mt-1 text-muted">
              Tài khoản ChatGPT và OpenAI Platform là một. Thầy cô bấm, đăng nhập như vào ChatGPT, tạo một khóa, sao chép.
            </p>
            <a
              href={OPENAI_KEYS_PAGE}
              target="_blank"
              rel="noreferrer"
              className={cn(buttonClasses(), "mt-3")}
              data-testid="mo-trang-khoa-openai"
            >
              Mở trang khóa OpenAI
            </a>
          </li>
          <li>
            <p className="font-medium">2. Dán khóa — một lần</p>
            <form action={ketNoiBangKhoa} className="mt-3 space-y-3">
              <label className="block">
                <span className="sr-only">Khóa API</span>
                <input
                  name="ai_api_key"
                  type="password"
                  autoComplete="off"
                  required
                  data-testid="ket-noi-khoa"
                  className={fieldControl}
                  placeholder={mask ? `${mask} — dán khóa mới để thay` : "sk-… chỉ dán ở đây, không gửi cho học sinh"}
                />
              </label>
              <label className="block">
                <span className="mb-2 block font-medium">Mô hình (tùy chọn)</span>
                <input name="ai_model" data-testid="ket-noi-model" className={fieldControl} placeholder="gpt-4o-mini" />
              </label>
              <Button type="submit" data-testid="ket-noi-luu">
                Kết nối cho cả lớp
              </Button>
            </form>
          </li>
        </ol>

        <div className="mt-8 border-t border-line pt-6">
          <p className="text-sm font-medium">Đăng nhập ChatGPT (OAuth chính thức)</p>
          <p className="mt-2 text-sm text-muted">
            OpenAI (2026) «Sign in with ChatGPT» là định danh — tên và email — cho ứng dụng đã được OpenAI cấp{" "}
            <span className="font-mono">client_id</span>. Nó không thay khóa API, và không phải device-OAuth Codex/CLI.
          </p>
          {oauthSanSang ? (
            <a href="/gv/ket-noi-ai/oauth" className={cn(buttonClasses({ variant: "secondary" }), "mt-3")} data-testid="oauth-chatgpt">
              Tiếp tục với ChatGPT
            </a>
          ) : (
            <p className="mt-3 text-sm text-muted" data-testid="oauth-chua-dk">
              Trường chưa có <span className="font-mono">OPENAI_OAUTH_CLIENT_ID</span>. Hai bước trên là đường chính thức cho lớp thử.
            </p>
          )}
        </div>

        {ready || setting?.aiOpenaiEmail ? (
          <form action={ngatKetNoiAi} className="mt-6">
            <Button type="submit" variant="ghost" data-testid="ngat-ket-noi">
              Ngắt kết nối
            </Button>
          </form>
        ) : null}
      </section>

      <p className="mt-6 text-sm text-muted">
        <Link href="/gv/cai-dat" className="underline underline-offset-2">
          Về cài đặt lớp
        </Link>
        {" · "}
        Gia sư chỉ đọc kho tài liệu và công thức đã duyệt, không đọc lời giải.
      </p>
    </main>
  );
}
