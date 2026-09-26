"use client";

import { useMemo, useState } from "react";
import { hoiGiaSu, nopBuoc, type StepPayload } from "@/lib/actions/hs";
import { BUOC } from "@/lib/levels";
import { MathInput } from "./math-input";
import { Tex } from "./tex";

type Grade = {
  ket_qua: string;
  loai_ket_qua: string;
  buoc_sai: { ma_buoc: string; dong: number | null; o: { hang: string; k: number | null } | null } | null;
  thong_bao: string;
  per_buoc?: Record<string, string>;
  finished?: boolean;
};

type Ev = StepPayload["events"][number];

const ORDER = BUOC.map((b) => b.ma);

export function SolveClient({
  problemId,
  title,
  latex,
}: {
  problemId: string;
  title: string;
  latex: string;
}) {
  const [step, setStep] = useState(0);
  const [txd, setTxd] = useState("");
  const [dh, setDh] = useState<string[]>([""]);
  const [roots, setRoots] = useState<{ latex: string; loai: "NGHIEM" | "KHONG_XD" }[]>([{ latex: "", loai: "NGHIEM" }]);
  const [points, setPoints] = useState<string[]>([]);
  const [draftPoint, setDraftPoint] = useState("");
  const [signs, setSigns] = useState<Record<number, string>>({});
  const [arrows, setArrows] = useState<Record<number, string>>({});
  const [kl, setKl] = useState({ db: "", nb: "", cd: "", ct: "" });
  const [events, setEvents] = useState<Ev[]>([]);
  const [grade, setGrade] = useState<Grade | null>(null);
  const [busy, setBusy] = useState(false);
  const [chat, setChat] = useState<{ role: "hs" | "gia_su"; text: string }[]>([
    {
      role: "gia_su",
      text: "Mình là gia sư AI. Mình sửa bài và giảng cho em hiểu, không đưa đáp án trong lúc làm.",
    },
  ]);
  const [ask, setAsk] = useState("");
  const [openTutor, setOpenTutor] = useState(false);

  const sorted = useMemo(() => {
    return [...points].sort((a, b) => parseFloat(a.replace(",", ".")) - parseFloat(b.replace(",", ".")));
  }, [points]);

  function log(ev: Ev) {
    setEvents((xs) => [...xs, ev]);
  }

  function payload(nopToi: string): StepPayload {
    const until = ORDER.indexOf(nopToi as (typeof ORDER)[number]);
    const cac_buoc: StepPayload["cac_buoc"] = [];
    if (until >= 0) cac_buoc.push({ ma_buoc: "B.DH.TXD", cac_dong: [{ dong: 0, latex: txd }] });
    if (until >= 1) {
      const lines = dh.map((latex, i) => ({ dong: i, latex })).filter((l) => l.latex.trim());
      cac_buoc.push({ ma_buoc: "B.DH.DAOHAM", cac_dong: lines.length ? lines : [{ dong: 0, latex: "" }] });
    }
    if (until >= 2) {
      const lines = roots
        .filter((r) => r.latex.trim())
        .map((r, i) => ({ dong: i, latex: r.loai === "KHONG_XD" ? `y' không xác định tại ${r.latex}` : r.latex, loai: r.loai }));
      cac_buoc.push({
        ma_buoc: "B.DH.NGHIEM",
        cac_dong: lines.length ? lines : [{ dong: 0, latex: "không có nghiệm", loai: "NGHIEM" }],
      });
    }
    if (until >= 3) {
      const cells: { hang: string; k: number; gia_tri: string }[] = [];
      sorted.forEach((p, i) => cells.push({ hang: "X", k: i, gia_tri: p }));
      for (let j = 0; j <= sorted.length; j++) cells.push({ hang: "DAU_YPHAY", k: 2 * j, gia_tri: signs[2 * j] || "" });
      for (let j = 0; j < sorted.length; j++) {
        if (signs[2 * j + 1]) cells.push({ hang: "DAU_YPHAY", k: 2 * j + 1, gia_tri: signs[2 * j + 1] });
      }
      for (let j = 0; j <= sorted.length; j++) {
        if (arrows[2 * j]) cells.push({ hang: "BIEN_THIEN", k: 2 * j, gia_tri: arrows[2 * j] });
      }
      cac_buoc.push({ ma_buoc: "B.DH.XETDAU", bang: { loai_bang: "XET_DAU", cac_o: cells } });
    }
    if (until >= 4) {
      const lines: { dong: number; latex: string }[] = [];
      const khai: string[] = [];
      const push = (key: string, text: string) => {
        khai.push(key);
        lines.push({ dong: lines.length, latex: text || `không ${key}` });
      };
      push("dong_bien", kl.db);
      push("nghich_bien", kl.nb);
      push("cuc_dai", kl.cd);
      push("cuc_tieu", kl.ct);
      cac_buoc.push({ ma_buoc: "B.DH.KETLUAN", khai_bao: khai, cac_dong: lines });
    }
    return { nop_toi: nopToi, cac_buoc, events };
  }

  async function submit() {
    setBusy(true);
    const ma = ORDER[step];
    const res = await nopBuoc(problemId, payload(ma));
    setBusy(false);
    if (!res.ok) {
      setGrade({ ket_qua: "KHONG_KIEM_DUOC", loai_ket_qua: "KHONG_KIEM_DUOC", buoc_sai: null, thong_bao: res.thong_bao });
      return;
    }
    setGrade(res);
    if (res.ket_qua === "DAT" && !res.finished && step < ORDER.length - 1) setStep(step + 1);
  }

  async function sendChat() {
    const text = ask.trim();
    if (!text) return;
    setAsk("");
    setChat((c) => [...c, { role: "hs", text }]);
    const res = await hoiGiaSu(problemId, text);
    setChat((c) => [...c, { role: "gia_su", text: res.ok ? res.tra_loi : res.tra_loi }]);
  }

  const badStep = grade?.ket_qua === "SAI" ? grade.buoc_sai?.ma_buoc : grade?.ket_qua === "KHONG_KIEM_DUOC" ? grade.buoc_sai?.ma_buoc : null;

  function lineBad(ma: string, dong: number) {
    return badStep === ma && grade?.buoc_sai?.dong === dong;
  }
  function cellBad(hang: string, k: number) {
    const o = grade?.buoc_sai?.o;
    return badStep === "B.DH.XETDAU" && o?.hang === hang && o?.k === k;
  }

  const ma = ORDER[step];

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
      <section className="rounded-2xl border border-stone-300 bg-white p-4 shadow-sm" data-testid="solve-screen">
        <h1 className="text-lg font-bold">Làm bài theo 5 bước</h1>
        <p className="mt-2 text-sm leading-relaxed">{title}</p>
        <div className="mt-2 rounded-xl bg-stone-50 px-3 py-2">
          <Tex tex={latex} />
        </div>
        <div className="mt-3 flex gap-1 overflow-x-auto pb-1">
          {BUOC.map((b, i) => (
            <button
              key={b.ma}
              type="button"
              data-testid={`step-${b.ma}`}
              onClick={() => setStep(i)}
              className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ${
                badStep === b.ma ? "bg-red-700 text-white" : i === step ? "bg-navy text-white" : "bg-stone-100"
              }`}
            >
              {i + 1}. {b.ten}
            </button>
          ))}
        </div>

        <div className="mt-4 space-y-3">
          {ma === "B.DH.TXD" && (
            <div className={lineBad("B.DH.TXD", 0) ? "cell-bad rounded-xl p-2" : ""}>
              <MathInput testId="latex-txd" label="Tập xác định" value={txd} onChange={setTxd} />
            </div>
          )}
          {ma === "B.DH.DAOHAM" && (
            <div className="space-y-2">
              {dh.map((line, i) => (
                <div key={i} className={lineBad("B.DH.DAOHAM", i) ? "cell-bad rounded-xl p-2" : ""}>
                  <MathInput
                    testId={i === 0 ? "latex-dh" : `latex-dh-${i}`}
                    label={`Dòng ${i + 1} của đạo hàm`}
                    value={line}
                    onChange={(v) => setDh(dh.map((x, j) => (j === i ? v : x)))}
                  />
                </div>
              ))}
              <button type="button" className="text-sm text-navy" onClick={() => setDh([...dh, ""])}>
                Thêm dòng biến đổi
              </button>
            </div>
          )}
          {ma === "B.DH.NGHIEM" && (
            <div className={`space-y-2 ${badStep === "B.DH.NGHIEM" ? "cell-bad rounded-xl p-2" : ""}`} data-testid="nghiem-block">
              {roots.map((r, i) => (
                <div key={i} className="flex flex-col gap-1 sm:flex-row sm:items-end">
                  <div className="flex-1">
                    <MathInput
                      testId={i === 0 ? "latex-nghiem" : `latex-nghiem-${i}`}
                      label={r.loai === "KHONG_XD" ? "Điểm y′ không xác định" : "Nghiệm y′ = 0"}
                      value={r.latex}
                      onChange={(v) => setRoots(roots.map((x, j) => (j === i ? { ...x, latex: v } : x)))}
                    />
                  </div>
                  <button
                    type="button"
                    className="rounded-lg border px-2 py-1 text-xs"
                    onClick={() =>
                      setRoots(roots.map((x, j) => (j === i ? { ...x, loai: x.loai === "NGHIEM" ? "KHONG_XD" : "NGHIEM" } : x)))
                    }
                  >
                    {r.loai === "NGHIEM" ? "Đổi thành điểm không xác định" : "Đổi thành nghiệm"}
                  </button>
                </div>
              ))}
              <button type="button" data-testid="them-nghiem" className="text-sm text-navy" onClick={() => setRoots([...roots, { latex: "", loai: "NGHIEM" }])}>
                Thêm dòng nghiệm
              </button>
            </div>
          )}
          {ma === "B.DH.XETDAU" && (
            <div data-testid="bang-xet-dau">
              <p className="text-sm text-slate-600">
                Em tự ghi các mốc trên hàng x. Hai đầu −∞ và +∞ là khung bảng, không phải điểm tới hạn. Ứng dụng không thêm mốc và không báo đúng sai từng ô khi đang gõ.
              </p>
              <div className="mt-2 flex gap-2">
                <input
                  data-testid="moc-nhap"
                  value={draftPoint}
                  onChange={(e) => setDraftPoint(e.target.value)}
                  className="w-28 rounded-lg border px-2 py-1"
                  placeholder="mốc x"
                />
                <button
                  type="button"
                  data-testid="moc-them"
                  className="rounded-lg bg-stone-800 px-3 py-1 text-sm text-white"
                  onClick={() => {
                    const v = draftPoint.trim();
                    if (!v) return;
                    log({
                      ma_buoc: "B.DH.XETDAU",
                      o: { hang: "X", k: points.length },
                      gia_tri_cu: null,
                      gia_tri_moi: v,
                      thoi_diem: new Date().toISOString(),
                    });
                    setPoints([...points, v]);
                    setDraftPoint("");
                  }}
                >
                  Thêm mốc
                </button>
              </div>
              <div className="mt-3 overflow-x-auto">
                <table className="w-full min-w-[320px] border-collapse text-center text-sm">
                  <tbody>
                    <tr>
                      <th className="p-1 text-left">x</th>
                      {Array.from({ length: sorted.length * 2 + 1 }, (_, k) => {
                        if (k % 2 === 1) {
                          const p = sorted[(k - 1) / 2];
                          return (
                            <td key={k} className="p-1 font-semibold">
                              {p}
                              <button type="button" className="ml-1 text-xs text-red-700" onClick={() => setPoints(points.filter((x) => x !== p))}>
                                xóa
                              </button>
                            </td>
                          );
                        }
                        if (k === 0) return <td key={k}>−∞</td>;
                        if (k === sorted.length * 2) return <td key={k}>+∞</td>;
                        return <td key={k} />;
                      })}
                    </tr>
                    <tr>
                      <th className="p-1 text-left">y′</th>
                      {Array.from({ length: sorted.length * 2 + 1 }, (_, k) => {
                        const pointCell = k % 2 === 1;
                        const cur = signs[k];
                        return (
                          <td key={k} className={`p-1 ${cellBad("DAU_YPHAY", k) ? "cell-bad" : ""}`}>
                            <div className="flex justify-center gap-0.5">
                              {(pointCell ? ["0", "||"] : ["+", "−"]).map((opt) => (
                                <button
                                  key={opt}
                                  type="button"
                                  data-testid={`dau-${k}-${opt === "−" ? "-" : opt}`}
                                  className={`rounded px-1.5 py-0.5 text-xs ${cur === (opt === "−" ? "-" : opt) ? "bg-navy text-white" : "bg-stone-100"}`}
                                  onClick={() => {
                                    const val = opt === "−" ? "-" : opt;
                                    log({
                                      ma_buoc: "B.DH.XETDAU",
                                      o: { hang: "DAU_YPHAY", k },
                                      gia_tri_cu: cur || null,
                                      gia_tri_moi: val,
                                      thoi_diem: new Date().toISOString(),
                                    });
                                    setSigns({ ...signs, [k]: val });
                                  }}
                                >
                                  {opt}
                                </button>
                              ))}
                            </div>
                          </td>
                        );
                      })}
                    </tr>
                    <tr>
                      <th className="p-1 text-left">chiều</th>
                      {Array.from({ length: sorted.length * 2 + 1 }, (_, k) =>
                        k % 2 === 1 ? (
                          <td key={k} />
                        ) : (
                          <td key={k} className={`p-1 ${cellBad("BIEN_THIEN", k) ? "cell-bad" : ""}`}>
                            <div className="flex justify-center gap-0.5">
                              {[
                                ["TANG", "↗"],
                                ["GIAM", "↘"],
                              ].map(([val, icon]) => (
                                <button
                                  key={val}
                                  type="button"
                                  data-testid={`mui-${k}-${val}`}
                                  className={`rounded px-1.5 py-0.5 text-xs ${arrows[k] === val ? "bg-teal text-white" : "bg-stone-100"}`}
                                  onClick={() => {
                                    log({
                                      ma_buoc: "B.DH.XETDAU",
                                      o: { hang: "BIEN_THIEN", k },
                                      gia_tri_cu: arrows[k] || null,
                                      gia_tri_moi: val,
                                      thoi_diem: new Date().toISOString(),
                                    });
                                    setArrows({ ...arrows, [k]: val });
                                  }}
                                >
                                  {icon}
                                </button>
                              ))}
                            </div>
                          </td>
                        ),
                      )}
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}
          {ma === "B.DH.KETLUAN" && (
            <div className={`space-y-2 ${badStep === "B.DH.KETLUAN" ? "cell-bad rounded-xl p-2" : ""}`}>
              {(
                [
                  ["db", "latex-db", "Đồng biến"],
                  ["nb", "latex-nb", "Nghịch biến"],
                  ["cd", "latex-cd", "Cực đại"],
                  ["ct", "latex-ct", "Cực tiểu"],
                ] as const
              ).map(([key, id, label]) => (
                <label key={key} className="block text-sm">
                  <span className="font-medium">{label}</span>
                  <input
                    data-testid={id}
                    value={kl[key]}
                    onChange={(e) => setKl({ ...kl, [key]: e.target.value })}
                    className="mt-1 w-full rounded-lg border px-2 py-1.5"
                    placeholder={
                      key === "db"
                        ? "đồng biến trên (...; ...) và (...; ...)"
                        : key === "nb"
                          ? "nghịch biến trên (...; ...)"
                          : key === "cd"
                            ? "cực đại tại x = ..., y = ..."
                            : "cực tiểu tại x = ..., y = ..."
                    }
                  />
                </label>
              ))}
            </div>
          )}
        </div>

        {grade ? (
          <p
            data-testid="cham-thong-bao"
            className={`mt-4 rounded-xl px-3 py-2 text-sm ${grade.ket_qua === "DAT" ? "bg-emerald-50 text-emerald-900" : "bg-amber-50 text-amber-950"}`}
          >
            {grade.finished ? "Em đã hoàn thành bài này." : grade.thong_bao}
          </p>
        ) : null}

        <button
          type="button"
          data-testid="nop-buoc"
          disabled={busy}
          onClick={submit}
          className="mt-4 w-full rounded-xl bg-navy px-4 py-3 font-semibold text-white disabled:opacity-60"
        >
          {busy ? "Đang chấm..." : `Nộp bước ${BUOC[step].ten}`}
        </button>
      </section>

      <aside className={`rounded-2xl border border-stone-300 bg-white p-4 shadow-sm ${openTutor ? "block" : "hidden lg:block"}`} data-testid="tutor-panel">
        <p className="text-xs font-semibold uppercase text-teal">Gia sư AI</p>
        <p className="text-xs text-slate-500">Đang tương tác với AI, không phải giáo viên. Không có lời giải chuẩn trong hội thoại này.</p>
        <div data-testid="tutor-log" className="mt-3 max-h-80 space-y-2 overflow-y-auto">
          {chat.map((m, i) => (
            <p key={i} className={`rounded-xl px-3 py-2 text-sm ${m.role === "hs" ? "bg-navy text-white" : "bg-stone-100"}`}>
              {m.text}
            </p>
          ))}
        </div>
        <div className="mt-3 flex gap-2">
          <input
            data-testid="tutor-input"
            value={ask}
            onChange={(e) => setAsk(e.target.value)}
            className="flex-1 rounded-xl border px-2 py-2 text-sm"
            placeholder="Hỏi gợi ý, không hỏi đáp án"
          />
          <button type="button" data-testid="tutor-send" className="rounded-xl bg-teal px-3 text-sm font-semibold text-white" onClick={sendChat}>
            Gửi
          </button>
        </div>
      </aside>
      <button
        type="button"
        data-testid="mo-gia-su"
        className="fixed bottom-4 right-4 rounded-full bg-teal px-4 py-3 text-sm font-semibold text-white shadow-lg lg:hidden"
        onClick={() => setOpenTutor((v) => !v)}
      >
        {openTutor ? "Đóng gia sư" : "Hỏi gia sư"}
      </button>
    </div>
  );
}
