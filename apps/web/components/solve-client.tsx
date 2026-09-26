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
  moLoiGiai = false,
  loiGiai = null,
}: {
  problemId: string;
  title: string;
  latex: string;
  moLoiGiai?: boolean;
  loiGiai?: string | null;
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
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_280px]">
      <section data-testid="solve-screen">
        <h1 className="text-pretty text-xl font-semibold">Làm bài theo 5 bước</h1>
        <p className="mt-2 text-sm leading-relaxed">{title}</p>
        <div className="mt-3 border-y border-line bg-wash px-3 py-2">
          <Tex tex={latex} />
        </div>
        <ol className="mt-4 flex gap-0 overflow-x-auto border-y border-line lg:flex-col lg:border-0">
          {BUOC.map((b, i) => (
            <li key={b.ma} className="shrink-0 lg:shrink">
              <button
                type="button"
                data-testid={`step-${b.ma}`}
                onClick={() => setStep(i)}
                className={`flex w-full items-center gap-2 px-3 py-2 text-left text-sm ${
                  badStep === b.ma
                    ? "bg-red-50 font-medium text-mark"
                    : i === step
                      ? "bg-ink text-chalk"
                      : i < step
                        ? "text-ink"
                        : "text-muted"
                }`}
              >
                <span className="tabular font-mono text-xs">{i + 1}</span>
                {b.ten}
              </button>
            </li>
          ))}
        </ol>

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
              <button type="button" className="text-sm underline underline-offset-2" onClick={() => setDh([...dh, ""])}>
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
                    className="rounded-button border border-line px-2 py-1 text-xs hover:bg-paper"
                    onClick={() =>
                      setRoots(roots.map((x, j) => (j === i ? { ...x, loai: x.loai === "NGHIEM" ? "KHONG_XD" : "NGHIEM" } : x)))
                    }
                  >
                    {r.loai === "NGHIEM" ? "Đổi thành điểm không xác định" : "Đổi thành nghiệm"}
                  </button>
                </div>
              ))}
              <button type="button" data-testid="them-nghiem" className="text-sm underline underline-offset-2" onClick={() => setRoots([...roots, { latex: "", loai: "NGHIEM" }])}>
                Thêm dòng nghiệm
              </button>
            </div>
          )}
          {ma === "B.DH.XETDAU" && (
            <div data-testid="bang-xet-dau">
              <p className="text-sm text-muted">
                Em tự ghi các mốc trên hàng x. Hai đầu −∞ và +∞ là khung bảng, không phải điểm tới hạn. Ứng dụng không thêm mốc và không báo đúng sai từng ô khi đang gõ.
              </p>
              <div className="mt-2 flex gap-2">
                <input
                  data-testid="moc-nhap"
                  value={draftPoint}
                  onChange={(e) => setDraftPoint(e.target.value)}
                  className="w-28 rounded-button border border-line px-2 py-1"
                  placeholder="mốc x…"
                  aria-label="Mốc x"
                />
                <button
                  type="button"
                  data-testid="moc-them"
                  className="rounded-button bg-ink px-3 py-1 text-sm text-chalk hover:bg-primary-hover"
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
                              <button type="button" className="ml-1 text-xs text-danger hover:underline" onClick={() => setPoints(points.filter((x) => x !== p))}>
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
                                                  className={`rounded px-1.5 py-0.5 text-xs ${cur === (opt === "−" ? "-" : opt) ? "bg-ink text-chalk" : "bg-wash"}`}
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
                                  className={`rounded px-1.5 py-0.5 text-xs ${arrows[k] === val ? "bg-pass text-white" : "bg-wash"}`}
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
                    className="mt-1 w-full rounded-button border border-line px-2 py-1.5"
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
            aria-live="polite"
            className={`mt-4 px-3 py-2 text-sm ${grade.ket_qua === "DAT" ? "bg-pass/10 text-pass" : "bg-amber-50 text-amber-950"}`}
          >
            {grade.finished ? "Em đã hoàn thành bài này." : grade.thong_bao}
          </p>
        ) : null}

        {grade?.finished && moLoiGiai && loiGiai ? (
          <div data-testid="loi-giai-sau-nop" className="mt-3 rounded-xl bg-paper px-3 py-2 text-sm leading-relaxed">
            <p className="font-semibold">Lời giải lớp cho phép xem sau khi nộp</p>
            <p className="mt-1">{loiGiai}</p>
          </div>
        ) : null}

        <button
          type="button"
          data-testid="nop-buoc"
          disabled={busy}
          onClick={submit}
          className="mt-4 w-full rounded-button bg-ink px-4 py-3 font-medium text-chalk hover:bg-primary-hover disabled:opacity-60"
        >
          {busy ? "Đang chấm…" : `Nộp bước ${BUOC[step].ten}`}
        </button>
      </section>

      <aside className={`border-t border-line pt-4 lg:border-t-0 lg:pt-0 ${openTutor ? "block" : "hidden lg:block"}`} data-testid="tutor-panel">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-sm font-semibold">Gia sư AI</p>
            <p className="text-xs text-muted">Đang tương tác với AI, không phải giáo viên. Không có lời giải chuẩn trong hội thoại này.</p>
          </div>
          <button
            type="button"
            className="text-sm underline underline-offset-2 lg:hidden"
            onClick={() => setOpenTutor(false)}
          >
            Đóng
          </button>
        </div>
        <div data-testid="tutor-log" className="mt-3 max-h-80 space-y-2 overflow-y-auto">
          {chat.map((m, i) => (
            <p key={i} className={`px-3 py-2 text-sm ${m.role === "hs" ? "bg-ink text-chalk" : "bg-wash"}`}>
              {m.text}
            </p>
          ))}
        </div>
        <div className="mt-3 flex gap-2">
          <label className="sr-only" htmlFor="tutor-input">
            Câu hỏi cho gia sư
          </label>
          <input
            id="tutor-input"
            data-testid="tutor-input"
            value={ask}
            onChange={(e) => setAsk(e.target.value)}
            className="min-w-0 flex-1 rounded-button border border-line px-2 py-2 text-sm"
            placeholder="Hỏi gợi ý, không hỏi đáp án…"
          />
          <button type="button" data-testid="tutor-send" className="rounded-button bg-ink px-3 text-sm font-medium text-chalk hover:bg-primary-hover" onClick={sendChat}>
            Gửi
          </button>
        </div>
      </aside>
      {!openTutor ? (
        <button
          type="button"
          data-testid="mo-gia-su"
          className="fixed bottom-4 right-4 z-20 rounded-button bg-ink px-4 py-3 text-sm font-medium text-chalk shadow-lg lg:hidden"
          onClick={() => setOpenTutor(true)}
        >
          Hỏi gia sư
        </button>
      ) : null}
    </div>
  );
}
