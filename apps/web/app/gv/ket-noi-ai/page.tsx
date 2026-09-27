import Link from "next/link";
import { ketNoiBangKhoa, ngatKetNoiAi } from "@/lib/actions/gv";
import { maskKey } from "@/lib/ai-catalog";
import { docKhoaCloud } from "@/lib/ai-harness";
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
  const khung = xemKhoTheoKhung(await taiNguyenKhoLop());
  return (
    <main className="max-w-xl">
      <PageHeader
        kicker="Gia sư"
        title="Kết nối ChatGPT"
        description="Thầy cô làm một lần. Học sinh chỉ thấy gia sư — không thấy khóa, không cài phần mềm."
      />

      {q.loi ? (
        <p className="mb-6 bg-amber-50 px-4 py-3 text-sm text-amber-950" data-testid="ket-noi-loi">
          {q.loi}
        </p>
      ) : null}
      {q.ok ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass" data-testid="ket-noi-ok">
          Đã kết nối. Học sinh hỏi gia sư là dùng ChatGPT của lớp. Vẫn lọc lộ đáp án.
        </p>
      ) : null}
      {q.oauth ? (
        <p className="mb-6 bg-pass/10 px-4 py-3 text-sm text-pass" data-testid="ket-noi-oauth">
          Đã xác nhận tài khoản ChatGPT (tên và email). Bước còn lại: dán khóa cùng tài khoản để gọi mô hình.
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

        {ready ? (
          <p className="mt-3 text-sm">Học sinh không làm việc kỹ thuật. Gia sư vẫn chỉ đọc kho đã duyệt, không đọc lời giải.</p>
        ) : (
          <ol className="mt-6 space-y-6 text-sm">
            <li className="flex gap-4">
              <span className="tabular flex size-8 shrink-0 items-center justify-center bg-ink text-sm font-medium text-chalk">
                1
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-medium">Mở ChatGPT</p>
                <p className="mt-1 text-muted">
                  Tài khoản ChatGPT và OpenAI là một. Thầy cô đăng nhập như vào ChatGPT, tạo một khóa, sao chép.
                </p>
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
                <form action={ketNoiBangKhoa} className="mt-3 space-y-4">
                  <label className="block">
                    <span className="sr-only">Khóa API</span>
                    <input
                      name="ai_api_key"
                      type="password"
                      autoComplete="off"
                      required
                      data-testid="ket-noi-khoa"
                      className={fieldControl}
                      placeholder={mask ? `${mask} — dán khóa mới để thay` : "Dán khóa vừa sao chép — không gửi cho học sinh"}
                    />
                  </label>
                  <details className="text-sm">
                    <summary className="cursor-pointer font-medium">Mô hình (để trống = mặc định)</summary>
                    <input
                      name="ai_model"
                      data-testid="ket-noi-model"
                      className={`${fieldControl} mt-2`}
                      placeholder="gpt-4o-mini"
                    />
                  </details>
                  <Button type="submit" data-testid="ket-noi-luu">
                    Kết nối cho cả lớp
                  </Button>
                </form>
              </div>
            </li>
          </ol>
        )}

        {ready ? (
          <details className="mt-6 text-sm">
            <summary className="cursor-pointer font-medium">Đổi khóa</summary>
            <form action={ketNoiBangKhoa} className="mt-3 space-y-4">
              <label className="block">
                <span className="sr-only">Khóa API</span>
                <input
                  name="ai_api_key"
                  type="password"
                  autoComplete="off"
                  required
                  className={fieldControl}
                  placeholder={`${mask || "sk-…"} — dán khóa mới`}
                />
              </label>
              <Button type="submit">Lưu khóa mới</Button>
            </form>
          </details>
        ) : null}

        <div className="mt-8 border-t border-line pt-6">
          <p className="text-sm font-medium">Đăng nhập ChatGPT (chỉ tên và email)</p>
          <p className="mt-2 text-sm text-muted">
            «Sign in with ChatGPT» chính thức (2026) là định danh — không thay khóa để gọi mô hình, không phải đăng nhập Codex.
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

      <section className="border-b border-line py-6">
        <h2 className="text-base font-semibold">Gia sư sẽ đọc kho lớp</h2>
        <p className="mt-2 text-sm text-muted">Cùng nguồn tầng 2/3. Không đọc lời giải chuẩn. Kong IJCAI 2026: truy hồi rồi mới sinh.</p>
        <div className="mt-3">
          <KhoTheoBuoc khung={khung} />
        </div>
      </section>

      <p className="mt-6 text-sm text-muted">
        <Link href="/gv/cai-dat" className="underline underline-offset-2">
          Về cài đặt lớp
        </Link>
        {" · "}
        <Link href="/gv/tai-lieu" className="underline underline-offset-2">
          Tài liệu
        </Link>
      </p>
    </main>
  );
}
