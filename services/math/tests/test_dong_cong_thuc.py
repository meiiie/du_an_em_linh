# -*- coding: utf-8 -*-
"""Job kiem_dong_cong_thuc (ADR 013, #83, T042b–T042c).

Bảng 6 dòng của v0 phải DAT cả 6 ở hai tầng với 5 tài liệu của lớp (3 của v0, sp-tai-lieu-0001, sp-tai-lieu-0002).
Ca âm và ca rà độc lập (math-verifier trên #101): định lí chỉ DAT khi khớp danh mục; câu máy không đọc trọn không bao
giờ DAT; quy tắc sai bị SAI kèm phản ví dụ đúng mệnh đề đã viết.
«Hết giờ → không DAT» do sandbox (test_sandbox.py) và client của core (#82) bảo đảm: kết quả hết giờ không có `dong`.
"""
import io
import json
import os
import re

import pytest

from app.dong_cong_thuc import kiem_dong_cong_thuc

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))

# apps/web/scripts/seed.ts@3bfc584, dòng 612–617: bảng công thức khóa của v0 (nguyên văn).
BANG_V0 = [
    ("d-1", "Đạo hàm lũy thừa", r"(x^n)' = n x^{n-1}", "Đạo hàm của x mũ n là n nhân x mũ n trừ 1. Hằng số có đạo hàm bằng 0."),
    ("d-2", "Đạo hàm tổng", r"(u+v)' = u' + v'", "Đạo hàm của tổng bằng tổng các đạo hàm."),
    ("d-3", "Đạo hàm thương", r"(u/v)' = (u'v - uv') / v^2", "Với thương, tử là u'v trừ uv', mẫu là v bình."),
    ("d-4", "Đơn điệu", r"y' \ge 0,\ y' = 0 \text{ chỉ tại hữu hạn điểm} \Rightarrow \text{đồng biến}",
     "Hàm đồng biến trên khoảng khi đạo hàm không âm và bằng 0 tại hữu hạn điểm; nghịch biến khi đạo hàm không dương theo cùng quy tắc."),
    ("d-5", "Cực trị", r"+ \to - : \text{cực đại}",
     "Đạo hàm đổi từ dương sang âm thì cực đại; từ âm sang dương thì cực tiểu. Đạo hàm bằng 0 mà không đổi dấu thì chưa phải cực trị."),
    ("d-6", "Điểm tới hạn", r"y'=0 \text{ hoặc } y' \text{ không xác định}",
     "Điểm tới hạn gồm nghiệm của đạo hàm bằng 0 và điểm thuộc tập xác định mà đạo hàm không xác định."),
]

# apps/web/scripts/seed.ts@3bfc584, dòng 551–599: ba tài liệu của v0 (nguyên văn).
TAI_LIEU_V0 = [
    {"id": "v0-don-dieu", "license_status": "tu_soan", "text": " ".join([
        "Ghi chú tự soạn cho lớp 12A1 thử, không chép sách.",
        "Với hàm số xác định trên một khoảng: nếu đạo hàm không âm trên khoảng đó và chỉ bằng 0 tại hữu hạn điểm thì hàm đồng biến;",
        "nếu đạo hàm không dương và chỉ bằng 0 tại hữu hạn điểm thì hàm nghịch biến.",
        "Lập bảng xét dấu của đạo hàm trên từng khoảng xác định bởi nghiệm y' = 0 và điểm y' không xác định.",
        "Nếu đạo hàm đổi từ dương sang âm khi đi qua một điểm trong thì đó là cực đại; từ âm sang dương là cực tiểu.",
        "Điểm cực trị phải nằm trong một khoảng mở chứa trong tập xác định.",
    ])},
    {"id": "v0-de-mau", "license_status": "tu_soan", "text":
        "Đề tự soạn, không chép sách. Dạng: tìm khoảng đồng biến, nghịch biến của y = ax^3 + bx^2 + cx + d với a khác 0. "
        "Học sinh đi đủ năm bước: tập xác định, đạo hàm, nghiệm y′, bảng xét dấu, kết luận. "
        "Yêu cầu cần đạt công khai (CT GDPT môn Toán 2018, TT 32/2018/TT-BGDĐT, lớp 12): nhận biết đơn điệu từ dấu y′; thể hiện trên bảng biến thiên. "
        "Không dùng làm lời giải chuẩn cho gia sư."},
    {"id": "v0-phuong-phap", "license_status": "tu_soan", "text":
        "Văn bản tự soạn. Mỗi buổi tự viết lại quy tắc đạo hàm rồi làm một bài, không mở đáp án trước. "
        "Gia sư chỉ gợi ý quy trình, không đưa kết quả. "
        "Khi kẹt cùng bước ba lần thì gửi thầy cô, chưa chuyển mức khó hơn."},
]


