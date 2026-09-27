import assert from "node:assert/strict";
import { test } from "node:test";
import { moTaTrichDan, moTaTrichDanDuyet } from "./citations";

test("trích dẫn hiện đoạn và tên, không dump cụm khớp", () => {
  const dong = moTaTrichDan([
    {
      trich: "nếu đạo hàm không âm trên khoảng đó thì hàm đồng biến",
      ten: "Ghi chú tự soạn: đơn điệu và cực trị",
      cum_tu: ["đồng biến", "đạo hàm", "xét dấu"],
      phien_ban: 1,
    },
    { formula_id: "ct-1", ten: "Cực trị", noi_dung: "Đạo hàm đổi từ dương sang âm thì cực đại.", cum_tu: ["cực đại"] },
    { formula_id: "ct-cu", cum_tu: ["đồng biến", "đạo hàm"] },
  ]);
  assert.deepEqual(dong, [
    "«nếu đạo hàm không âm trên khoảng đó thì hàm đồng biến» — Đơn điệu và cực trị",
    "Cực trị",
  ]);
  assert.doesNotMatch(dong.join(" "), /khớp|đồng biến, đạo hàm/);
});

test("duyệt: một căn cứ, im khi không kiểm được", () => {
  const raw = [
    {
      trich: "nếu đạo hàm không âm trên khoảng đó thì hàm đồng biến",
      ten: "Ghi chú tự soạn: đơn điệu và cực trị",
    },
    { formula_id: "ct-1", ten: "Cực trị" },
    { formula_id: "ct-2", ten: "Đơn điệu" },
  ];
  assert.deepEqual(moTaTrichDanDuyet(raw, "DAT"), [
    "«nếu đạo hàm không âm trên khoảng đó thì hàm đồng biến» — Đơn điệu và cực trị",
  ]);
  assert.deepEqual(moTaTrichDanDuyet(raw, "KHONG_KIEM_DUOC"), []);
  assert.deepEqual(moTaTrichDanDuyet([{ ten: "Cực trị" }], "SAI"), ["Cực trị"]);
});
