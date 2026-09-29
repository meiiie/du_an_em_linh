# -*- coding: utf-8 -*-
"""SP-06, SP-11: nhãn mức bài sinh máy và thang gợi ý theo bài."""
from app.generator import sinh


def test_nhan_muc_va_ky_nang():
    for dang in ("bac_ba", "trung_phuong", "huu_ti"):
        r = sinh({"dang": dang, "seed": 3})
        assert r["muc_do_4"] == "VAN_DUNG", dang
    assert sinh({"dang": "huu_ti", "seed": 3})["ky_nang_chinh"] == "T12.DH.03"


def test_goi_y_theo_bai_va_khong_bi_thay_cau_chung():
    r = sinh({"dang": "huu_ti", "seed": 5})
    caps = {b["ma_buoc"]: [c["noi_dung"] for c in b["cac_cap"]] for b in r["thang_goi_y"]}
    assert "tách tổng" not in " ".join(caps["B.DH.DAOHAM"]).lower()
    assert "thương" in caps["B.DH.DAOHAM"][0]
    assert "y' đồng biến" not in caps["B.DH.XETDAU"][1] and "thương" in caps["B.DH.XETDAU"][1]
    assert "Thử lần lượt x =" in caps["B.DH.XETDAU"][2]
    for dang in ("bac_ba", "trung_phuong", "huu_ti"):
        for seed in (1, 2, 3, 4, 5):
            r = sinh({"dang": dang, "seed": seed})
            for b in r["thang_goi_y"]:
                for c in b["cac_cap"]:
                    assert "Mình không đưa kết quả" not in (c["noi_dung"] or ""), (dang, seed, b["ma_buoc"], c["cap"])
                    if c["noi_dung"] is None:
                        assert c.get("ly_do_trong")


def test_huu_ti_khong_bao_gio_dao_ham_tung_hang_tu():
    from app.generator import _hints
    from app.machine import bai_lam_may
    for ham in ("x/(x+3)", "(x+1)/(x-1)", "(2*x-1)/(x+2)"):
        blob = str(_hints(bai_lam_may(ham))).lower()
        assert "từng hạng tử" not in blob and "tách tổng" not in blob, ham
        assert "thương" in blob


def test_txd_cap3_de_trong():
    from app.generator import _hints
    from app.machine import bai_lam_may
    txd = [b for b in _hints(bai_lam_may("x**3-3*x")) if b["ma_buoc"] == "B.DH.TXD"][0]
    assert txd["cac_cap"][2]["noi_dung"] is None