def _tai_lieu_sp(ma="sp-tai-lieu-0001", quyen=None):
    """Tài liệu của lab Sư phạm: tệp dữ liệu nếu #85 đã áp bản vá, không thì đọc thẳng từ bản vá (#84, #103)."""
    p = os.path.join(REPO, "data", "supham", "tai-lieu", ma + ".json")
    if os.path.exists(p):
        doc = json.load(io.open(p, encoding="utf-8"))
    else:
        patch = io.open(os.path.join(REPO, "labs", "pedagogy", "ban-va", ma + ".patch"), encoding="utf-8").read()
        doc = json.loads("\n".join(d[1:] for d in patch.splitlines() if d.startswith("+") and not d.startswith("+++")))
    return {"id": doc["ma"], "license_status": quyen or doc["licenseStatus"], "text": doc["textContent"]}


def _tai_lieu_lop():
    return TAI_LIEU_V0 + [_tai_lieu_sp("sp-tai-lieu-0001"), _tai_lieu_sp("sp-tai-lieu-0002")]


def _dong(*rows):
    return [{"id": i, "tieu_de": t, "latex": l, "phat_bieu": p} for i, t, l, p in rows]


def _chay(rows, tai_lieu):
    kq = kiem_dong_cong_thuc({"dong": _dong(*rows), "tai_lieu": tai_lieu})
    return {d["id"]: d for d in kq["dong"]}, kq


def _nguon(d):
    t2 = d["tang2"]
    return {t["tai_lieu"] for t in [t2["trich_dan"]] + t2.get("trich_dan_them", [])}


# ------------------------------------------------------------------ bảng v0
def test_bang_v0_dat_ca_6_o_hai_tang_voi_5_tai_lieu():
    kq, _ = _chay(BANG_V0, _tai_lieu_lop())
    for ma, loai in (("d-1", "DANG_THUC"), ("d-2", "DANG_THUC"), ("d-3", "DANG_THUC"),
                     ("d-4", "DINH_LI"), ("d-5", "DINH_LI"), ("d-6", "DINH_LI")):
        d = kq[ma]
        assert d["loai"] == loai, (ma, d)
        assert d["tang1"]["trang_thai"] == "DAT", (ma, d["tang1"])
        assert d["tang2"]["trang_thai"] == "DAT", (ma, d["tang2"])
    assert [_nguon(kq[m]) for m in ("d-1", "d-2", "d-3")] == [{"sp-tai-lieu-0001"}] * 3
    assert _nguon(kq["d-4"]) == {"v0-don-dieu"}
    assert _nguon(kq["d-5"]) == {"v0-don-dieu", "sp-tai-lieu-0002"}
    assert _nguon(kq["d-6"]) == {"sp-tai-lieu-0002"}
    assert {kq[m]["tang1"]["muc_bang_chung"] for m in ("d-1", "d-2", "d-3")} == {"CAS"}
    assert {kq[m]["tang1"]["muc_bang_chung"] for m in ("d-4", "d-5", "d-6")} == {"DANH_MUC"}


def test_thieu_sp_tai_lieu_0002_thi_d5_d6_thieu_can_cu():
    # Lý do có bản vá sp-tai-lieu-0002 (#103): mọi mệnh đề của dòng định lí phải có đoạn trích.
    kq, _ = _chay(BANG_V0, TAI_LIEU_V0 + [_tai_lieu_sp("sp-tai-lieu-0001")])
    assert [kq[m]["tang2"]["trang_thai"] for m in ("d-1", "d-2", "d-3", "d-4")] == ["DAT"] * 4
    assert [kq[m]["tang2"]["trang_thai"] for m in ("d-5", "d-6")] == ["KHONG_KIEM_DUOC"] * 2
    assert "không đổi dấu" in kq["d-5"]["tang2"]["ly_do"]


