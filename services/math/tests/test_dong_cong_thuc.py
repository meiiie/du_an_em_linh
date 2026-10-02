# -*- coding: utf-8 -*-
"""Job kiem_dong_cong_thuc (ADR 013, #83, T042b–T042c).

Bảng 6 dòng của v0 phải DAT cả 6 ở tầng 1 và tầng 2 với 4 tài liệu của lớp (3 của v0 + sp-tai-lieu-0001).
Ca âm: quy tắc sai bị SAI kèm phản ví dụ; loại máy chưa biết, tài liệu `chua_ro`, đầu vào độc không bao giờ DAT.
«Hết giờ → không DAT» do sandbox (test_sandbox.py) và client của core (#82, T009) bảo đảm: kết quả hết giờ không có `dong`.
"""
import io
import json
import os

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


def _tai_lieu_sp(quyen=None):
    """sp-tai-lieu-0001: tệp dữ liệu nếu #85 đã áp bản vá, không thì đọc thẳng từ bản vá của lab (#84)."""
    p = os.path.join(REPO, "data", "supham", "tai-lieu", "sp-tai-lieu-0001.json")
    if os.path.exists(p):
        doc = json.load(io.open(p, encoding="utf-8"))
    else:
        patch = io.open(os.path.join(REPO, "labs", "pedagogy", "ban-va", "sp-tai-lieu-0001.patch"), encoding="utf-8").read()
        doc = json.loads("\n".join(d[1:] for d in patch.splitlines() if d.startswith("+") and not d.startswith("+++")))
    return {"id": doc["ma"], "license_status": quyen or doc["licenseStatus"], "text": doc["textContent"]}


def _dong(*rows):
    return [{"id": i, "tieu_de": t, "latex": l, "phat_bieu": p} for i, t, l, p in rows]


def _chay(rows, tai_lieu):
    kq = kiem_dong_cong_thuc({"dong": _dong(*rows), "tai_lieu": tai_lieu})
    return {d["id"]: d for d in kq["dong"]}, kq


def test_bang_v0_dat_ca_6_o_hai_tang():
    kq, _ = _chay(BANG_V0, TAI_LIEU_V0 + [_tai_lieu_sp()])
    for ma, loai in (("d-1", "DANG_THUC"), ("d-2", "DANG_THUC"), ("d-3", "DANG_THUC"),
                     ("d-4", "DINH_LI"), ("d-5", "DINH_LI"), ("d-6", "DINH_LI")):
        d = kq[ma]
        assert d["loai"] == loai, (ma, d)
        assert d["tang1"]["trang_thai"] == "DAT", (ma, d["tang1"])
        assert d["tang2"]["trang_thai"] == "DAT", (ma, d["tang2"])
    # căn cứ tầng 2 là đúng tài liệu: 3 đẳng thức từ sp-tai-lieu-0001, 3 định lí từ ghi chú đơn điệu của v0
    assert {kq[m]["tang2"]["trich_dan"]["tai_lieu"] for m in ("d-1", "d-2", "d-3")} == {"sp-tai-lieu-0001"}
    assert {kq[m]["tang2"]["trich_dan"]["tai_lieu"] for m in ("d-4", "d-5", "d-6")} == {"v0-don-dieu"}


def test_tai_lieu_chia_doan_theo_cau_van_dat_va_trich_dung_doan():
    # Core gửi tài liệu đã chia đoạn (`doan`). Phát biểu dòng lũy thừa dài hai câu nên không nằm trọn trong một
    # đoạn câu; LaTeX của dòng vẫn nằm trọn trong một đoạn nên tầng 2 vẫn đạt, và trích dẫn trỏ đúng đoạn đó.
    import re

    sp_doc = _tai_lieu_sp()
    cau = [c.strip() for c in re.split(r"(?<=[.;])\s+", sp_doc["text"]) if c.strip()]
    chia = {"id": sp_doc["id"], "license_status": "tu_soan", "doan": [{"id": "p-%d" % i, "text": c} for i, c in enumerate(cau)]}
    kq, _ = _chay(BANG_V0[:3], [chia])
    for ma, lt in (("d-1", "(x^n)'"), ("d-2", "(u+v)'"), ("d-3", "(u/v)'")):
        t2 = kq[ma]["tang2"]
        assert t2["trang_thai"] == "DAT", (ma, t2)
        assert t2["trich_dan"]["doan"].startswith("p-") and lt in t2["trich_dan"]["trich"], (ma, t2)


def test_chi_tai_lieu_v0_thi_ba_dang_thuc_thieu_can_cu():
    # Lý do có bản vá sp-tai-lieu-0001 (#84): tài liệu của v0 không phát biểu lũy thừa, tổng, thương.
    kq, _ = _chay(BANG_V0, TAI_LIEU_V0)
    assert [kq[m]["tang2"]["trang_thai"] for m in ("d-1", "d-2", "d-3")] == ["KHONG_KIEM_DUOC"] * 3
    assert [kq[m]["tang2"]["trang_thai"] for m in ("d-4", "d-5", "d-6")] == ["DAT"] * 3


def test_dinh_li_dat_ghi_ro_khong_phai_chung_minh():
    kq, _ = _chay(BANG_V0[3:], TAI_LIEU_V0)
    assert all("không phải chứng minh" in kq[m]["tang1"]["can_cu"] for m in ("d-4", "d-5", "d-6"))


def test_thuong_doi_dau_tu_so_la_SAI_kem_phan_vi_du():
    kq, _ = _chay([("s", "Đạo hàm thương", r"(u/v)' = (uv' - u'v) / v^2", "")], [])
    t1 = kq["s"]["tang1"]
    assert (kq["s"]["loai"], t1["trang_thai"]) == ("DANG_THUC", "SAI")
    assert t1["phan_vi_du"]["hieu_hai_ve"] != "0"


