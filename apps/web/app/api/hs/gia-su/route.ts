import { getSession } from "@/lib/auth";
import { chayHoiGiaSu } from "@/lib/gia-su-luot";
import { vietSse } from "@/lib/sse";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

const SSE = {
  "content-type": "text/event-stream; charset=utf-8",
  "cache-control": "no-cache, no-transform",
  connection: "keep-alive",
  "x-accel-buffering": "no",
};

/** POST: SSE trạng thái (kho → gọi → lọc → xong). Không stream token chưa lọc. */
export async function POST(req: Request) {
  const user = await getSession();
  if (!user || !user.roles.includes("HS")) {
    return new Response(JSON.stringify({ ok: false, tra_loi: "Chưa vào lớp." }), { status: 401 });
  }
  let body: { problemId?: string; text?: string; provider?: string; model?: string; maBuoc?: string };
  try {
    body = (await req.json()) as typeof body;
  } catch {
    return new Response(JSON.stringify({ ok: false, tra_loi: "Thiếu câu hỏi." }), { status: 400 });
  }
  const problemId = String(body.problemId || "").trim();
  const text = String(body.text || "").trim();
  if (!problemId || !text) {
    return new Response(JSON.stringify({ ok: false, tra_loi: "Thiếu câu hỏi." }), { status: 400 });
  }

  const stream = new ReadableStream({
    async start(controller) {
      const enc = new TextEncoder();
      const gui = (event: string, data: unknown) => {
        if (req.signal.aborted) return;
        controller.enqueue(enc.encode(vietSse(event, data)));
      };
      try {
        const ket = await chayHoiGiaSu({
          user,
          problemId,
          text,
          provider: body.provider,
          model: body.model,
          maBuoc: typeof body.maBuoc === "string" ? body.maBuoc : undefined,
          signal: req.signal,
          onTrangThai: (buoc) => gui("trang_thai", { buoc }),
        });
        if (!req.signal.aborted) gui("xong", ket);
      } catch {
        if (!req.signal.aborted) {
          gui("loi", { ok: false, tra_loi: "Gia sư đang bận. Em cứ sửa bước được tô và nộp lại.", offline: true, provider: "offline" });
        }
      } finally {
        try {
          controller.close();
        } catch {
          /* đã đóng */
        }
      }
    },
    cancel() {
      /* trình duyệt Dừng */
    },
  });

  return new Response(stream, { headers: SSE });
}
