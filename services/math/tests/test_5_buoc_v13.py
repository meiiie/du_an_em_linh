# -*- coding: utf-8 -*-
"""Bộ bổ sung v1.3 (S17–S31): mốc hàng X không tăng dần (SAI_THU_TU_MOC), điểm thừa ERR.DH.24/ERR.DH.31,
nhiều lỗi gốc. So ĐỦ cac_van_de_nhan (thứ tự, ô, nguyen_nhan, ma_loi) và loai_ket_qua_nhan.
Quy ước chỉ số (docs/chi-so-o-bang.md): k và dong đếm từ 0; hàng viết HOA."""
import os

import pytest
import yaml

from app.paths import KIEMDINH, load_kiem

K = load_kiem()
BO = os.path.join(KIEMDINH, "bo-de-kiem-thu", "cac-ca-5-buoc-bo-sung-v1.3.yaml")
CA = yaml.safe_load(open(BO, encoding="utf-8"))["ca"]


def _bs_ra(bs):
    if bs is None:
        return None
    dong = bs.get("dong")
    # bộ kiểm nội bộ ghi dong theo số bước (1,2,3,5) khi bước chỉ có một dòng logic -> dòng 0
    if dong in (1, 2, 3, 5):
        dong = 0
    return {"ma_buoc": bs["ma_buoc"], "dong": dong, "o": K.o_ra_ngoai(bs.get("o"))}


def _van_de_ra(r):
    ds = r.get("cac_van_de") or ([{"id": "VD1", "loai_ket_qua": K.loai_ket_qua(r)[0], "buoc_sai": r["buoc_sai"]}] if r["trang_thai"] == "SAI" else [])
    loai_theo_id = {v["id"]: v["loai_ket_qua"] for v in ds}
    ra = []
    for v in ds:
        d = {"loai_ket_qua": v["loai_ket_qua"], "buoc_sai": _bs_ra(v["buoc_sai"])}
        if v.get("nguyen_nhan"):
            d["nguyen_nhan"] = loai_theo_id[v["nguyen_nhan"]]
        for t in ("so_diem_thieu", "ma_loi", "ky_nang"):
            if t in v:
                d[t] = v[t]
        ra.append(d)
    return ra


def _nhan(v):
    d = {"loai_ket_qua": v["loai_ket_qua"], "buoc_sai": v["buoc_sai"]}
    for t in ("nguyen_nhan", "so_diem_thieu", "ma_loi", "ky_nang"):
        if t in v:
            d[t] = v[t]
    return d


@pytest.mark.parametrize("ca", CA, ids=[c["id"] for c in CA])
def test_v13(ca):
    r = K.kiem_5_buoc(ca["bai_lam"])
    assert K.loai_ket_qua(r) == ca["loai_ket_qua_nhan"]
    ra = _van_de_ra(r)
    nhan = [_nhan(v) for v in ca["cac_van_de_nhan"]]
    # chỉ so các khoá nhãn có ghi (ma_loi/ky_nang/so_diem_thieu chỉ ghi khi đã chốt)
    ra = [{k: v for k, v in a.items() if k in b or k in ("loai_ket_qua", "buoc_sai", "nguyen_nhan")} for a, b in zip(ra, nhan)] + ra[len(nhan):]
    assert ra == nhan
    if ca["buoc_sai_nhan"] is None:
        assert r["trang_thai"] != "SAI"
    else:
        assert _bs_ra(r["buoc_sai"]) == ca["buoc_sai_nhan"]
