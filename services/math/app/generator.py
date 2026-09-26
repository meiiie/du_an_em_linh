# -*- coding: utf-8 -*-
"""Sinh biến thể tham số hóa. Lời giải do SymPy tính, không lấy từ mô hình ngôn ngữ.

Họ hàm: bậc ba, trùng phương (bậc bốn chẵn), phân thức bậc nhất.
"""
import random

from app.grader import bai_lam_sang_payload, grade
from app.leakfilter import loc_ban_nhap
from app.machine import bai_lam_may, latex_ham, su_kien_bao_ve

GOI_Y = {
    "B.DH.TXD": [
        "Hàm này có mẫu số hoặc căn bậc hai không? Hãy viết điều kiện để biểu thức có nghĩa.",
        "Tập xác định là những x làm cho mẫu khác 0 và biểu thức dưới căn không âm. Ghi thành một dòng.",
        "Viết tập xác định bằng ký hiệu khoảng. Nếu có điểm bị loại, dùng dấu hợp các khoảng.",
    ],
    "B.DH.DAOHAM": [
        "Em tính đạo hàm từng hạng tử bằng quy tắc nào?",
        "Nhớ (x^n)' = n x^{n-1} và đạo hàm của tổng. Với thương, dùng (u/v)' = (u'v − uv')/v^2.",
        "Tính riêng đạo hàm từng hạng tử rồi cộng lại. Viết y' ở dòng đầu của bước này.",
    ],
    "B.DH.NGHIEM": [
        "Những điểm nào có thể làm đổi chiều biến thiên?",
        "Giải y' = 0 bằng cách đưa về tích các nhân tử. Đừng bỏ điểm làm cho y' không xác định nếu điểm đó thuộc tập xác định.",
        "Liệt kê nghiệm của y' = 0 và, nếu có, điểm thuộc tập xác định mà y' không tính được.",
    ],
    "B.DH.XETDAU": [
        "Trên mỗi khoảng, em lấy dấu của y' bằng cách nào?",
        "Nếu y' > 0 trên một khoảng thì hàm đồng biến trên khoảng đó; y' < 0 thì nghịch biến.",
        "Chọn một giá trị trong từng khoảng, thay vào y' để biết dấu, rồi chọn mũi tên tương ứng. Chưa cần viết kết luận.",
    ],
    "B.DH.KETLUAN": [
        "Từ dấu của y' em kết luận chiều biến thiên trên từng khoảng riêng, không gộp qua điểm bị loại.",
        "y' đổi dấu từ dương sang âm thì là cực đại; từ âm sang dương thì là cực tiểu. y' = 0 mà không đổi dấu thì chưa phải cực trị.",
        "Viết mỗi khoảng đơn điệu một dòng, rồi ghi điểm cực trị nếu có. Đừng chép đáp án từ chỗ khác — hãy đọc lại bảng của em.",
    ],
}


def _hints():
    out = []
    for ma, caps in GOI_Y.items():
        out.append({"ma_buoc": ma, "cac_cap": [{"cap": i + 1, "noi_dung": caps[i]} for i in range(3)]})
    return out


def _ham_bac_ba(rng):
    # p < q, p+q chẵn để a nguyên: y' = 3(x-p)(x-q)
    while True:
        p = rng.randint(-3, 2)
        q = rng.randint(p + 1, 4)
        if (p + q) % 2 == 0 and not (p == 0 and q == 0):
            break
    a = -3 * (p + q) // 2
    b = 3 * p * q
    c = rng.randint(-2, 3)
    ham = "x**3 + (%s)*x**2 + (%s)*x + (%s)" % (a, b, c)
    return ham, "VAN_DUNG", "APPLY", "VAN_DUNG"


def _ham_trung_phuong(rng):
    k = rng.choice([1, 2])
    a = -2 * k * k
    b = rng.randint(-2, 2)
    ham = "x**4 + (%s)*x**2 + (%s)" % (a, b)
    return ham, "VAN_DUNG_CAO", "ANALYZE", "VAN_DUNG"


def _ham_huu_ti(rng):
    n = rng.choice([-2, -1, 1, 2, 3])
    m = rng.choice([v for v in (-3, -1, 0, 1, 2, 4) if v != n])
    ham = "(x+(%s))/(x+(%s))" % (m, n)
    return ham, "VAN_DUNG_CAO", "ANALYZE", "VAN_DUNG"


_DANG = {"bac_ba": _ham_bac_ba, "trung_phuong": _ham_trung_phuong, "huu_ti": _ham_huu_ti}


def sinh(payload):
    dang = payload.get("dang") or "bac_ba"
    if dang not in _DANG:
        return {"loi": "dang khong ho tro"}
    rng = random.Random(int(payload.get("seed") or 1))
    ham, muc4, bloom, muc3 = _DANG[dang](rng)
    bl = bai_lam_may(ham)
    if not bl:
        return {"loi": "khong dung duoc loi giai may", "ham": ham}
    gp = bai_lam_sang_payload(bl, ham)
    g = grade(gp)
    if g["ket_qua"] != "DAT":
        return {"loi": "loi giai may khong qua tu cham", "ham": ham, "cham": g["ket_qua"], "thong_bao": g.get("thong_bao"), "buoc_sai": g.get("buoc_sai")}
    sk = su_kien_bao_ve(bl)
    hints = _hints()
    for block in hints:
        for cap in block["cac_cap"]:
            quyet = loc_ban_nhap(cap["noi_dung"], sk)
            cap["qua_loc"] = quyet["cho_phep"]
            if not quyet["cho_phep"]:
                cap["noi_dung"] = "Em đọc lại đề và làm nốt bước đang dở. Mình không đưa kết quả của bước này."
                cap["qua_loc"] = True
    lx = latex_ham(ham)
    return {
        "dang": dang,
        "ham": ham,
        "latex": lx,
        "de_bai": "Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số $y = %s$." % lx,
        "muc_do_4": muc4,
        "muc_do_bo_3": muc3,
        "muc_bloom": bloom,
        "bai_lam": bl,
        "payload_cham": gp,
        "thang_goi_y": hints,
        "su_kien": [{"loai": a, "gia_tri": b} for a, b in sk],
        "ky_nang_chinh": "T12.DH.05" if dang != "bac_ba" else "T12.DH.03",
    }
