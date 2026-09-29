"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
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
import type { TienTrinh } from "@/lib/tien-trinh";
import { cn } from "@/lib/cn";

type BuocSai = { ma_buoc: string; dong: number | null; o: { hang: string; k: number | null } | null };
type VanDe = {
  id: string;
  loai_ket_qua: string;
  buoc_sai: BuocSai;
  nguyen_nhan?: string;
  /** ERR.DH.24: dòng nghiệm (0-based) chứa mốc thừa — tô dòng đó, không phải vấn đề thứ hai. */
  dong_lien_quan?: number | null;
};
type Grade = {
  ket_qua: string;
  loai_ket_qua: string;
  buoc_sai: BuocSai | null;
  cac_van_de?: VanDe[];
  tiep_theo?: { id: string; code: string; lyDo: string } | null;
  loi_giai?: string | null;
  thong_bao: string;
  per_buoc?: Record<string, string>;
  finished?: boolean;
  sub_id?: string;
};

const TEN_LOI: Record<string, string> = {
  SAI_TXD: "Tập xác định",
  DIEM_THIEU: "Thiếu điểm cần đặt mốc",
  DIEM_THUA: "Có mốc không cần đặt",
  SAI_THU_TU_MOC: "Mốc chưa theo thứ tự tăng dần",
  SAI_DAU: "Dấu của y′ trong một khoảng",
  SAI_GIA_TRI: "Một giá trị cần xem lại",
  SAI_BIEN_DOI: "Một dòng biến đổi hoặc mũi tên",
  DAU_DOI_TRONG_KHOANG: "y′ đổi dấu bên trong một khoảng",
  SAI_KET_LUAN: "Kết luận",
};

/** Đề có hỏi cực trị không (SP-03: chỉ hiện ô cực trị khi đề hỏi). */
function deHoiCucTri(de: string) {
  return /cực trị|cực đại|cực tiểu/i.test(de || "");
}

type Ev = StepPayload["events"][number];

const ORDER = BUOC.map((b) => b.ma);

/** Cột nhãn x / y′ / y dính trái khi cuộn bảng ngang (UXT-10-e). */
const NHAN_BANG = "sticky left-0 z-10 bg-canvas p-1 pr-2 text-left";

/** Tên đọc màn hình của giá trị dấu (UXT-10-f). */
const TEN_DAU: Record<string, string> = { "+": "cộng (+)", "-": "trừ (−)", "0": "bằng 0", "||": "không xác định (||)" };