def test_chi_tai_lieu_v0_thi_ba_dang_thuc_thieu_can_cu():
    # Lý do có bản vá sp-tai-lieu-0001 (#84): tài liệu của v0 không phát biểu lũy thừa, tổng, thương.
    kq, _ = _chay(BANG_V0, TAI_LIEU_V0)
    assert [kq[m]["tang2"]["trang_thai"] for m in ("d-1", "d-2", "d-3")] == ["KHONG_KIEM_DUOC"] * 3
    assert kq["d-4"]["tang2"]["trang_thai"] == "DAT"


def test_tai_lieu_chia_doan_theo_cau_van_dat_va_trich_dung_doan():
    # Core gửi tài liệu đã chia đoạn (`doan`). Mỗi câu của phát biểu phải nằm nguyên văn trong một đoạn được phép.
    sp_doc = _tai_lieu_sp()
    cau = [c.strip() for c in re.split(r"(?<=[.;])\s+", sp_doc["text"]) if c.strip()]
    chia = {"id": sp_doc["id"], "license_status": "tu_soan", "doan": [{"id": "p-%d" % i, "text": c} for i, c in enumerate(cau)]}
    kq, _ = _chay(BANG_V0[:3], [chia])
    for ma, lt in (("d-1", "(x^n)'"), ("d-2", "(u+v)'"), ("d-3", "(u/v)'")):
        t2 = kq[ma]["tang2"]
        assert t2["trang_thai"] == "DAT", (ma, t2)
        assert t2["trich_dan"]["doan"].startswith("p-") and lt in t2["trich_dan"]["trich"], (ma, t2)


def test_dong_dang_thuc_latex_dung_loi_sai_khong_khoa_duoc():
    # Rà #101, mục 12: phát biểu bằng lời của dòng phải có nguyên văn trong tài liệu được phép.
    kq, _ = _chay([("l5", "Thương", r"(u/v)' = (u'v - uv') / v^2", "Với thương, tử là uv' trừ u'v, mẫu là v bình.")], _tai_lieu_lop())
    assert (kq["l5"]["tang1"]["trang_thai"], kq["l5"]["tang2"]["trang_thai"]) == ("DAT", "KHONG_KIEM_DUOC")


def test_tai_lieu_chua_ro_khong_lam_can_cu_tang_2():
    kq, toan_bo = _chay(BANG_V0[:3], [_tai_lieu_sp(quyen="chua_ro")])
    assert [kq[m]["tang2"]["trang_thai"] for m in ("d-1", "d-2", "d-3")] == ["KHONG_KIEM_DUOC"] * 3
    assert toan_bo["bo_qua"] == [{"tai_lieu": "sp-tai-lieu-0001", "ly_do": "quyen_khong_hop_le"}]


def test_cong_thuc_dung_nhung_khong_co_trong_tai_lieu():
    kq, _ = _chay([("p", "Tích", r"(uv)' = u'v + uv'", "")], _tai_lieu_lop())
    assert (kq["p"]["tang1"]["trang_thai"], kq["p"]["tang2"]["trang_thai"]) == ("DAT", "KHONG_KIEM_DUOC")


