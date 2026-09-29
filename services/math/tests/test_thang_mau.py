# -*- coding: utf-8 -*-
"""Thang gợi ý mẫu Sư phạm (supham/thang-goi-y-mau v0.1) nối vào generator / máy giải / job goi_y."""
import hashlib
import json
import os

import pytest

from app import thang_mau as tm
from app.generator import _hints, sinh

SHA = {  # README Sư phạm §6 (16 ký tự đầu) — file chép nguyên văn, không sửa tay
    "bac-ba.json": "7a57ac9d9bf9e2d2544b29e0cbc07aba5b0e5299fc074fbfd897bc8a3fc95afa",
    "trung-phuong.json": "5afc324873cb8c177d59a416dd027ae12f961ab2517fbd1abbe65a79e20a27fa",
    "huu-ti.json": "e29ca4ef4c5c515fe18ef6680cc8d619f8de94e0140461db5a2278fbec54f5c8",
}


@pytest.mark.parametrize("ten", sorted(SHA))
def test_file_chep_nguyen_van(ten):
    with open(os.path.join(tm.THU_MUC, ten), "rb") as f:
        assert hashlib.sha256(f.read()).hexdigest() == SHA[ten]


def test_dem_thang_cap():
    so_thang, co_nd, rong = 0, 0, 0
    for dang in tm.FILE:
        for mb, v in tm.nap(dang)["thang"].items():
            for k, t in v.items():
                if k == "_meta":
                    continue
                so_thang += 1
                for c in "123":
                    if t[c]["noi_dung"] is None:
                        rong += 1
                    else:
                        co_nd += 1
    assert (so_thang, co_nd, rong) == (52, 152, 4)


@pytest.mark.parametrize("ham,dang", [
    ("x**3 + (-3)*x**2 + (0)*x + (2)", "bac_ba"), ("x**4 + (-2)*x**2 + (1)", "trung_phuong"),
    ("x/(x+3)", "huu_ti"), ("(2*x-1)/(x+1)", "huu_ti"), ("x**2-2*x", None), ("x**4+x**3", None),
    ("(x**2+1)/(x-1)", None), ("__import__('os')", None), ("", None), (None, None)])
def test_dang_cua(ham, dang):
    assert tm.dang_cua(ham) == dang


def test_tham_so_nguyen_van_tu_de():
    assert tm.tham_so_tu_de("Tìm … của hàm số $y = \\frac{2 x - 1}{x + 1}$.") == {
        "ham": "$\\frac{2 x - 1}{x + 1}$", "tu": "$2 x - 1$", "mau": "$x + 1$"}
    assert tm.tham_so_tu_de("… hàm số y = x^{3} - 3 x^{2} + 2.") == {"ham": "$x^{3} - 3 x^{2} + 2$"}
    assert tm.tham_so_tu_de("Cho hàm số y = f(x) có bảng biến thiên") == {}


def test_dien_bo_cau_co_cho_dien_la_hoac_thieu():
    assert tm.dien({"noi_dung": "Hàm {ham}.", "tham_so": ["ham"]}, {"ham": "$x$"}) == "Hàm $x$."
    assert tm.dien({"noi_dung": "Hàm {ham} và {dap_an}.", "tham_so": ["ham", "dap_an"]}, {"ham": "$x$", "dap_an": "1"}) is None
    assert tm.dien({"noi_dung": "Mẫu {mau}.", "tham_so": ["mau"]}, {"ham": "$x$"}) is None
    assert tm.dien({"noi_dung": "Hàm {ham}.", "tham_so": []}, {"ham": "$x$"}) is None   # chỗ điền không khai báo
    assert tm.dien({"noi_dung": "D = ℝ \\ {…}", "tham_so": []}, {}) == "D = ℝ \\ {…}"
    assert tm.dien({"noi_dung": None}, {"ham": "$x$"}) is None


def _g(dang, seed=3):
    g = sinh({"dang": dang, "seed": seed})
    assert not g.get("loi"), g
    return g


def _hoi(g, mb, loai, cap):
    return tm.goi_y({"ham": g["ham"], "de_bai": g["de_bai"], "ma_buoc": mb, "loai_ket_qua": loai, "cap": cap,
                     "su_kien": g["su_kien"]})


def test_chon_thang():
    g = _g("bac_ba")
    assert _hoi(g, "B.DH.XETDAU", "DAU_DOI_TRONG_KHOANG", 1)["thang"] == "B.DH.NGHIEM/DIEM_THIEU"
    assert _hoi(g, "B.DH.XETDAU", "SAI_DAU", 1)["thang"] == "B.DH.XETDAU/SAI_DAU"
    r = _hoi(g, "B.DH.XETDAU", "KHONG_KIEM_DUOC", 1)
    assert r["thang"] == "B.DH.XETDAU/chung" and r["rieng"] is False
    assert _hoi(g, "B.DH.TXD", "LOAI_LA", 2)["thang"] == "B.DH.TXD/chung"
    r = _hoi(g, "B.DH.KETLUAN", "DAT", 1)
    assert r["noi_dung"] is None and r["hanh_dong"] is None


