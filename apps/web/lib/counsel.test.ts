import assert from "node:assert/strict";
import { test } from "node:test";
import { tuVanHocTap } from "./counsel";

test("lịch học không lộ xác suất hay giọng nghiên cứu", () => {
  const trong = tuVanHocTap([]);
  assert.match(trong.loiKhuyen, /Chưa làm bài nào/);
  assert.doesNotMatch(trong.loiKhuyen, /ước lượng|thành thạo|phát hành|xác suất|ngưỡng|nấc|sơ đồ/);

  const yeu = tuVanHocTap([
    { skillCode: "T12.DH.03", mastery: 0.41, currentMucDo4: "NHAN_BIET", stuckCounter: 0 },
  ]);
  assert.match(yeu.loiKhuyen, /Yếu nhất: xét dấu/);
  assert.match(yeu.loiKhuyen, /Nhận biết/);
  assert.doesNotMatch(yeu.loiKhuyen, /0\.41|xác suất|T12\.DH|nấc|ngưỡng|sơ đồ/);

  const ket = tuVanHocTap([
    { skillCode: "T12.DH.02", mastery: 0.2, currentMucDo4: "THONG_HIEU", stuckCounter: 3 },
  ]);
  assert.match(ket.loiKhuyen, /tính đạo hàm/);
  assert.match(ket.loiKhuyen, /chưa tăng độ khó/);
  assert.doesNotMatch(ket.loiKhuyen, /nâng nấc|xác suất/);
});
