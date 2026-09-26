# -*- coding: utf-8 -*-
"""16 ca bài làm 5 bước: đúng mã bước và ô so với nhãn YAML của Kiểm định."""
import json
import os

import yaml

from app.paths import KIEMDINH, load_kiem

K = load_kiem()
BO = os.path.join(KIEMDINH, "bo-de-kiem-thu", "cac-ca-5-buoc.yaml")


def _khop(nhan, bs):
    if nhan is None:
        return bs is None
    if bs is None:
        return False
    if nhan["ma_buoc"] != bs["ma_buoc"]:
        return False
    if nhan.get("dong") != bs.get("dong"):
        return False
    o1, o2 = nhan.get("o"), bs.get("o")
    if o1 is None and o2 is None:
        return True
    if not o1 or not o2:
        return False
    return o1.get("hang") == o2.get("hang") and o1.get("k") == o2.get("k")


def test_16_ca_dung_vi_tri():
    bo = yaml.safe_load(open(BO, encoding="utf-8"))
    loi = dung = 0
    lech = []
    bao_nham = 0
    for ca in bo["ca"]:
        r = K.kiem_5_buoc(ca["bai_lam"])
        bs = r["buoc_sai"] if r["trang_thai"] == "SAI" else None
        ok = _khop(ca["buoc_sai_nhan"], bs)
        if ca["nhan"] == "sai":
            loi += 1
            if not ok or r["trang_thai"] != "SAI":
                lech.append((ca["id"], r["trang_thai"], bs, ca["buoc_sai_nhan"], r.get("chi_tiet", "")[:160]))
        else:
            dung += 1
            if r["trang_thai"] == "SAI":
                bao_nham += 1
            if not ok:
                lech.append((ca["id"], r["trang_thai"], bs, ca["buoc_sai_nhan"]))
    print("5BUOC", json.dumps({"ca": len(bo["ca"]), "loi": loi, "dung": dung, "lech": len(lech), "bao_nham": bao_nham}, ensure_ascii=False))
    assert lech == []
    assert bao_nham == 0
    assert loi == 12 and dung == 4