def test_frac_latex_dung_dat():
    kq, _ = _chay([("q", "Thương", r"\left(\frac{u}{v}\right)' = \frac{u'v-uv'}{v^2}", "")], [])
    assert kq["q"]["tang1"]["trang_thai"] == "DAT"


def test_dong_bien_suy_ra_dao_ham_duong_la_SAI_phan_vi_du_x3():
    kq, _ = _chay([("m", "Đảo", "", "Nếu hàm đồng biến trên khoảng thì y' > 0 trên khoảng đó.")], [])
    t1 = kq["m"]["tang1"]
    assert (kq["m"]["loai"], t1["trang_thai"]) == ("DINH_LI", "SAI")
    assert t1["phan_vi_du"]["ham"] == "y = x**3"


def test_thieu_dieu_kien_huu_han_diem_la_SAI():
    kq, _ = _chay([("m", "Thiếu điều kiện", "", "Nếu y' ≥ 0 trên khoảng thì hàm đồng biến trên khoảng đó.")], [])
    t1 = kq["m"]["tang1"]
    assert t1["trang_thai"] == "SAI"
    assert t1["phan_vi_du"]["ham"] == "y = 1"  # hàm hằng: y' = 0 ≥ 0 nhưng không đồng biến


def test_dao_ham_duong_suy_ra_nghich_bien_la_SAI():
    kq, _ = _chay([("m", "Ngược chiều", r"y' > 0 \Rightarrow \text{nghịch biến}", "")], [])
    assert kq["m"]["tang1"]["trang_thai"] == "SAI"


def test_cuc_tri_nguoc_la_SAI():
    kq, _ = _chay([("c", "Ngược", "", "Đạo hàm đổi từ dương sang âm thì cực tiểu.")], [])
    t1 = kq["c"]["tang1"]
    assert t1["trang_thai"] == "SAI"
    assert t1["phan_vi_du"]["thuc_te"] == "cực đại"


def test_khong_doi_dau_van_la_cuc_tri_la_SAI():
    kq, _ = _chay([("c", "Sai", "", "Đạo hàm bằng 0 mà không đổi dấu thì vẫn là cực trị.")], [])
    t1 = kq["c"]["tang1"]
    assert t1["trang_thai"] == "SAI"
    assert t1["phan_vi_du"]["ham"] == "y = x**3"


def test_loai_may_chua_biet_khong_kiem_duoc():
    kq, _ = _chay([("k", "Nguyên hàm", r"\int x^n dx = \frac{x^{n+1}}{n+1} + C", "Nguyên hàm của x mũ n.")], [])
    assert (kq["k"]["loai"], kq["k"]["tang1"]["trang_thai"], kq["k"]["tang2"]["trang_thai"]) == \
        ("KHONG_BIET", "KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC")


def test_dinh_nghia_toi_han_thieu_thanh_phan_khong_dat():
    kq, _ = _chay([("t", "Điểm tới hạn", "", "Điểm tới hạn là nghiệm của đạo hàm bằng 0.")], [])
    assert kq["t"]["tang1"]["trang_thai"] == "KHONG_KIEM_DUOC"


def test_tai_lieu_chua_ro_khong_lam_can_cu_tang_2():
    kq, toan_bo = _chay(BANG_V0[:3], [_tai_lieu_sp(quyen="chua_ro")])
    assert [kq[m]["tang2"]["trang_thai"] for m in ("d-1", "d-2", "d-3")] == ["KHONG_KIEM_DUOC"] * 3
    assert toan_bo["bo_qua"] == [{"tai_lieu": "sp-tai-lieu-0001", "ly_do": "quyen_khong_hop_le"}]


def test_cong_thuc_dung_nhung_khong_co_trong_tai_lieu():
    kq, _ = _chay([("p", "Tích", r"(uv)' = u'v + uv'", "")], TAI_LIEU_V0 + [_tai_lieu_sp()])
    assert (kq["p"]["tang1"]["trang_thai"], kq["p"]["tang2"]["trang_thai"]) == ("DAT", "KHONG_KIEM_DUOC")


@pytest.mark.parametrize("latex", [
    "(__import__('os').system('id'))' = 0",
    "(x^n)' = x^(10^(10^10))",
    "(x)' = exp(x)",
    "(" * 300 + "x" + ")" * 300 + "' = 1",
])
def test_dau_vao_doc_khong_bao_gio_DAT(latex):
    kq, _ = _chay([("x", "Độc", latex, "")], [])
    assert kq["x"]["tang1"]["trang_thai"] != "DAT"


def test_gioi_han_so_dong():
    kq = kiem_dong_cong_thuc({"dong": [{"id": str(i), "latex": "(u+v)' = u' + v'"} for i in range(100)], "tai_lieu": []})
    assert len(kq["dong"]) == 60


@pytest.mark.skipif(os.name == "nt", reason="sandbox dùng preexec_fn và resource: chỉ chạy trên Linux (CI)")
def test_router_kiem_dong_cong_thuc():
    from fastapi.testclient import TestClient

    from app.main import app

    r = TestClient(app).post("/v1/kiem-dong-cong-thuc", json={"dong": _dong(BANG_V0[1]), "tai_lieu": [_tai_lieu_sp()]})
    assert r.status_code == 200
    d = r.json()["dong"][0]
    assert (d["loai"], d["tang1"]["trang_thai"], d["tang2"]["trang_thai"]) == ("DANG_THUC", "DAT", "DAT")
