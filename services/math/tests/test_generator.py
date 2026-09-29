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
    assert "y' đồng biến" not in caps["B.DH.XETDAU"][1] and "nhân tử" in caps["B.DH.XETDAU"][1]
    assert "Thử lần lượt x =" in caps["B.DH.XETDAU"][2]
    for dang in ("bac_ba", "trung_phuong", "huu_ti"):
        for seed in (1, 2, 3, 4, 5):
            r = sinh({"dang": dang, "seed": seed})
            for b in r["thang_goi_y"]:
                for c in b["cac_cap"]:
                    assert "Mình không đưa kết quả" not in c["noi_dung"], (dang, seed, b["ma_buoc"], c["cap"])
