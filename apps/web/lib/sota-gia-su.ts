/** Rubric SOTA 2026-09-27 — đo harness trích dẫn, không đo màu thương hiệu. */
import {
  chonKho,
  dongKhoChoPrompt,
  duongKhoTrichDan,
  ganNeoTrongLoi,
  locTrichDanTheoLoi,
  nhanTrichDan,
} from "./kien-thuc";
import { chonTrichHien, soTuChuThe, tenMoKho } from "./trich-dan-ui";

export type TieuChiSota = { id: string; ten: string; dat: boolean };

const LIEU = {
  id: "ghi",
  title: "Ghi chú: đơn điệu",
  licenseStatus: "tu_soan",
  version: 1,
  text: "Lập bảng xét dấu của đạo hàm. Đồng biến khi đạo hàm không âm. Đạo hàm lũy thừa hạ bậc.",
};

const CT = [
  { id: "luy", title: "Đạo hàm lũy thừa", latex: "(x^n)'", noiDung: "Đạo hàm của x mũ n là n nhân x mũ n trừ 1." },
  { id: "tong", title: "Đạo hàm tổng", latex: "(u+v)'", noiDung: "Đạo hàm của tổng bằng tổng các đạo hàm." },
  { id: "thuong", title: "Đạo hàm thương", latex: "(u/v)'", noiDung: "Thương: tử u'v trừ uv'." },
];

export function chamSotaGiaSu(): { diem: number; toiDa: number; tieuChi: TieuChiSota[] } {
  const kho = chonKho({
    maBuoc: "B.DH.DAOHAM",
    cauHoi: "Nhắc nguyên lý đạo hàm lũy thừa trong ghi chú lớp",
    taiLieu: [LIEU],
    congThuc: CT,
  });
  const dan = nhanTrichDan(kho);
  const prompt = dongKhoChoPrompt(kho);
  const dung = locTrichDanTheoLoi("Em nhớ «Đạo hàm lũy thừa» [1].", dan);
  const mo = locTrichDanTheoLoi("Chỉ hỏi quy trình.", dan);
  const neo = ganNeoTrongLoi("Nhớ [1] rồi $$[1]$$.", dan);
  const nho = chonKho({
    maBuoc: "B.DH.TXD",
    cauHoi: "Tập xác định đa thức",
    nhoId: ["on"],
    taiLieu: [
      { id: "on", title: "Ôn thế nào", licenseStatus: "tu_soan", version: 1, text: "Mỗi buổi tự viết lại quy tắc rồi làm một bài." },
      LIEU,
    ],
    congThuc: [],
  });
  const mot = chonTrichHien(dan, 2);

  const tieuChi: TieuChiSota[] = [
    { id: "cung-so", ten: "Prompt và mặt cùng số [n]", dat: prompt === dan.map((x) => `[${x.so}] ${x.loai === "tai_lieu" ? "Tài liệu" : "Công thức"} «${x.ten}»: ${x.trich}`).join("\n") },
    { id: "tran-3", ten: "Tối đa 3 nguồn trên mặt", dat: dan.length >= 1 && dan.length <= 3 },
    { id: "hon-lieu", ten: "Có chỗ cho tài liệu khi kho có", dat: dan.some((x) => x.loai === "tai_lieu") && dan.filter((x) => x.loai === "cong_thuc").length <= 2 },
    { id: "loc-n", ten: "Lọc đúng mục khi lời có [n]", dat: dung.length === 1 && dung[0]?.so === 1 && dung[0]?.dung === true },
    { id: "mo-khi-quen", ten: "Không bịa nguồn khi model quên [n]", dat: mo.length === dan.length && mo.every((x) => x.dung === false) },
    { id: "neo-ct", ten: "Neo công thức #ct-", dat: duongKhoTrichDan({ loai: "cong_thuc", id: "luy" }) === "/hs/kho#ct-luy" },
    { id: "neo-tl", ten: "Neo tài liệu #tl-", dat: duongKhoTrichDan({ loai: "tai_lieu", id: "ghi" }) === "/hs/kho?muc=lieu#tl-ghi" },
    { id: "khong-toan", ten: "[n] trong $$ không thành liên kết", dat: /\$\$\[1\]\$\$/.test(neo) && /\[1\]\(\/hs\/kho#ct-/.test(neo) },
    { id: "nho-id", ten: "Lượt trước còn tài liệu vừa mở", dat: nho.taiLieu.some((d) => d.id === "on") },
    { id: "badge-md", ten: "[n] thành liên kết số", dat: /\[1\]\(\/hs\/kho/.test(neo) && soTuChuThe("1") === 1 && soTuChuThe("Đạo hàm") === null },
    { id: "mot-doan", ten: "Một đoạn đang xem, không đổ tập", dat: mot?.so === 2 && chonTrichHien(dan, null)?.id === (dan.find((x) => x.dung) || dan[0])?.id },
    { id: "ten-mo", ten: "Mở công thức / Mở tài liệu — một việc", dat: tenMoKho("cong_thuc") === "Mở công thức" && tenMoKho("tai_lieu") === "Mở tài liệu" },
  ];
  const diem = tieuChi.filter((t) => t.dat).length;
  return { diem, toiDa: tieuChi.length, tieuChi };
}