# ------------------------------------------------------------------ tầng 1: bộ ca của lượt rà độc lập (#101)
DAT, SAI, KKD = "DAT", "SAI", "KHONG_KIEM_DUOC"
CA = [
    ("T1", DAT, r"y' > 0 \Rightarrow \text{đồng biến}", ""),
    ("T2", DAT, "", "Nếu hàm số đồng biến trên khoảng K thì y' ≥ 0 trên K."),
    ("T4", SAI, "", "Nếu hàm đồng biến trên khoảng thì y' > 0 trên khoảng đó."),
    ("T5", DAT, "", "Nếu y' ≥ 0 trên khoảng K và y' = 0 chỉ tại hữu hạn điểm thì hàm đồng biến trên K."),
    ("T-thieu-huu-han", SAI, "", "Nếu y' ≥ 0 trên khoảng thì hàm đồng biến trên khoảng đó."),
    ("T15", SAI, "", "Hàm đồng biến trên khoảng K khi và chỉ khi y' ≥ 0 trên K và y' = 0 chỉ tại hữu hạn điểm."),
    ("T16", SAI, "", "Nếu hàm đồng biến trên khoảng K thì y' ≥ 0 trên K và y' = 0 chỉ tại hữu hạn điểm."),
    ("T17", SAI, "", "Nếu y' > 0 trên tập xác định thì hàm số đồng biến trên tập xác định."),
    ("T18", KKD, "", "Nếu y' < 0 với mọi x > 0 thì hàm đồng biến khi x > 0."),
    ("T19", DAT, "", "Nếu y' < 0 với mọi x thuộc khoảng (0; +∞) thì hàm nghịch biến trên khoảng (0; +∞)."),
    ("T21", KKD, "", "Nếu y' > 0 trên khoảng thì hàm không đồng biến trên khoảng đó."),
    ("T22", KKD, "", "Nếu y' ≥ 0 trên khoảng thì hàm chưa chắc đồng biến."),
    ("T23", DAT, "", "Nếu hàm đồng biến trên khoảng K thì y' > 0 hoặc y' = 0 trên K."),
    ("T24", DAT, "", "Nếu hàm đồng biến trên khoảng thì đạo hàm dương hoặc bằng 0 trên khoảng đó."),
    ("T27", KKD, "", "Nếu y'(1) > 0 thì hàm đồng biến trên ℝ."),
    ("T28", KKD, "", "Nếu y' ≥ 0 và y' = 0 không chỉ tại hữu hạn điểm thì hàm đồng biến."),
    ("T35", KKD, "", "Nếu y' > 0 trên khoảng thì hàm đồng biến và đạt cực đại tại mọi điểm."),
    ("T36", KKD, "", "Mọi nghiệm của y' = 0 đều là cực trị."),
    ("NB-nguoc", SAI, r"y' > 0 \Rightarrow \text{nghịch biến}", ""),
    ("C5", SAI, r"- \to + : \text{cực đại}", ""),
    ("C8", DAT, "", "Đạo hàm bằng 0 mà không đổi dấu thì không có cực trị."),
    ("C9", DAT, "", "Đạo hàm bằng 0 mà không đổi dấu thì hàm không đạt cực trị tại đó."),
    ("C10", SAI, "", "Đạo hàm đổi từ dương sang âm thì cực đại, từ âm sang dương thì cực đại."),
    ("C11", SAI, "", "Hàm đạt cực tiểu khi y' đổi từ dương sang âm."),
    ("C12", KKD, "", "Đạo hàm đổi từ dương sang âm thì không phải là cực đại."),
    ("C15", SAI, "", "Đạo hàm đổi từ dương sang âm thì cực đại, kể cả khi x0 không thuộc tập xác định."),
    ("C19", KKD, "", "Nếu y' đổi dấu từ dương sang âm tại x0 thì f(x0) là giá trị lớn nhất."),
    ("C22", DAT, "", "Hàm số đạt cực đại tại x0 khi y' đổi dấu từ dương sang âm khi x qua x0."),
    ("C24", KKD, r"+ \to - \Leftrightarrow \text{cực đại}", ""),
    ("KD-van", SAI, "", "Đạo hàm bằng 0 mà không đổi dấu thì vẫn là cực trị."),
    ("TH-du", DAT, "", "Điểm tới hạn là điểm thuộc tập xác định mà tại đó đạo hàm bằng 0 hoặc đạo hàm không xác định."),
    ("TH2", KKD, "", "Điểm tới hạn là điểm mà y' = 0 hoặc y' không xác định."),
    ("TH3", SAI, "", "Điểm tới hạn là điểm thuộc tập xác định mà y' = 0 hoặc hàm số không xác định."),
    ("TH5", SAI, "", "Điểm tới hạn gồm nghiệm của y' = 0 và điểm y' không xác định, kể cả điểm không thuộc tập xác định."),
    ("TH6", KKD, "", "Mọi điểm tới hạn đều là cực trị."),
    ("D27", DAT, r"(u(x)v(x))' = u'(x)v(x) + u(x)v'(x)", ""),
    ("D28", DAT, r"(u(x)+v(x))' = u'(x) + v'(x)", ""),
    ("D29", DAT, r"\left(\frac{u(x)}{v(x)}\right)' = \frac{u'(x)v(x)-u(x)v'(x)}{v(x)^2}", ""),
    ("D35", SAI, r"(u(x))' = u + u'(x)", ""),
    ("D-quen-v'", SAI, r"(u/v)' = u'/v", ""),
    ("D-tich-sai", SAI, r"(uv)' = u'v'", ""),
    ("D-dfrac-prime", DAT, r"\left(\dfrac{u}{v}\right)^{\prime} = \dfrac{u'v-uv'}{v^2}", ""),
    ("D-thuong-doi-dau", SAI, r"(u/v)' = (uv' - u'v) / v^2", ""),
    # Codex trên #101 (0ea34d5): phủ định còn sót sau các cụm đã nhận diện
    ("C-khong-suy-ra", KKD, "", "Đạo hàm đổi từ dương sang âm không suy ra cực đại."),
    ("T-khong-suy-ra", KKD, "", "y' > 0 không suy ra đồng biến."),
    ("TH-phu-dinh", KKD, "", "Điểm tới hạn không phải là điểm thuộc tập xác định mà tại đó đạo hàm bằng 0 hoặc đạo hàm không xác định."),
    # Codex trên #101 (66d15ac): hai vế phải cùng một khoảng
    ("K-khac", KKD, "", "Nếu y' > 0 trên khoảng (0; 1) thì hàm đồng biến trên khoảng (2; 3)."),
    ("K-cung", DAT, "", "Nếu y' > 0 trên khoảng (0; 1) thì hàm đồng biến trên khoảng (0; 1)."),
    ("K-K-doan", KKD, "", "Nếu y' > 0 trên khoảng K thì hàm đồng biến trên một đoạn."),
    ("K-mien-tung", KKD, "", "Nếu y' > 0 trên tập xác định thì hàm đồng biến trên từng khoảng xác định."),
    ("K-tung", DAT, "", "Nếu y' > 0 trên từng khoảng của tập xác định thì hàm đồng biến trên từng khoảng đó."),
    # Codex trên #101 (66d15ac): «A chỉ khi B» là A ⇒ B (điều kiện cần)
    ("CK-sai", SAI, "", "Hàm đồng biến trên khoảng chỉ khi y' > 0 trên khoảng."),
    ("CK-dung", DAT, "", "Hàm đồng biến trên khoảng K chỉ khi y' ≥ 0 trên K."),
    ("CK-dau-cau", KKD, "", "Chỉ khi y' > 0 trên K thì hàm đồng biến trên K."),
    ("CK-cuc-tri", KKD, "", "Hàm số đạt cực đại tại x0 chỉ khi y' đổi dấu từ dương sang âm khi x qua x0."),
    ("NEU-sau", DAT, "", "Hàm đồng biến trên K nếu y' > 0 trên K."),
    # cùng họ: chiều suy ra của dấu hiệu cực trị (câu đảo không có trong danh mục)
    ("CT-dao", KKD, "", "Nếu hàm số đạt cực đại tại x0 thì y' đổi dấu từ dương sang âm khi x qua x0."),
    ("CT-dung-khi", DAT, "", "x0 là điểm cực tiểu khi y' đổi dấu từ âm sang dương khi x qua x0."),
    ("CT-phay-thi", DAT, "", "Nếu y' đổi dấu từ dương sang âm khi x qua x0, thì x0 là điểm cực đại."),
    ("CT-neu-sau", DAT, "", "x0 là điểm cực đại nếu y' đổi dấu từ dương sang âm khi x qua x0."),
    ("KD-dao", KKD, "", "Nếu hàm không đạt cực trị tại x0 thì đạo hàm bằng 0 mà không đổi dấu."),
    ("KD-thieu-y0", KKD, "", "Nếu y' không đổi dấu khi x qua x0 thì x0 không phải là cực trị."),
    ("CT-ke-ca-khac", KKD, "", "Đạo hàm đổi từ dương sang âm thì cực đại, kể cả khi y' không xác định tại x0."),
    # cùng họ: phần đứng trước «nếu» và từ máy không biết
    ("TD-phu-dinh", KKD, "", "Không đúng rằng nếu y' > 0 trên K thì hàm đồng biến trên K."),
    ("TD-khoang", DAT, "", "Với hàm số xác định trên một khoảng: nếu y' > 0 trên khoảng đó thì hàm đồng biến."),
    ("TU-tru-khi", KKD, "", "Hàm đồng biến trên K trừ khi y' < 0 trên K."),
    ("TU-gia-su", KKD, "", "Giả sử y' > 0 thì hàm đồng biến."),
    ("TU-theo-sach", KKD, "", "Theo sách, đạo hàm đổi từ dương sang âm thì cực đại."),
    # cùng họ: định nghĩa điểm tới hạn đọc trọn
    ("TH-va", KKD, "", "Điểm tới hạn là điểm thuộc tập xác định mà tại đó đạo hàm bằng 0 và đạo hàm không xác định."),
    ("TH-hai-phu-dinh", KKD, "", "Điểm tới hạn không phải là điểm không thuộc tập xác định."),
    # Codex trên #101 (2d5ce6f): mệnh đề toán ngoài danh sách thuật ngữ không được biến mất khỏi cổng
    ("THEM-ham-chan", KKD, "", "y' > 0 ⇒ đồng biến. Hàm số này là hàm chẵn."),
    ("THEM-lom", KKD, "", "Nếu y' > 0 trên khoảng K thì hàm đồng biến trên K. Đồ thị lõm trên K."),
    ("THEM-latex", KKD, r"y' > 0 \Rightarrow \text{đồng biến}", "Hàm số lẻ thì đồ thị đối xứng qua gốc tọa độ."),
]


