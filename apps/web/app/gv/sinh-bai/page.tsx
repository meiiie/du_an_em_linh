import { sinhBienThe } from "@/lib/actions/gv";

export const dynamic = "force-dynamic";

export default function Page() {
  return (
    <main className="max-w-lg rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
      <h1 className="text-xl font-bold">Sinh biến thể tham số</h1>
      <p className="mt-1 text-sm text-slate-600">
        Bậc ba, trùng phương, hoặc phân thức bậc nhất. Lời giải do SymPy tính rồi đi qua cổng 3 tầng. Chưa đạt thì không phát hành cho học sinh.
      </p>
      <form
        action={async (fd) => {
          "use server";
          await sinhBienThe(fd);
        }}
        className="mt-3 space-y-2"
      >
        <select name="dang" className="w-full rounded-lg border px-2 py-1.5">
          <option value="bac_ba">Hàm bậc ba</option>
          <option value="trung_phuong">Hàm trùng phương</option>
          <option value="huu_ti">Phân thức bậc nhất</option>
        </select>
        <input name="seed" type="number" defaultValue={11} className="w-full rounded-lg border px-2 py-1.5" />
        <button className="rounded-xl bg-clay px-4 py-2 font-semibold text-white" type="submit">
          Sinh và kiểm định
        </button>
      </form>
    </main>
  );
}