def test_cap_rong_khong_lo_ly_do_trong():
    g = _g("bac_ba")
    for loai in ("SAI_TXD", "chung"):
        r = _hoi(g, "B.DH.TXD", loai, 3)
        assert r["noi_dung"] is None and r["hanh_dong"] == "BAI_TUONG_TU_DE_HON"
        assert "ly_do_trong" not in r and "ly_do_trong" not in json.dumps(r)


def test_bi_chan_thi_lui_cap_roi_ve_chung(monkeypatch):
    import app.leakfilter as lf
    g = _g("huu_ti")
    cau3 = tm.dien(tm.nap("huu_ti")["thang"]["B.DH.KETLUAN"]["SAI_KET_LUAN"]["3"], tm.tham_so_tu_de(g["de_bai"]))
    monkeypatch.setattr(lf, "loc_ban_nhap", lambda s, sk, c: {"cho_phep": s != cau3, "ly_do": "LO_DAP_AN"})
    r = _hoi(g, "B.DH.KETLUAN", "SAI_KET_LUAN", 3)
    assert r["cap"] == 2 and r["noi_dung"] and r["nhat_ky"] == [{"thang": "B.DH.KETLUAN/SAI_KET_LUAN", "cap": 3, "ly_do": "LO_DAP_AN"}]
    monkeypatch.setattr(lf, "loc_ban_nhap", lambda s, sk, c: {"cho_phep": False, "ly_do": "LO_DAP_AN"})
    r = _hoi(g, "B.DH.KETLUAN", "SAI_KET_LUAN", 2)
    assert r["noi_dung"] is None and [x["cap"] for x in r["nhat_ky"]] == [2, 1, 2]
    assert r["nhat_ky"][-1]["thang"] == "B.DH.KETLUAN/chung"


def test_moi_cau_rieng_that_qua_loc_that():
    """Không mock: mọi (bước, thang, cấp) có nội dung của 3 dạng x 3 seed đều trả câu (không rỗng), không chữ '{'."""
    n = 0
    for dang in tm.FILE:
        for seed in (1, 2, 3):
            g = _g(dang, seed)
            for mb, v in tm.nap(dang)["thang"].items():
                for loai, t in v.items():
                    if loai == "_meta":
                        continue
                    for c in (1, 2, 3):
                        r = _hoi(g, mb, loai, c)
                        if t[str(c)]["noi_dung"] is None:
                            assert r["hanh_dong"] == "BAI_TUONG_TU_DE_HON"
                            continue
                        assert r["noi_dung"], (dang, seed, mb, loai, c, r)
                        assert "{ham}" not in r["noi_dung"] and "{tu}" not in r["noi_dung"] and "{mau}" not in r["noi_dung"]
                        n += 1
    assert n >= 3 * 3 * 48


def test_generator_da_thuc_khong_dung_chung_mot_thang():
    ba, tp = _g("bac_ba"), _g("trung_phuong")
    cau = lambda g: [c["noi_dung"] for b in g["thang_goi_y"] for c in b["cac_cap"] if c["noi_dung"]]
    assert cau(ba) != cau(tp)
    x3 = [c for b in ba["thang_goi_y"] if b["ma_buoc"] == "B.DH.XETDAU" for c in b["cac_cap"]]
    x4 = [c for b in tp["thang_goi_y"] if b["ma_buoc"] == "B.DH.XETDAU" for c in b["cac_cap"]]
    assert "tam thức bậc hai" in x3[1]["noi_dung"] and "bậc ba" in x4[1]["noi_dung"]
    # điểm thử cụ thể nằm trong danh sách cấm của thang mẫu (lộ vị trí mốc)
    assert not any("Thử lần lượt x =" in s for s in cau(ba) + cau(tp))


def test_generator_cap_rong_va_nguon():
    g = _g("bac_ba")
    txd = [b for b in g["thang_goi_y"] if b["ma_buoc"] == "B.DH.TXD"][0]["cac_cap"]
    assert txd[2]["noi_dung"] is None and txd[2]["hanh_dong"] == "BAI_TUONG_TU_DE_HON"
    assert txd[0]["nguon"] == "thang-mau/bac_ba/B.DH.TXD/chung/1" and txd[0]["qua_loc"] is True


def test_huu_ti_quy_tac_thuong_khong_tach_tong():
    for seed in range(1, 9):
        g = _g("huu_ti", seed)
        s = " ".join(c["noi_dung"] or "" for b in g["thang_goi_y"] for c in b["cac_cap"])
        assert "từng hạng tử" not in s and "tách tổng" not in s
        dh = [c["noi_dung"] for b in g["thang_goi_y"] if b["ma_buoc"] == "B.DH.DAOHAM" for c in b["cac_cap"]]
        assert any("thương" in (d or "") for d in dh)


def test_dang_ngoai_thang_mau_giu_thang_du_phong():
    from app.machine import bai_lam_may
    bl = bai_lam_may("x**2 - 2*x")
    h = _hints(bl)
    assert [b["ma_buoc"] for b in h] == list(tm.BUOC)
    assert not any((c.get("nguon") or "").startswith("thang-mau") for b in h for c in b["cac_cap"])