@pytest.mark.parametrize("ma,mong,latex,loi", CA, ids=[c[0] for c in CA])
def test_tang_1_bo_ca_ra_doc_lap(ma, mong, latex, loi):
    tieu_de = "Điểm tới hạn" if ma.startswith("TH") else ""
    d = kiem_dong_cong_thuc({"dong": [{"id": ma, "tieu_de": tieu_de, "latex": latex, "phat_bieu": loi}]})["dong"][0]
    assert d["tang1"]["trang_thai"] == mong, d["tang1"]


def test_phan_vi_du_dung_menh_de_da_viet():
    # «đồng biến ⇒ y' > 0»: x³; «y' ≥ 0 ⇒ đồng biến»: hàm hằng; «ĐB ⇒ hữu hạn điểm»: x − sin x; trên TXĐ: (2x − 1)/(x + 1)
    def pv(loi, latex=""):
        return kiem_dong_cong_thuc({"dong": [{"id": "a", "latex": latex, "phat_bieu": loi}]})["dong"][0]["tang1"]["phan_vi_du"]
    assert pv("Nếu hàm đồng biến trên khoảng thì y' > 0 trên khoảng đó.")["ham"] == "y = x**3"
    assert pv("Nếu y' ≥ 0 trên khoảng thì hàm đồng biến trên khoảng đó.")["ham"] == "y = 1"
    assert pv("Nếu hàm đồng biến trên khoảng K thì y' ≥ 0 trên K và y' = 0 chỉ tại hữu hạn điểm.")["ham"] == "y = x - sin(x)"
    tren_txd = pv("Nếu y' > 0 trên tập xác định thì hàm số đồng biến trên tập xác định.")
    assert tren_txd["ham"] == "y = (2*x - 1)/(x + 1)" and tren_txd["gia_tri"] == ["5", "-1"]
    nguoc = pv("", r"- \to + : \text{cực đại}")
    assert (nguoc["x0"], nguoc["doi_dau"], nguoc["thuc_te"]) == ("1", "- sang +", "cực tiểu")


