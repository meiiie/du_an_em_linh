import { sinhBienThe } from "@/lib/actions/gv";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Field, fieldControl } from "@/components/ui/field";
import { PageHeader } from "@/components/ui/page-header";
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
      {sp.loi ? <p className="mb-4 text-sm text-mark">{sp.loi}</p> : null}
      {sp.ma && sp.trang ? (
        <p className="mb-4 text-sm" data-testid="ket-sinh">
          Đã sinh{" "}
          <span className="font-mono" translate="no">
            {sp.ma}
          </span>{" "}
          <Badge tone={sp.trang === "DA_PHAT_HANH" ? "ok" : sp.trang === "BI_CHAN" ? "bad" : "warn"}>
            {STATUS_LABEL[sp.trang] || sp.trang}
          </Badge>
        </p>
      ) : null}
      <form
        action={async (fd) => {
          "use server";
          await sinhBienThe(fd);
        }}
        className="space-y-3 border-y border-line py-5"
      >
        <Field label="Dạng hàm">
          <select name="dang" className={fieldControl} autoComplete="off">
            <option value="bac_ba">Hàm bậc ba</option>
            <option value="trung_phuong">Hàm trùng phương</option>
            <option value="huu_ti">Phân thức bậc nhất</option>
          </select>
        </Field>
        <Field label="Hạt giống số">
          <input name="seed" type="number" defaultValue={11} className={fieldControl} autoComplete="off" />
        </Field>
        <Button type="submit">Sinh và kiểm định</Button>
      </form>
    </main>
  );
}
