import { sinhBienThe } from "@/lib/actions/gv";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { fieldControl } from "@/components/ui/field";
import { STATUS_LABEL } from "@/lib/levels";

export const dynamic = "force-dynamic";

export default async function Page({ searchParams }: { searchParams: Promise<{ ma?: string; trang?: string; loi?: string }> }) {
  const sp = await searchParams;
  return (
    <main className="max-w-xl">
      <PageHeader
        kicker="Sinh bài"
        title="Sinh biến thể tham số"
        description="Bậc ba, trùng phương, hoặc phân thức bậc nhất. Lời giải do SymPy tính rồi đi qua cổng 3 tầng. Chưa đạt thì không phát hành."
      />
      {sp.loi ? <Card className="mb-4 text-sm text-rose-800">{sp.loi}</Card> : null}
      {sp.ma && sp.trang ? (
        <Card className="mb-4 text-sm" data-testid="ket-sinh">
          Đã sinh <span className="font-mono">{sp.ma}</span>{" "}
          <Badge tone={sp.trang === "DA_PHAT_HANH" ? "ok" : sp.trang === "BI_CHAN" ? "bad" : "warn"}>
            {STATUS_LABEL[sp.trang] || sp.trang}
          </Badge>
        </Card>
      ) : null}
      <Card>
        <form
          action={async (fd) => {
            "use server";
            await sinhBienThe(fd);
          }}
          className="space-y-3"
        >
          <select name="dang" className={fieldControl}>
            <option value="bac_ba">Hàm bậc ba</option>
            <option value="trung_phuong">Hàm trùng phương</option>
            <option value="huu_ti">Phân thức bậc nhất</option>
          </select>
          <input name="seed" type="number" defaultValue={11} className={fieldControl} />
          <button className="rounded-lg bg-clay px-4 py-2 font-semibold text-white" type="submit">
            Sinh và kiểm định
          </button>
        </form>
      </Card>
    </main>
  );
}