def test_cau_khong_doc_tron_ghi_ro_ly_do():
    d = kiem_dong_cong_thuc({"dong": [{"id": "a", "latex": "", "phat_bieu": "Nếu y' > 0 trên khoảng thì hàm đồng biến. Mọi nghiệm của y' = 0 đều là cực trị."}]})["dong"][0]
    assert d["tang1"]["trang_thai"] == KKD and "chưa đọc trọn" in d["tang1"]["ly_do"]


def test_loai_may_chua_biet_khong_kiem_duoc():
    kq, _ = _chay([("k", "Nguyên hàm", r"\int x^n dx = \frac{x^{n+1}}{n+1} + C", "Nguyên hàm của x mũ n.")], [])
    assert (kq["k"]["loai"], kq["k"]["tang1"]["trang_thai"], kq["k"]["tang2"]["trang_thai"]) == ("KHONG_BIET", KKD, KKD)


@pytest.mark.parametrize("latex", [
    "(__import__('os').system('id'))' = 0",
    "(x^n)' = x^(10^(10^10))",
    "(x)' = exp(x)",
    "(" * 300 + "x" + ")" * 300 + "' = 1",
])
def test_dau_vao_doc_khong_bao_gio_DAT(latex):
    kq, _ = _chay([("x", "Độc", latex, "")], [])
    assert kq["x"]["tang1"]["trang_thai"] != DAT


def test_lo_qua_gioi_han_moi_dong_khong_kiem_duoc():
    # Codex trên #101: không cắt im lặng; core khóa khi mọi dòng trả về DAT, nên dòng không kiểm cũng phải có kết quả.
    kq = kiem_dong_cong_thuc({"dong": [{"id": str(i), "latex": "(u+v)' = u' + v'"} for i in range(100)], "tai_lieu": []})
    assert len(kq["dong"]) == 100 and kq["loi"] == "QUA_NHIEU_DONG"
    assert {d["tang1"]["trang_thai"] for d in kq["dong"]} == {KKD}


