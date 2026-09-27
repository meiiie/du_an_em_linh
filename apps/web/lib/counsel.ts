import { LABEL4, type Muc4 } from "./levels";

export type MucState = {
  skillCode: string;
  mastery: number;
  currentMucDo4: string;
  stuckCounter: number;
};

const TEN_KY_NANG: Record<string, string> = {
  "T12.DH.01": "đọc dấu y′",
  "T12.DH.02": "tính đạo hàm và điểm tới hạn",
  "T12.DH.03": "xét dấu và kết luận đơn điệu",
  "T12.DH.04": "nhận biết cực trị trên bảng",
  "T12.DH.05": "tìm cực trị từ công thức",
};

/** Lịch học: tiếng lớp 12. Không xác suất BKT, không sơ đồ/nấc/ngưỡng. */
export function tuVanHocTap(states: MucState[]) {
  const weak = [...states].sort((a, b) => a.mastery - b.mastery || b.stuckCounter - a.stuckCounter)[0];
  if (!weak) {
    return {
      loiKhuyen: "Chưa làm bài nào. Mỗi buổi một bài, nộp đủ năm bước.",
      slots: [
        { thu: "Thứ Hai", gio: "19:00", viec: "Một bài nhận biết — viết đủ 5 bước" },
        { thu: "Thứ Tư", gio: "19:00", viec: "Ôn quy tắc đạo hàm trên giấy" },
        { thu: "Thứ Sáu", gio: "19:30", viec: "Làm lại bước từng bị tô" },
        { thu: "Chủ Nhật", gio: "09:00", viec: "Xem lại bảng xét dấu đã nộp" },
      ],
    };
  }
  const ten = TEN_KY_NANG[weak.skillCode] || weak.skillCode;
  const muc = LABEL4[weak.currentMucDo4 as Muc4] || weak.currentMucDo4;
  const ket = weak.stuckCounter >= 2;
  const loiKhuyen = ket
    ? `Yếu nhất: ${ten} (${muc}). Tuần này ôn đúng dạng này, chưa tăng độ khó.`
    : `Yếu nhất: ${ten} (${muc}). Mỗi buổi một bài.`;

  const slots = ket
    ? [
        { thu: "Thứ Hai", gio: "19:00", viec: `Ôn ${ten} — bài ngắn` },
        { thu: "Thứ Tư", gio: "19:00", viec: "Viết lại bước bị tô, không mở bài mới" },
        { thu: "Thứ Sáu", gio: "19:30", viec: "Hỏi gia sư gợi ý quy trình, rồi tự nộp" },
        { thu: "Chủ Nhật", gio: "09:00", viec: "Nếu hết kẹt: một bài khác cùng dạng" },
      ]
    : [
        { thu: "Thứ Hai", gio: "19:00", viec: "Ôn công thức đạo hàm / xét dấu" },
        { thu: "Thứ Tư", gio: "19:00", viec: "Một bài kỹ năng đang yếu" },
        { thu: "Thứ Sáu", gio: "19:30", viec: "Sửa dạng đã sai trong tuần" },
        { thu: "Chủ Nhật", gio: "09:00", viec: "Một bài khó hơn nếu đã vững" },
      ];

  return { loiKhuyen, slots, skillCode: weak.skillCode, muc };
}
