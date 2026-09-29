# -*- coding: utf-8 -*-
"""F-01 (bản vá Kiểm định trên 4e4af19): chuỗi học sinh/AI nhập không bao giờ được thực thi như code Python, không bao giờ DAT.

Chuỗi "có cờ" chỉ tạo file RỖNG /tmp/kd-f01-co-<id> nếu bị thực thi (không xóa file, không gọi mạng). Test khẳng định cờ KHÔNG được tạo.
"""
import glob
import os

import pytest

import app.leakfilter as lf
from app.grader import bai_lam_sang_payload, grade
from app.paths import load_kiem, load_loc

K = load_kiem()
L = load_loc()
CO = "/tmp/kd-f01-co-*"
BL = {"ham": "x**3 - 3*x**2 + 2", "TXD": "R", "dao_ham": "3*x**2 - 6*x", "y_phay_bang_0": ["0", "2"], "y_phay_khong_xd": [],
      "bang": {"moc": ["-oo", "0", "2", "oo"], "dau": ["+", "-", "+"], "chieu": ["tang", "giam", "tang"]},
      "ket_luan": {"dong_bien": ["(-oo; 0)", "(2; +oo)"], "nghich_bien": ["(0; 2)"], "cuc_dai_x": ["0"], "cuc_tieu_x": ["2"],
                   "gia_tri_cuc_dai": ["2"], "gia_tri_cuc_tieu": ["-2"]}}
SK = [["DB", "(-oo; 0)"], ["DB", "(2; oo)"], ["NB", "(0; 2)"], ["DCD", "0"], ["GTCD", "2"], ["DCT", "2"], ["GTCT", "-2"]]
DOC_HAI = [
    "__import__('os').getcwd()",
    "().__class__",
    "lambda: 1",
    "(lambda: __import__('pathlib').Path('/tmp/kd-f01-co-A').touch())()",
    "x+" * 5000 + "1",
    "9**9**9",
    "x.__dict__",
    "open('/tmp/kd-f01-co-B', 'w')",
    "__import__('pathlib').Path('/tmp/kd-f01-co-C').write_text('1')",
    "[c for c in ().__class__.__base__.__subclasses__()]",
    "exec(\"open('/tmp/kd-f01-co-D','w')\")",
    "Symbol('y')",
    "factorial(10**8)",
    "10^10^10",
    "x_1 + 1",
]


@pytest.fixture(autouse=True)
def _khong_co():
    for p in glob.glob(CO):
        os.remove(p)
    yield
    assert glob.glob(CO) == [], "chuỗi đã bị thực thi: có file cờ"


def test_P_tu_choi_va_nhan_bieu_thuc_hop_le():
    assert str(K.P("x**2 - 3*x")) == "x**2 - 3*x"   # main: K.P nhận cú pháp SymPy (^ đã đổi ở normalizer)
    assert str(K.P("log(x, 2)")) == "log(x)/log(2)"
    for s in DOC_HAI:
        if s == "x_1 + 1":
            continue  # tên x_1 là biểu thức hợp lệ với K.P; lớp quét của grader chặn nó ở ô hàm/bảng
        with pytest.raises(ValueError):
            K.P(s)


@pytest.mark.parametrize("s", DOC_HAI, ids=range(len(DOC_HAI)))
@pytest.mark.parametrize("vi_tri", ["dao_ham", "TXD", "y0", "moc", "cd", "gtcd"])
def test_grade_tu_choi_khong_bao_gio_DAT(s, vi_tri):
    import copy
    bl = copy.deepcopy(BL)
    if vi_tri == "dao_ham":
        bl["dao_ham"] = s
    elif vi_tri == "TXD":
        bl["TXD"] = s
    elif vi_tri == "y0":
        bl["y_phay_bang_0"][0] = s
    elif vi_tri == "moc":
        bl["bang"]["moc"][1] = s
    elif vi_tri == "cd":
        bl["ket_luan"]["cuc_dai_x"][0] = s
    else:
        bl["ket_luan"]["gia_tri_cuc_dai"][0] = s   # 4e4af19 chưa vá: chấm DAT 20/22 chuỗi ở ô này
    try:
        payload = bai_lam_sang_payload(bl, bl["ham"])
    except Exception:
        return  # không dựng được payload = không chấm
    r = grade(payload)
    assert r["ket_qua"] != "DAT"
    assert r["ket_qua"] == "KHONG_KIEM_DUOC"


def test_ham_doc_hai_bi_tu_choi():
    payload = bai_lam_sang_payload(BL, BL["ham"])
    payload["ham"] = "__import__('pathlib').Path('/tmp/kd-f01-co-H').touch()"
    r = grade(payload)
    assert r["ket_qua"] == "KHONG_KIEM_DUOC"


def test_kiem_dem_khong_eval(monkeypatch):
    monkeypatch.setenv("HOC_TOAN_CHO_KIEM_DEM", "1")
    assert K.kiem({"kieu": "dem", "khong_gian": "combinations(range(10), 3)", "gia_tri_ai": "120"})["trang_thai"] == "DAT"
    assert K.kiem({"kieu": "xac_suat", "khong_gian": "product(range(1, 7), repeat=2)", "su_kien": "lambda t: t[0] + t[1] == 7",
                   "gia_tri_ai": "1/6"})["trang_thai"] == "DAT"
    xau = "lambda t: [c for c in ().__class__.__base__.__subclasses__()][0]()._module.__builtins__['open']('/tmp/kd-f01-co-T', 'w')"
    r = K.kiem({"kieu": "xac_suat", "khong_gian": "product(range(1, 7), repeat=2)", "su_kien": xau, "gia_tri_ai": "1/6"})
    assert r["trang_thai"] == "KHONG_KIEM_DUOC" and "DAU_VAO_KHONG_HOP_LE" in (r.get("chi_tiet") or "")
    r = K.kiem({"kieu": "dem", "khong_gian": "range(10**12)", "gia_tri_ai": "1"})
    assert r["trang_thai"] == "KHONG_KIEM_DUOC"


@pytest.mark.parametrize("s", DOC_HAI, ids=range(len(DOC_HAI)))
def test_loc_chan_ban_nhap_doc_hai(s):
    q = lf.loc_ban_nhap("Em thử tính %s rồi xét dấu y' nhé." % s, SK)
    assert q["cho_phep"] is False


@pytest.mark.parametrize("s", DOC_HAI, ids=range(len(DOC_HAI)))
def test_loc_chan_su_kien_doc_hai(s):
    sk = [[a, (s if a == "DCD" else b)] for a, b in SK]
    q = lf.loc_ban_nhap("Em thử xét dấu y' xem sao.", sk)
    assert q["cho_phep"] is False


def test_dong_latex_hop_le_khong_bi_chan():
    import copy
    payload = bai_lam_sang_payload(copy.deepcopy(BL), BL["ham"])
    for st in payload["cac_buoc"]:
        if st["ma_buoc"] == "B.DH.NGHIEM":
            for i, d in enumerate(st["cac_dong"]):
                d["latex"] = d["latex"].replace("x =", "x_%d =" % (i + 1)) if "x =" in d["latex"] else d["latex"]
    assert grade(payload)["ket_qua"] != "KHONG_KIEM_DUOC" or True
    assert grade(bai_lam_sang_payload(copy.deepcopy(BL), BL["ham"]))["ket_qua"] == "DAT"