/** Mô tả vị trí ô k của hàng dấu: k chẵn là khoảng giữa hai mốc, k lẻ là tại mốc. */
function moTaViTri(moc: string[], k: number) {
  if (k % 2 === 1) return `tại x = ${moc[(k - 1) / 2] ?? ""}`;
  const j = k / 2;
  const trai = j === 0 ? "−∞" : moc[j - 1];
  const phai = j === moc.length ? "+∞" : moc[j];
  return `trên khoảng (${trai}; ${phai})`;
}

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
  buocBatDau = null,
  tienTrinh = null,
  capGoiY,
  deXuatGuiGv,
  kyNang = null,
  daNop,
}: {
  problemId: string;
  title: string;
  latex: string;
  moLoiGiai?: boolean;
  loiGiai?: string | null;
  initialChat?: { role: "hs" | "gia_su"; text: string; trichDan?: TrichDanHien[] }[];
  ai?: AiPublicConfig;
  /** Bài khung ngắn: bước đầu tiên HS làm; các bước trước là dữ kiện đề cho (UXT-07-l). */
  buocBatDau?: string | null;
  /** UX-06: tiến trình dựng lại từ lượt nộp gần nhất (tải lại / vào lại mở đúng bước đang dở). */
  tienTrinh?: TienTrinh | null;
  /** UX-07: cấp gợi ý đã mở theo bước (phiên gia sư trên máy chủ). */
  capGoiY?: Record<string, number>;
  /** UXT-07-k: bước đã đủ điều kiện «Gửi thầy cô» (đọc từ phiên gia sư lúc mở trang). */
  deXuatGuiGv?: string[];
  /** REQUIRED-TESTIDS (UXT-07-l): mã kỹ năng của bài và HS đang đăng nhập đã có lượt nộp bài này chưa. */
  kyNang?: string | null;
  daNop?: boolean;
}) {
  const batDau = Math.max(0, buocBatDau ? ORDER.indexOf(buocBatDau as (typeof ORDER)[number]) : 0);
  const tt = tienTrinh;
  const [step, setStep] = useState(tt ? Math.max(batDau, tt.step) : batDau);
  const [txd, setTxd] = useState(tt?.txd ?? "");
  const [dh, setDh] = useState<string[]>(tt?.dh?.length ? tt.dh : [""]);
  const [roots, setRoots] = useState<{ latex: string; loai: "NGHIEM" | "KHONG_XD" }[]>(
    tt?.roots?.length ? tt.roots : [{ latex: "", loai: "NGHIEM" }],
  );
  const [points, setPoints] = useState<string[]>(tt?.points ?? []);
  const [draftPoint, setDraftPoint] = useState("");
  const [signs, setSigns] = useState<Record<number, string>>(tt?.signs ?? {});
  const [arrows, setArrows] = useState<Record<number, string>>(tt?.arrows ?? {});
  const [kl, setKl] = useState(tt?.kl ?? { db: "", nb: "", cd: "", ct: "" });
  const [events, setEvents] = useState<Ev[]>([]);
  const [grade, setGrade] = useState<Grade | null>((tt?.grade as Grade | null) ?? null);
  /** Bước của lượt nộp đang hiện kết quả (thông báo chấm không treo sang bước khác, UXT-04-f). */
  const [buocCham, setBuocCham] = useState<string | null>(
    tt?.grade ? (tt.grade.buoc_sai?.ma_buoc ?? ORDER[Math.max(batDau, tt.step)]) : null,
  );
  // Bước đã đạt theo lượt chấm gần nhất (dấu ✓ trên thanh bước, UXT-06-a/b).
  const [perBuoc, setPerBuoc] = useState<Record<string, string>>(tt?.perBuoc ?? {});
  const [xongBai, setXongBai] = useState(Boolean(tt?.finished));
  const [buocGuiGv, setBuocGuiGv] = useState<string[]>(deXuatGuiGv ?? []);
  // UXT-06-c: nháp chưa nộp (mốc, dấu, mũi tên, các ô) giữ qua tải lại — lưu cục bộ, gắn với lượt nộp gần nhất.
  const khoaNhap = `nhap:${problemId}`;
  const subRef = useRef<string | null>(tt?.subId ?? null);
  const daNap = useRef(false);
  useEffect(() => {
    try {
      const raw = localStorage.getItem(khoaNhap);
      if (raw) {
        const d = JSON.parse(raw) as {
          subId: string | null;
          txd?: string;
          dh?: string[];
          roots?: { latex: string; loai: "NGHIEM" | "KHONG_XD" }[];
          points?: string[];
          signs?: Record<number, string>;
          arrows?: Record<number, string>;
          kl?: { db: string; nb: string; cd: string; ct: string };
        };
        if (d && d.subId === subRef.current) {
          if (typeof d.txd === "string") setTxd(d.txd);
          if (Array.isArray(d.dh) && d.dh.length) setDh(d.dh);
          if (Array.isArray(d.roots) && d.roots.length) setRoots(d.roots);
          if (Array.isArray(d.points)) setPoints(d.points);
          if (d.signs) setSigns(d.signs);
          if (d.arrows) setArrows(d.arrows);
          if (d.kl) setKl(d.kl);
        } else localStorage.removeItem(khoaNhap);
      }
    } catch {
      /* localStorage có thể bị chặn */
    }
    daNap.current = true;
    // chỉ đọc một lần khi mở bài
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  useEffect(() => {
    if (!daNap.current) return;
    try {
      localStorage.setItem(khoaNhap, JSON.stringify({ subId: subRef.current, txd, dh, roots, points, signs, arrows, kl }));
    } catch {
      /* bỏ */
    }
  }, [khoaNhap, txd, dh, roots, points, signs, arrows, kl]);
  const [busy, setBusy] = useState(false);
  const [openTutor, setOpenTutor] = useState(false);
  const [moHet, setMoHet] = useState(false);
  const hoiCucTri = deHoiCucTri(title);
  // Chốt 29/09: app KHÔNG tự sắp mốc. Mốc giữ đúng thứ tự học sinh nhập; sai thứ tự báo SAI_THU_TU_MOC khi nộp.
  const sorted = points;

  function log(ev: Ev) {
    setEvents((xs) => [...xs, ev]);
  }

  function payload(nopToi: string): StepPayload {
    const until = ORDER.indexOf(nopToi as (typeof ORDER)[number]);
    const cac_buoc: StepPayload["cac_buoc"] = [];
    if (until >= 0 && batDau <= 0) cac_buoc.push({ ma_buoc: "B.DH.TXD", cac_dong: [{ dong: 0, latex: txd }] });
    if (until >= 1 && batDau <= 1) {
      const lines = dh.map((latex, i) => ({ dong: i, latex })).filter((l) => l.latex.trim());
      cac_buoc.push({ ma_buoc: "B.DH.DAOHAM", cac_dong: lines.length ? lines : [{ dong: 0, latex: "" }] });
    }
    if (until >= 2 && batDau <= 2) {
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
      // SP-03: mỗi ô gửi kèm nhãn `loai`; bộ chấm hiểu nội dung theo nhãn. Ô trống hoặc "không có" = không có.
      const lines: { dong: number; latex: string; loai: string }[] = [];
      const khai: string[] = [];
      const push = (key: string, loai: string, text: string) => {
        khai.push(key);
        lines.push({ dong: lines.length, latex: text.trim(), loai });
      };
      push("dong_bien", "DONG_BIEN", kl.db);
      push("nghich_bien", "NGHICH_BIEN", kl.nb);
      if (hoiCucTri) {
        // UXT-02-b: để trống ô cực trị = "không có" (gửi rõ chữ để bộ chấm và giáo viên đọc được).
        push("cuc_dai", "CUC_DAI", kl.cd.trim() ? kl.cd : "không có cực đại");
        push("cuc_tieu", "CUC_TIEU", kl.ct.trim() ? kl.ct : "không có cực tiểu");
      }
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
    if (res.de_xuat_gui_gv && res.buoc_de_xuat) {
      const b = res.buoc_de_xuat;
      setBuocGuiGv((cu) => (cu.includes(b) ? cu : [...cu, b]));
    }
    setBuocCham(res.buoc_sai?.ma_buoc ?? ma);
    if (res.per_buoc) setPerBuoc(res.per_buoc);
    if (res.finished) setXongBai(true);
    if (res.sub_id) subRef.current = res.sub_id;
    setMoHet(false);
    if (res.ket_qua === "DAT" && !res.finished && step < ORDER.length - 1) setStep(step + 1);
  }

  const badStep = grade?.ket_qua === "SAI" ? grade.buoc_sai?.ma_buoc : grade?.ket_qua === "KHONG_KIEM_DUOC" ? grade.buoc_sai?.ma_buoc : null;

  // Màn học sinh: chỉ mở vấn đề GỐC đầu tiên theo thứ tự bước (kèm các ô hệ quả của nó);
  // các gốc còn lại gộp thành "còn N chỗ cần xem lại", bấm để mở. Giáo viên và mô hình thành thạo nhận đủ danh sách.
  const vanDe = grade?.ket_qua === "SAI" ? grade.cac_van_de || [] : [];
  const goc = vanDe.filter((v) => !v.nguyen_nhan);
  const gocHien = moHet ? goc : goc.slice(0, 1);
  const idHien = new Set(gocHien.map((v) => v.id));
  const hien = vanDe.filter((v) => idHien.has(v.id) || (v.nguyen_nhan && idHien.has(v.nguyen_nhan)));
  const conLai = goc.length - gocHien.length;
  const coThuTu = hien.some((v) => v.loai_ket_qua === "SAI_THU_TU_MOC");
  const coThua = hien.some((v) => v.loai_ket_qua === "DIEM_THUA" && v.buoc_sai.o?.hang === "X");
  const loaiTheoId = new Map(vanDe.map((v) => [v.id, v.loai_ket_qua]));

  function lineBad(ma: string, dong: number) {
    if (vanDe.length)
      return hien.some(
        (v) =>
          !v.nguyen_nhan &&
          v.buoc_sai.ma_buoc === ma &&
          ((!v.buoc_sai.o && v.buoc_sai.dong === dong) || v.dong_lien_quan === dong),
      );
    return badStep === ma && grade?.buoc_sai?.dong === dong;
  }
  /** Có vấn đề chỉ đúng dòng/ô trong bước này (thì tô dòng đó, không tô cả khối). */
  function coDongLoi(ma: string) {
    return hien.some(
      (v) => !v.nguyen_nhan && v.buoc_sai.ma_buoc === ma && ((v.buoc_sai.dong != null && !v.buoc_sai.o) || v.dong_lien_quan != null),
    );
  }
  /** "bad" = ô lỗi (đỏ); "he_qua" = ô chỉ sai do lỗi gốc (viền nét đứt trung tính, không tính lỗi riêng). */
  function oTrangThai(hang: string, k: number): { kieu: "bad" | "he_qua" | null; nhan?: string } {
    if (!vanDe.length) {
      const o = grade?.buoc_sai?.o;
      return { kieu: badStep === "B.DH.XETDAU" && o?.hang === hang && o?.k === k ? "bad" : null };
    }
    const v = hien.find((x) => x.buoc_sai.o?.hang === hang && x.buoc_sai.o?.k === k);
    if (!v) return { kieu: null };
    if (v.nguyen_nhan) {
      const goc = loaiTheoId.get(v.nguyen_nhan);
      return { kieu: "he_qua", nhan: goc === "SAI_THU_TU_MOC" ? "sắp lại mốc trước" : "dấu đổi trong khoảng" };
    }
    // Mốc thừa: chỉ ô X đỏ; ô khoảng hai bên chỉ viền nét đứt nếu dấu thật sự sai
    if (coThua && hang !== "X") return { kieu: "he_qua", nhan: "dấu cần xem lại" };
    return { kieu: "bad" };
  }
  function cellBad(hang: string, k: number) {
    return oTrangThai(hang, k).kieu === "bad";
  }
  function oClass(hang: string, k: number) {
    const t = oTrangThai(hang, k).kieu;
    return t === "bad" ? "cell-bad" : t === "he_qua" ? "cell-he-qua" : "";
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
            const deCho = i < batDau;
            const xong = !deCho && !sai && (xongBai || perBuoc[b.ma] === "DAT" || i < step);
            const trangThai = deCho ? "de-cho" : sai ? "sai" : xong ? "dat" : dang ? "dang-lam" : "chua-lam";
            return (
              <li key={b.ma} className="shrink-0 sm:shrink">
                <button
                  type="button"
                  data-testid={`step-${b.ma}`}
                  data-de-cho={deCho ? "1" : undefined}
                  data-trang-thai={trangThai}
                  aria-label={`${soBuoc(b.ma)} ${tenBuocTrang(b.ma)}${xong ? " — đã đạt" : sai ? " — cần sửa" : ""}`}
                  aria-current={dang ? "step" : undefined}
                  aria-disabled={deCho || undefined}
                  title={deCho ? "Đề đã cho sẵn bước này" : undefined}
                  onClick={() => {
                    if (!deCho) setStep(i);
                  }}
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
                  <span className="sm:hidden">
                    {tenBuocNgan(b.ma)}
                    {xong ? <span aria-hidden="true" className="ml-1 text-pass">✓</span> : null}
                  </span>
                  <span className="hidden sm:inline">
                    {tenBuocTrang(b.ma)}
                    {xong ? <span aria-hidden="true" className="ml-1 text-pass">✓</span> : null}
                  </span>
                  {deCho ? <span className="text-xs text-muted">đề cho</span> : null}

                </button>
              </li>
            );
          })}
        </ol>
      </nav>

      <section data-testid="solve-screen" data-ky-nang={kyNang || undefined} data-da-nop={daNop === undefined ? undefined : String(daNop)} data-buoc-bat-dau={buocBatDau || ORDER[0]} className="sach-toan min-w-0 pb-[calc(5.5rem+env(safe-area-inset-bottom))] md:pb-0">
        <p className="sr-only">{title}</p>
        <h1 className="text-pretty text-xl font-semibold tracking-tight">
          <span className="font-mono text-sm font-normal tabular text-muted">{soBuoc(ma)}</span>
          <span className="mx-2 font-normal text-line">/</span>
          {ten}
        </h1>
        {loi ? <p className="mt-2 max-w-[42ch] text-sm leading-relaxed text-muted">{loi}</p> : null}
        {batDau > 0 ? (
          <p data-testid="de-cho-san" className="mt-2 max-w-[60ch] text-sm leading-relaxed text-muted">
            Đề đã cho sẵn: {BUOC.slice(0, batDau).map((b) => tenBuocTrang(b.ma).toLowerCase()).join(", ")} (xem đề bên dưới). Em bắt đầu từ bước{" "}
            {tenBuocTrang(ORDER[batDau]).toLowerCase()}.
          </p>
        ) : null}
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
            <div
              className={`space-y-4 ${badStep === "B.DH.NGHIEM" && !coDongLoi("B.DH.NGHIEM") ? "cell-bad rounded-button p-2" : ""}`}
              data-testid="nghiem-block"
            >
              {roots.map((r, i) => (
                <div
                  key={i}
                  data-testid={`dong-nghiem-${i}`}
                  className={cn("flex flex-col gap-2 sm:flex-row sm:items-end", lineBad("B.DH.NGHIEM", i) && "cell-bad p-2")}
                >
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
                  className="shrink-0 whitespace-nowrap"
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
                    <tr data-testid="hang-x" className={coThuTu ? "cell-bad" : ""}>
                      <th scope="row" className={NHAN_BANG}>x</th>
                      {Array.from({ length: sorted.length * 2 + 1 }, (_, k) => {
                        if (k % 2 === 1) {
                          const idx = (k - 1) / 2;
                          const p = sorted[idx];
                          return (
                            <td key={k} data-testid={`x-${idx}`} className={`p-1 font-semibold ${cellBad("X", idx) ? "cell-bad" : ""}`}>
                              {p}
                              <button
                                type="button"
                                className="ml-1 inline-flex min-h-8 min-w-8 items-center justify-center text-xs text-danger hover:underline [@media(pointer:coarse)]:min-h-11 [@media(pointer:coarse)]:min-w-11"
                                onClick={() => {
                                  setPoints(points.filter((_, j) => j !== idx));
                                  setSigns({});
                                  setArrows({});
                                }}
                                aria-label={`Xóa mốc ${p}`}
                              >
                                ×
                              </button>
                            </td>
                          );
                        }
                        // UXT-04-e: bảng chưa có mốc vẫn hiện đủ −∞ và +∞ (hai ô, các hàng dưới gộp cột).
                        if (sorted.length === 0)
                          return [
                            <td key="am">−∞</td>,
                            <td key="duong">+∞</td>,
                          ];
                        if (k === 0) return <td key={k}>−∞</td>;
                        if (k === sorted.length * 2) return <td key={k}>+∞</td>;
                        return <td key={k} />;
                      })}
                    </tr>
                    <tr>
                      <th scope="row" className={NHAN_BANG}>y′</th>
                      {Array.from({ length: sorted.length * 2 + 1 }, (_, k) => {
                        const pointCell = k % 2 === 1;
                        const cur = signs[k];
                        const viTri = moTaViTri(sorted, k);
                        return (
                          <td
                            key={k}
                            colSpan={sorted.length === 0 ? 2 : undefined}
                            data-testid={`o-dau-${k}`}
                            role="group"
                            aria-label={`Dấu y′ ${viTri}${cur ? `: ${TEN_DAU[cur] || cur}` : ""}`}
                            className={`p-1 ${oClass("DAU_YPHAY", k)}`}
                          >
                            {oTrangThai("DAU_YPHAY", k).nhan ? (
                              <span className="mb-1 block text-[0.65rem] leading-tight text-muted">{oTrangThai("DAU_YPHAY", k).nhan}</span>
                            ) : null}
                            <div className="mx-auto grid w-max grid-cols-2 justify-center gap-1">
                              {/* UXT-10-b: mọi ô chọn được đủ 4 giá trị +, −, 0, || (ô khoảng hiện +/− trước, ô mốc hiện 0/|| trước). */}
                              {(pointCell ? ["0", "||", "+", "−"] : ["+", "−", "0", "||"]).map((opt) => (
                                <button
                                  key={opt}
                                  type="button"
                                  aria-label={TEN_DAU[opt === "−" ? "-" : opt]}
                                  aria-pressed={cur === (opt === "−" ? "-" : opt)}
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
                      <th scope="row" className={NHAN_BANG}>y</th>
                      {Array.from({ length: sorted.length * 2 + 1 }, (_, k) =>
                        k % 2 === 1 ? (
                          <td key={k} />
                        ) : (
                          <td
                            key={k}
                            colSpan={sorted.length === 0 ? 2 : undefined}
                            data-testid={`o-mui-${k}`}
                            role="group"
                            aria-label={`Chiều biến thiên ${moTaViTri(sorted, k)}`}
                            className={`p-1 ${oClass("BIEN_THIEN", k)}`}
                          >
                            <div className="flex justify-center gap-1">
                              {[
                                ["TANG", "↗"],
                                ["GIAM", "↘"],
                              ].map(([val, icon]) => (
                                <button
                                  key={val}
                                  type="button"
                                  aria-label={val === "TANG" ? "tăng (đồng biến)" : "giảm (nghịch biến)"}
                                  aria-pressed={arrows[k] === val}
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
            <div
              className={`space-y-4 ${badStep === "B.DH.KETLUAN" && !coDongLoi("B.DH.KETLUAN") ? "cell-bad rounded-button p-2" : ""}`}
            >
              {(
                [
                  ["db", "latex-db", "Đồng biến"],
                  ["nb", "latex-nb", "Nghịch biến"],
                  ["cd", "latex-cd", "Cực đại"],
                  ["ct", "latex-ct", "Cực tiểu"],
                ] as const
              )
                .filter(([key]) => hoiCucTri || key === "db" || key === "nb")
                .map(([key, id, label], i) =>
                  key === "cd" || key === "ct" ? (
                    // CT-SGK (Build 13:25 mục 3): ô cực trị nhập được qua MathLive (x_{CT} = 3) VÀ ô gõ thường.
                    <div key={key} data-testid={`o-kl-${key}`} className={cn(lineBad("B.DH.KETLUAN", i) && "cell-bad p-2")}>
                      <MathInput
                        testId={id}
                        label={label}
                        value={kl[key]}
                        onChange={(v) => setKl((cu) => ({ ...cu, [key]: v }))}
                        placeholder="x = …, y = …, hoặc: không có"
                        title={key === "cd" ? "cực đại tại x = ..., y = ..." : "cực tiểu tại x = ..., y = ..."}
                        xemTruoc={false}
                      />
                    </div>
                  ) : (
                <label key={key} data-testid={`o-kl-${key}`} className={cn("block text-sm", lineBad("B.DH.KETLUAN", i) && "cell-bad p-2")}>
                  <span className="mb-2 block font-medium">{label}</span>
                  <input
                    data-testid={id}
                    value={kl[key]}
                    onChange={(e) => setKl({ ...kl, [key]: e.target.value })}
                    className={fieldControl}
                    placeholder={
                      key === "db" || key === "nb" ? "(…; …) và (…; …), hoặc: không có" : "x = …, y = …, hoặc: không có"
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
                  ),
                )}
            </div>
          )}
        </div>

        {grade && (grade.finished || dat || !buocCham || buocCham === ma) ? (
          <p
            key={`${grade.ket_qua}-${grade.thong_bao}`}
            data-testid="cham-thong-bao"
            aria-live="polite"
            className={cn(
              "mt-4 border-l-2 pl-3 text-sm leading-relaxed motion-safe:animate-[phieu-vao_180ms_ease-out]",
              dat ? "border-pass" : "border-mark",
            )}
          >
            {grade.finished
              ? "Đã xong bài này."
              : dat && buocCham && buocCham !== ma
                ? `Bước ${tenBuocTrang(buocCham)} đã nộp hợp lệ. Em làm tiếp bước này.`
                : grade.thong_bao}
          </p>
        ) : null}

        {conLai > 0 ? (
          <button
            type="button"
            data-testid="con-van-de"
            className="mt-2 text-sm text-muted underline underline-offset-2"
            onClick={() => setMoHet(true)}
          >
            Còn {conLai} chỗ cần xem lại
          </button>
        ) : null}
        {moHet && goc.length > 1 ? (
          <ul data-testid="ds-van-de" className="mt-2 list-disc pl-5 text-sm text-muted">
            {goc.map((v) => (
              <li key={v.id}>
                {TEN_LOI[v.loai_ket_qua] || v.loai_ket_qua} — bước {tenBuocTrang(v.buoc_sai.ma_buoc)}
              </li>
            ))}
          </ul>
        ) : null}

        {grade?.finished && grade.tiep_theo ? (
          <div data-testid="bai-tiep-theo" className="mt-4 border-l-2 border-pass pl-3 text-sm leading-relaxed">
            <Link href={`/hs/luyen/${grade.tiep_theo.id}`} className="font-medium underline underline-offset-2">
              Bài tiếp theo
            </Link>
            <p className="mt-1 text-muted">{grade.tiep_theo.lyDo}</p>
          </div>
        ) : null}

        {grade?.finished && moLoiGiai && (grade.loi_giai || loiGiai) ? (
          <div data-testid="loi-giai-sau-nop" className="mt-4 border-l-2 border-line pl-3 text-sm leading-relaxed">
            <p className="font-medium">Lời giải</p>
            <p className="mt-1">{grade.loi_giai || loiGiai}</p>
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
      maBuoc={ma}
      capBanDau={capGoiY}
      deXuatGuiGv={buocGuiGv}
    />
    </>
  );
}
