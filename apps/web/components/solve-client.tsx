"use client";

import { useMemo, useState } from "react";
import { nopBuoc, type StepPayload } from "@/lib/actions/hs";
import type { AiPublicConfig } from "@/lib/ai-catalog";
import type { TrichDanHien } from "@/lib/kien-thuc";
import { loiBuoc, soBuoc, tenBuocNgan, tenBuocTrang } from "@/lib/de-hoc-sinh";
import { BUOC } from "@/lib/levels";
import { Button, buttonClasses } from "./ui/button";
import { fieldControl } from "./ui/field";
import { MathInput } from "./math-input";
import { Tex } from "./tex";
import { TutorPanel } from "./tutor-panel";
import { cn } from "@/lib/cn";

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

const AI_MAC_DINH: AiPublicConfig = {
  classProvider: "offline",
  classModel: null,
  allowLocal: true,
  cloudReady: false,
  openrouterReady: false,
  zaiReady: false,
};

export function SolveClient({
  problemId,
  title,
  latex,
  moLoiGiai = false,
  loiGiai = null,
  initialChat = [],
  ai = AI_MAC_DINH,
}: {
  problemId: string;
  title: string;
  latex: string;
  moLoiGiai?: boolean;
  loiGiai?: string | null;
  initialChat?: { role: "hs" | "gia_su"; text: string; trichDan?: TrichDanHien[] }[];
  ai?: AiPublicConfig;
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

  const badStep = grade?.ket_qua === "SAI" ? grade.buoc_sai?.ma_buoc : grade?.ket_qua === "KHONG_KIEM_DUOC" ? grade.buoc_sai?.ma_buoc : null;

  function lineBad(ma: string, dong: number) {
    return badStep === ma && grade?.buoc_sai?.dong === dong;
  }
  function cellBad(hang: string, k: number) {
    const o = grade?.buoc_sai?.o;
    return badStep === "B.DH.XETDAU" && o?.hang === hang && o?.k === k;
  }

  const ma = ORDER[step];
  const ten = tenBuocTrang(ma);
  const loi = loiBuoc(ma);
  const dat = grade?.ket_qua === "DAT";

  return (
    <>
    <div className="sm:grid sm:grid-cols-[11rem_minmax(0,1fr)] sm:items-start sm:gap-8">
      <nav aria-label="Năm bước" className="mb-6 min-w-0 sm:sticky sm:top-8 sm:mb-0">
        <ol className="flex min-w-0 gap-1 overflow-x-auto sm:block sm:overflow-visible">
          {BUOC.map((b, i) => {
            const dang = i === step;
            const sai = badStep === b.ma;
            const xong = i < step && !sai;
            return (
              <li key={b.ma} className="shrink-0 sm:shrink">
                <button
                  type="button"
                  data-testid={`step-${b.ma}`}
                  aria-current={dang ? "step" : undefined}
                  onClick={() => setStep(i)}
                  className={cn(
                    "flex min-h-11 items-center gap-2 border-b-2 bg-transparent px-2 text-left text-sm leading-5 transition-colors duration-150 sm:grid sm:w-full sm:grid-cols-[1.25rem_minmax(0,1fr)] sm:border-b-0 sm:border-l-2 sm:px-3",
                    sai
                      ? "border-mark font-medium text-mark"
                      : dang
                        ? "border-ink font-medium text-ink"
                        : xong
                          ? "border-transparent text-ink"
                          : "border-transparent text-muted",
                  )}
                >
                  <span className="font-mono text-xs tabular text-muted">{soBuoc(b.ma)}</span>
                  <span className="sm:hidden">{tenBuocNgan(b.ma)}</span>
                  <span className="hidden sm:inline">{tenBuocTrang(b.ma)}</span>
                </button>
              </li>
            );
          })}
        </ol>
      </nav>

      <section data-testid="solve-screen" className="sach-toan min-w-0 pb-[calc(5.5rem+env(safe-area-inset-bottom))] md:pb-0">
        <p className="sr-only">{title}</p>
        <h1 className="text-pretty text-xl font-semibold tracking-tight">
          <span className="font-mono text-sm font-normal tabular text-muted">{soBuoc(ma)}</span>
          <span className="mx-2 font-normal text-line">/</span>
          {ten}
        </h1>
        {loi ? <p className="mt-2 max-w-[42ch] text-sm leading-relaxed text-muted">{loi}</p> : null}
        <p className="cong-thuc mt-8 max-w-[65ch] overflow-x-auto text-[1.5rem] leading-tight sm:text-[2.25rem]" translate="no">
          <Tex tex={latex} block />
        </p>

        <div className="mt-8 space-y-4">
          {ma === "B.DH.TXD" && (
            <div className={lineBad("B.DH.TXD", 0) ? "cell-bad p-2" : ""}>
              <MathInput testId="latex-txd" label="D" nhe value={txd} onChange={setTxd} />
            </div>
          )}
          {ma === "B.DH.DAOHAM" && (
            <div className="space-y-4">
              {dh.map((line, i) => (
                <div key={i} className={lineBad("B.DH.DAOHAM", i) ? "cell-bad p-2" : ""}>
                  <MathInput
                    testId={i === 0 ? "latex-dh" : `latex-dh-${i}`}
                    label={i === 0 ? "y′" : `Dòng ${i + 1}`}
                    nhe={i === 0}
                    value={line}
                    onChange={(v) => setDh(dh.map((x, j) => (j === i ? v : x)))}
                  />
                </div>
              ))}
              <button type="button" className={cn(buttonClasses({ variant: "ghost", size: "sm" }), "px-0")} onClick={() => setDh([...dh, ""])}>
                Thêm dòng
              </button>
            </div>
          )}
          {ma === "B.DH.NGHIEM" && (
            <div className={`space-y-4 ${badStep === "B.DH.NGHIEM" ? "cell-bad rounded-button p-2" : ""}`} data-testid="nghiem-block">
              {roots.map((r, i) => (
                <div key={i} className="flex flex-col gap-2 sm:flex-row sm:items-end">
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
                    className={buttonClasses({ variant: "secondary", size: "sm" })}
                    onClick={() =>
                      setRoots(roots.map((x, j) => (j === i ? { ...x, loai: x.loai === "NGHIEM" ? "KHONG_XD" : "NGHIEM" } : x)))
                    }
                    aria-label={r.loai === "NGHIEM" ? "Đổi thành điểm không xác định" : "Đổi thành nghiệm"}
                  >
                    {r.loai === "NGHIEM" ? "Không xác định" : "Nghiệm"}
                  </button>
                </div>
              ))}
              <button type="button" data-testid="them-nghiem" className={cn(buttonClasses({ variant: "ghost", size: "sm" }), "px-0")} onClick={() => setRoots([...roots, { latex: "", loai: "NGHIEM" }])}>
                Thêm dòng nghiệm
              </button>
            </div>
          )}
          {ma === "B.DH.XETDAU" && (
            <div data-testid="bang-xet-dau">
              <div className="flex gap-2">
                <input
                  data-testid="moc-nhap"
                  value={draftPoint}
                  onChange={(e) => setDraftPoint(e.target.value)}
                  className={`w-28 ${fieldControl}`}
                  placeholder="mốc x…"
                  aria-label="Mốc x"
                />
                <Button
                  type="button"
                  data-testid="moc-them"
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
                </Button>
              </div>
              <div className="mt-4 overflow-x-auto">
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
                              <button
                                type="button"
                                className="ml-1 inline-flex min-h-8 min-w-8 items-center justify-center text-xs text-danger hover:underline [@media(pointer:coarse)]:min-h-11 [@media(pointer:coarse)]:min-w-11"
                                onClick={() => setPoints(points.filter((x) => x !== p))}
                                aria-label={`Xóa mốc ${p}`}
                              >
                                ×
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
                            <div className="flex justify-center gap-1">
                              {(pointCell ? ["0", "||"] : ["+", "−"]).map((opt) => (
                                <button
                                  key={opt}
                                  type="button"
                                  data-testid={`dau-${k}-${opt === "−" ? "-" : opt}`}
                                  className={`inline-flex min-h-8 min-w-8 items-center justify-center rounded text-xs [@media(pointer:coarse)]:min-h-11 [@media(pointer:coarse)]:min-w-11 ${cur === (opt === "−" ? "-" : opt) ? "bg-ink text-chalk" : "bg-wash"}`}
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
                      <th className="p-1 text-left">y</th>
                      {Array.from({ length: sorted.length * 2 + 1 }, (_, k) =>
                        k % 2 === 1 ? (
                          <td key={k} />
                        ) : (
                          <td key={k} className={`p-1 ${cellBad("BIEN_THIEN", k) ? "cell-bad" : ""}`}>
                            <div className="flex justify-center gap-1">
                              {[
                                ["TANG", "↗"],
                                ["GIAM", "↘"],
                              ].map(([val, icon]) => (
                                <button
                                  key={val}
                                  type="button"
                                  data-testid={`mui-${k}-${val}`}
                                  className={`inline-flex min-h-8 min-w-8 items-center justify-center rounded text-xs [@media(pointer:coarse)]:min-h-11 [@media(pointer:coarse)]:min-w-11 ${arrows[k] === val ? "bg-ink text-chalk" : "bg-wash"}`}
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
            <div className={`space-y-4 ${badStep === "B.DH.KETLUAN" ? "cell-bad rounded-button p-2" : ""}`}>
              {(
                [
                  ["db", "latex-db", "Đồng biến"],
                  ["nb", "latex-nb", "Nghịch biến"],
                  ["cd", "latex-cd", "Cực đại"],
                  ["ct", "latex-ct", "Cực tiểu"],
                ] as const
              ).map(([key, id, label]) => (
                <label key={key} className="block text-sm">
                  <span className="mb-2 block font-medium">{label}</span>
                  <input
                    data-testid={id}
                    value={kl[key]}
                    onChange={(e) => setKl({ ...kl, [key]: e.target.value })}
                    className={fieldControl}
                    placeholder={
                      key === "db" || key === "nb" ? "(…; …) và (…; …)" : "x = …, y = …"
                    }
                    title={
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
            key={`${grade.ket_qua}-${grade.thong_bao}`}
            data-testid="cham-thong-bao"
            aria-live="polite"
            className={cn(
              "mt-4 border-l-2 pl-3 text-sm leading-relaxed motion-safe:animate-[phieu-vao_180ms_ease-out]",
              dat ? "border-pass" : "border-mark",
            )}
          >
            {grade.finished ? "Đã xong bài này." : grade.thong_bao}
          </p>
        ) : null}

        {grade?.finished && moLoiGiai && loiGiai ? (
          <div data-testid="loi-giai-sau-nop" className="mt-4 border-l-2 border-line pl-3 text-sm leading-relaxed">
            <p className="font-medium">Lời giải</p>
            <p className="mt-1">{loiGiai}</p>
          </div>
        ) : null}

        <div
          data-testid="thanh-nop"
          className="mt-6 flex items-center gap-3 max-md:fixed max-md:inset-x-0 max-md:bottom-0 max-md:z-20 max-md:mt-0 max-md:border-t max-md:border-line max-md:bg-canvas max-md:px-4 max-md:pt-3 max-md:pb-[max(0.75rem,env(safe-area-inset-bottom))]"
        >
          <Button
            type="button"
            data-testid="nop-buoc"
            disabled={busy}
            onClick={submit}
            className="max-md:min-w-0 max-md:flex-1"
          >
            {busy ? "Đang kiểm…" : grade ? "Kiểm tra lại" : "Kiểm tra"}
          </Button>
          {!openTutor ? (
            <Button type="button" data-testid="mo-gia-su" variant="ghost" className="shrink-0" onClick={() => setOpenTutor(true)}>
              Cần gợi ý?
            </Button>
          ) : null}
        </div>
      </section>

    </div>
    <TutorPanel
      problemId={problemId}
      initialChat={initialChat}
      ai={ai}
      open={openTutor}
      onClose={() => setOpenTutor(false)}
    />
    </>
  );
}
