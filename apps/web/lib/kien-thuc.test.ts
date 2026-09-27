import assert from "node:assert/strict";
import { test } from "node:test";
import {
  chamDiemVanBan,
  chonKho,
  docTrichDanLuu,
  dongKhoChoPrompt,
  duongKhoTrichDan,
  nhanTrichDan,
  trichDoan,
  vanBanKhop,
  xemKhoTheoKhung,
} from "./kien-thuc";

test("trích đoạn quanh từ khóa", () => {
  const t = "aaa đạo hàm của tổng bbb";
  assert.match(trichDoan(t, "đạo hàm"), /đạo hàm/);
});

test("trích đoạn khớp không dấu", () => {
  assert.equal(vanBanKhop("Hàm dong bien khi y' >= 0", "đồng biến"), true);
  assert.match(trichDoan("Hàm dong bien khi y' >= 0 trên khoảng", "đồng biến"), /dong bien/i);
});

test("điểm văn bản đếm cụm khớp", () => {
  assert.equal(chamDiemVanBan("hàm đồng biến khi đạo hàm không âm", ["đồng biến", "đạo hàm", "log"]), 2);
});

test("kho bỏ tài liệu chưa rõ quyền và lấy công thức đơn điệu", () => {
  const kho = chonKho({
    maBuoc: "B.DH.KETLUAN",
    cauHoi: "Em sai chỗ nào ở cực trị",
    taiLieu: [
      {
        id: "a",
        title: "Ghi chú",
        licenseStatus: "tu_soan",
        version: 1,
        text: "Lập bảng xét dấu của đạo hàm. Đồng biến khi đạo hàm không âm.",
      },
      {
        id: "b",
        title: "Lậu",
        licenseStatus: "chua_ro",
        version: 1,
        text: "đồng biến nghịch biến cực trị đạo hàm xét dấu",
      },
    ],
    congThuc: [
      { id: "c1", title: "Cực trị", latex: "+ \\to -", noiDung: "Đạo hàm đổi từ dương sang âm thì cực đại." },
      { id: "c2", title: "Khác", latex: "a=b", noiDung: "Không liên quan." },
    ],
  });
  assert.equal(kho.taiLieu.some((d) => d.id === "b"), false);
  assert.equal(kho.taiLieu.some((d) => d.id === "a"), true);
  assert.equal(kho.congThuc.some((c) => c.ten === "Cực trị"), true);
  assert.match(dongKhoChoPrompt(kho), /Cực trị/);
  assert.match(dongKhoChoPrompt(kho), /\[1\]/);
  assert.equal(dongKhoChoPrompt(kho).includes("lời giải"), false);
  const dan = nhanTrichDan(kho);
  assert.equal(dan[0]?.id, "c1");
  assert.match(dan[0]?.trich || "", /cực đại/);
});

test("câu hỏi nâng công thức khớp, neo kho đúng loại", () => {
  const kho = chonKho({
    maBuoc: "B.DH.DAOHAM",
    cauHoi: "Nhắc nguyên lý đạo hàm lũy thừa",
    taiLieu: [],
    congThuc: [
      { id: "luy", title: "Đạo hàm lũy thừa", latex: "(x^n)'", noiDung: "Đạo hàm của x mũ n là n nhân x mũ n trừ 1." },
      { id: "tong", title: "Đạo hàm tổng", latex: "(u+v)'", noiDung: "Đạo hàm của tổng bằng tổng các đạo hàm." },
    ],
  });
  assert.equal(kho.congThuc[0]?.id, "luy");
  assert.equal(duongKhoTrichDan({ loai: "cong_thuc", id: "luy" }), "/hs/kho#ct-luy");
  assert.equal(duongKhoTrichDan({ loai: "tai_lieu", id: "a" }), "/hs/kho?muc=lieu#tl-a");
  assert.deepEqual(docTrichDanLuu([{ loai: "cong_thuc", id: "luy", ten: "Đạo hàm lũy thừa", trich: "x mũ n" }])[0]?.ten, "Đạo hàm lũy thừa");
  assert.deepEqual(docTrichDanLuu("hỏng"), []);
});

test("khung năm bước không đọc tài liệu chưa rõ quyền", () => {
  const khung = xemKhoTheoKhung({
    taiLieu: [
      {
        id: "a",
        title: "Ghi chú",
        licenseStatus: "tu_soan",
        version: 1,
        text: "Lập bảng xét dấu của đạo hàm. Đồng biến khi đạo hàm không âm. Tập xác định của đa thức là R.",
      },
      {
        id: "b",
        title: "Lậu",
        licenseStatus: "chua_ro",
        version: 1,
        text: "đồng biến nghịch biến cực trị đạo hàm xét dấu tập xác định",
      },
    ],
    congThuc: [{ id: "c1", title: "Cực trị", latex: "+ \\to -", noiDung: "Đạo hàm đổi từ dương sang âm thì cực đại." }],
  });
  assert.equal(khung.length, 5);
  assert.equal(
    khung.every((b) => !b.taiLieu.some((d) => d.id === "b")),
    true,
  );
  assert.equal(khung.some((b) => b.congThuc.some((c) => c.ten === "Cực trị")), true);
});
