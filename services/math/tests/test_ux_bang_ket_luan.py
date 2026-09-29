"""UX-04 / UX-05 (bảng xét dấu, kết luận): bộ chấm nêu đủ vấn đề cho màn học sinh + màn giáo viên.

- UXT-04-a: mốc thừa ở hàng x → thông báo không nói tới «bước nghiệm» (lỗi nằm ở bảng).
- UXT-04-d: thiếu nghiệm ở bước Nghiệm, bảng theo mốc thiếu → có hệ quả DAU_DOI_TRONG_KHOANG ở ô dấu khoảng bị đổi dấu.
- UXT-05-b/c: kết luận sai hai ô → hai vấn đề SAI_KET_LUAN (ô Cực đại dòng 2 và ô Cực tiểu dòng 3), vấn đề đầu là Cực đại.
"""
from fastapi.testclient import TestClient

from app.main import app

c = TestClient(app)


def _grade(ham, cac_buoc, nop_toi):
    r = c.post("/v1/grade", json={"ham": ham, "nop_toi": nop_toi, "cac_buoc": cac_buoc})
    assert r.status_code == 200, r.text
    return r.json()


def _xd(moc, dau, mui):
    cells = [{"hang": "X", "k": i, "gia_tri": m} for i, m in enumerate(moc)]
    cells += [{"hang": "DAU_YPHAY", "k": k, "gia_tri": v} for k, v in dau.items()]
    cells += [{"hang": "BIEN_THIEN", "k": k, "gia_tri": v} for k, v in mui.items()]
    return {"ma_buoc": "B.DH.XETDAU", "bang": {"loai_bang": "XET_DAU", "cac_o": cells}}


def _dau(txd, dh, ng):
    return [
        {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": txd}]},
        {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": dh}]},
        {"ma_buoc": "B.DH.NGHIEM", "cac_dong": [{"dong": i, "latex": n, "loai": "NGHIEM"} for i, n in enumerate(ng)]},
    ]


def _kl(db, nb, cd, ct):
    return {
        "ma_buoc": "B.DH.KETLUAN",
        "khai_bao": ["dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu"],
        "cac_dong": [
            {"dong": 0, "latex": db, "loai": "DONG_BIEN"},
            {"dong": 1, "latex": nb, "loai": "NGHICH_BIEN"},
            {"dong": 2, "latex": cd, "loai": "CUC_DAI"},
            {"dong": 3, "latex": ct, "loai": "CUC_TIEU"},
        ],
    }


TH = "x**3 - 3*x"


def test_moc_thua_khong_nhac_buoc_nghiem():
    dau = {0: "+", 1: "0", 2: "-", 3: "-", 4: "-", 5: "0", 6: "+"}
    mui = {0: "TANG", 2: "GIAM", 4: "GIAM", 6: "TANG"}
    g = _grade(TH, _dau("\\mathbb{R}", "3x^2-3", ["1", "-1"]) + [_xd(["-1", "0", "1"], dau, mui)], "B.DH.XETDAU")
    assert g["ket_qua"] == "SAI"
    assert g["loai_ket_qua"] == "DIEM_THUA"
    assert "nghiệm" not in (g["thong_bao"] or "").lower()
    assert "mốc" in g["thong_bao"]


def test_thieu_nghiem_bang_theo_moc_thieu_co_he_qua_doi_dau():
    g = _grade(
        TH,
        _dau("\\mathbb{R}", "3x^2-3", ["1"]) + [_xd(["1"], {0: "-", 1: "0", 2: "+"}, {0: "GIAM", 2: "TANG"})],
        "B.DH.XETDAU",
    )
    assert g["ket_qua"] == "SAI"
    goc = g["cac_van_de"][0]
    assert goc["loai_ket_qua"] == "DIEM_THIEU" and goc["buoc_sai"]["ma_buoc"] == "B.DH.NGHIEM"
    he_qua = [v for v in g["cac_van_de"] if v["loai_ket_qua"] == "DAU_DOI_TRONG_KHOANG"]
    assert he_qua, g["cac_van_de"]
    for v in he_qua:
        assert v["nguyen_nhan"] == goc["id"]
        assert v["buoc_sai"]["o"]["hang"] == "DAU_YPHAY" and v["buoc_sai"]["o"]["k"] % 2 == 0
    # khoảng (−∞; 1) chứa −1 nơi y′ đổi dấu → ô k=0
    assert any(v["buoc_sai"]["o"]["k"] == 0 for v in he_qua)


def test_ket_luan_sai_hai_o_du_hai_van_de():
    bai = _dau("\\mathbb{R}", "2x", ["0"]) + [_xd(["0"], {0: "-", 1: "0", 2: "+"}, {0: "GIAM", 2: "TANG"})]
    g = _grade("x**2", bai + [_kl("(0;+∞)", "(-∞;0)", "x = 0, y = 0", "không có")], "B.DH.KETLUAN")
    assert g["ket_qua"] == "SAI"
    kl = [v for v in g["cac_van_de"] if v["buoc_sai"] and v["buoc_sai"]["ma_buoc"] == "B.DH.KETLUAN"]
    dong = [v["buoc_sai"]["dong"] for v in kl]
    assert dong[:2] == [2, 3], g["cac_van_de"]
    assert g["buoc_sai"]["dong"] == 2


def test_ket_luan_sai_mot_o_chi_mot_van_de():
    bai = _dau("\\mathbb{R}", "2x", ["0"]) + [_xd(["0"], {0: "-", 1: "0", 2: "+"}, {0: "GIAM", 2: "TANG"})]
    g = _grade("x**2", bai + [_kl("(0;+∞)", "(-∞;0)", "x = 0, y = 0", "x = 0, y = 0")], "B.DH.KETLUAN")
    assert g["ket_qua"] == "SAI"
    kl = [v for v in g["cac_van_de"] if v["buoc_sai"] and v["buoc_sai"]["ma_buoc"] == "B.DH.KETLUAN"]
    assert [v["buoc_sai"]["dong"] for v in kl] == [2]
