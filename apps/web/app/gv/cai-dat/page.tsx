import { eq } from "drizzle-orm";
import { luuCaiDatLop } from "@/lib/actions/gv";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { db } from "@/lib/db";
import { classSettings, classes } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function Page() {
  const lop = (await db.select().from(classes))[0];
  const setting = lop ? (await db.select().from(classSettings).where(eq(classSettings.classId, lop.id)))[0] : null;
  return (
    <main className="max-w-xl">
      <PageHeader
        kicker="Lớp"
        title="Cài đặt lớp"
        description="Mở lời giải sau khi nộp mặc định tắt. Gia sư không bao giờ đọc lời giải chuẩn trong lúc học sinh đang làm."
      />
      <form action={luuCaiDatLop} className="space-y-3 border-y border-line py-5">
        <p className="text-sm text-muted">Lớp {lop?.name || "—"}</p>
        <label className="flex items-start gap-3 text-sm">
          <input
            name="mo_loi_giai"
            type="checkbox"
            defaultChecked={setting?.moLoiGiaiSauKhiNop === true}
            className="mt-1 h-4 w-4 accent-ink"
            data-testid="mo-loi-giai"
          />
          <span>
            <span className="font-medium">Mở lời giải sau khi nộp xong cả năm bước</span>
            <span className="mt-1 block text-muted">
              Chỉ hiện khi học sinh đã đạt bước kết luận. Không hiện trong hội thoại gia sư.
            </span>
          </span>
        </label>
        <Button type="submit">Lưu cài đặt</Button>
      </form>
    </main>
  );
}