def test_doan_phu_dinh_khong_lam_can_cu_tang_2():
    phu_dinh = {"id": "tl-x", "license_status": "tu_soan", "text": "Đạo hàm đổi từ dương sang âm thì không phải là cực đại."}
    kq, _ = _chay([("c", "Cực trị", r"+ \to - : \text{cực đại}", "")], [phu_dinh])
    assert (kq["c"]["tang1"]["trang_thai"], kq["c"]["tang2"]["trang_thai"]) == (DAT, KKD)


DONG_DB = ("r", "", r"y' > 0 \Rightarrow \text{đồng biến}", "")
DONG_TONG = BANG_V0[1][:3] + ("",)


@pytest.mark.parametrize("dong,doan,mong", [
    # Codex trên #101 (66d15ac): đoạn sai khoảng hay ngược chiều không làm căn cứ cho định lí
    (DONG_DB, "Nếu y' > 0 trên khoảng (0; 1) thì hàm đồng biến trên khoảng (2; 3).", (DAT, KKD)),
    (DONG_DB, "Hàm đồng biến trên khoảng chỉ khi y' > 0 trên khoảng.", (DAT, KKD)),
    # đoạn nói về một khoảng cụ thể không đỡ định lí tổng quát; chiều ngược lại thì đỡ
    (("r", "", "", "Nếu y' > 0 trên khoảng K thì hàm đồng biến trên K."),
     "Nếu y' > 0 trên khoảng (0; 1) thì hàm đồng biến trên khoảng (0; 1).", (DAT, KKD)),
    (("r", "", "", "Nếu y' > 0 trên khoảng (0; 1) thì hàm đồng biến trên khoảng (0; 1)."),
     "Nếu y' > 0 trên khoảng K thì hàm đồng biến trên K.", (DAT, DAT)),
    # Codex trên #101 (66d15ac): công thức phải được phát biểu trọn — đóng khung, khớp trọn, nhãn không phủ định
    (DONG_TONG, "Công thức $(u+v)' = u' + v'$ là sai.", (DAT, KKD)),
    (DONG_TONG, "Đạo hàm tổng: $(u+v)' = u' + v' + 1$.", (DAT, KKD)),
    (DONG_TONG, "Sai lầm thường gặp: $(u+v)' = u' + v'$.", (DAT, KKD)),
    (DONG_TONG, "Đạo hàm tổng: (u+v)' = u' + v'.", (DAT, KKD)),
    (DONG_TONG, "Đạo hàm tổng: $(u+v)' = u' + v'$.", (DAT, DAT)),
    # câu của phát biểu phải trùng trọn một câu của đoạn, không là chuỗi con của câu phủ định
    (BANG_V0[1], "Đạo hàm tổng: $(u+v)' = u' + v'$. Không phải đạo hàm của tổng bằng tổng các đạo hàm.", (DAT, KKD)),
], ids=["khoang-khac", "chi-khi", "cu-the-khong-do-tong-quat", "tong-quat-do-cu-the", "cong-thuc-la-sai",
        "cong-thuc-dai-hon", "nhan-sai-lam", "khong-dong-khung", "cong-thuc-dung", "cau-phu-dinh"])
def test_tang_2_can_cu_dung_menh_de(dong, doan, mong):
    kq, _ = _chay([dong], [{"id": "tl-x", "license_status": "tu_soan", "text": doan}])
    d = kq[dong[0]]
    assert (d["tang1"]["trang_thai"], d["tang2"]["trang_thai"]) == mong, d["tang2"]


@pytest.mark.skipif(os.name == "nt", reason="sandbox dùng preexec_fn và resource: chỉ chạy trên Linux (CI)")
def test_router_kiem_dong_cong_thuc():
    from fastapi.testclient import TestClient

    from app.main import app

    r = TestClient(app).post("/v1/kiem-dong-cong-thuc", json={"dong": _dong(BANG_V0[1]), "tai_lieu": [_tai_lieu_sp()]})
    assert r.status_code == 200
    d = r.json()["dong"][0]
    assert (d["loai"], d["tang1"]["trang_thai"], d["tang2"]["trang_thai"]) == ("DANG_THUC", DAT, DAT)
