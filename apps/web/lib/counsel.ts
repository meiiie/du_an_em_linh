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

/** Tư vấn theo sơ đồ: phương pháp + lịch phù hợp mức hiện tại. Văn bản gốc, không chép SGK. */
export function tuVanHocTap(states: MucState[]) {
  const weak = [...states].sort((a, b) => a.mastery - b.mastery || b.stuckCounter - a.stuckCounter)[0];
  if (!weak) {
    return {
      loiKhuyen:
        "Chưa có ước lượng thành thạo. Mỗi buổi làm đúng một bài đã phát hành, nộp từng bước, không hỏi đáp án.",
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
    ? `Kỹ năng yếu nhất đang là ${ten} (mức ${muc}, xác suất ${weak.mastery.toFixed(2)}). Em đang kẹt — tuần này chỉ ôn đúng kỹ năng đó với bài cùng mức, chưa nâng nấc. Mỗi lần sai: viết lại quy tắc rồi nộp, đừng hỏi đáp án.`
    : `Kỹ năng yếu nhất: ${ten} (mức ${muc}, xác suất ${weak.mastery.toFixed(2)}). Mỗi buổi một việc: lấy lại công thức, làm một bài cùng mức, sửa bước bị tô. Khi đủ ngưỡng mới nâng một nấc — đó là vòng lặp tới vận dụng cao trên sơ đồ.`;

  const slots = ket
    ? [
        { thu: "Thứ Hai", gio: "19:00", viec: `Ôn ${ten} — bài ngắn cùng mức` },
        { thu: "Thứ Tư", gio: "19:00", viec: "Viết lại bước bị tô, không mở bài mới" },
        { thu: "Thứ Sáu", gio: "19:30", viec: "Hỏi gia sư gợi ý quy trình, rồi tự nộp" },
        { thu: "Chủ Nhật", gio: "09:00", viec: "Nếu hết kẹt: một bài cùng mức khác" },
      ]
    : [
        { thu: "Thứ Hai", gio: "19:00", viec: "Ôn công thức đạo hàm / xét dấu" },
        { thu: "Thứ Tư", gio: "19:00", viec: "Một bài cùng mức kỹ năng yếu" },
        { thu: "Thứ Sáu", gio: "19:30", viec: "Sửa dạng đã sai trong tuần" },
        { thu: "Chủ Nhật", gio: "09:00", viec: "Một bài nhích một nấc nếu đủ ngưỡng" },
      ];

  return { loiKhuyen, slots, skillCode: weak.skillCode, muc };
}
