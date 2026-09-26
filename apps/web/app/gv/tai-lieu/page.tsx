import { desc } from "drizzle-orm";
import { taiTaiLieu } from "@/lib/actions/gv";
import { db } from "@/lib/db";
import { documents } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export default async function Page() {
  const docs = await db.select().from(documents).orderBy(desc(documents.createdAt));
  return (
    <main className="grid gap-4 lg:grid-cols-2">
      <form action={taiTaiLieu} className="space-y-2 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
        <h1 className="text-xl font-bold">Nạp tài liệu</h1>
        <input name="title" required placeholder="Tên tài liệu" className="w-full rounded-lg border px-2 py-1.5" />
        <select name="kind" className="w-full rounded-lg border px-2 py-1.5">
          <option value="tu_soan">Tự soạn</option>
          <option value="de_mau">Đề mẫu</option>
          <option value="tham_khao">Tham khảo</option>
        </select>
        <select name="license" className="w-full rounded-lg border px-2 py-1.5">
          <option value="tu_soan">Tự soạn</option>
          <option value="cong_khai">Công khai</option>
          <option value="chua_ro">Chưa rõ quyền — không dùng ở tầng 2</option>
        </select>
        <textarea name="text" rows={6} placeholder="Dán văn bản" className="w-full rounded-lg border px-2 py-1.5" />
        <input name="file" type="file" accept=".pdf,.txt,.md" className="text-sm" />
        <button className="rounded-xl bg-clay px-4 py-2 font-semibold text-white" type="submit">
          Lưu
        </button>
      </form>
      <ul className="space-y-2">
        {docs.map((d) => (
          <li key={d.id} className="rounded-2xl bg-white p-3 text-sm shadow-sm ring-1 ring-stone-200">
            <p className="font-semibold">{d.title}</p>
            <p className="text-xs text-slate-500">
              {d.kind} · quyền {d.licenseStatus} · {d.textContent.length} ký tự
            </p>
          </li>
        ))}
      </ul>
    </main>
  );
}
