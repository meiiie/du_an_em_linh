# -*- coding: utf-8 -*-
"""Bộ tin nhắn lộ đáp án: so cờ chặn M1/M2/M3 với ket-qua-loc-lo-dap-an.json đã đo."""
import json
import os

import yaml

from app.paths import KIEMDINH, load_loc

L = load_loc()
BO = os.path.join(KIEMDINH, "loc-lo-dap-an", "bo-tin-nhan.yaml")
DA_DO = os.path.join(KIEMDINH, "ket-qua", "ket-qua-loc-lo-dap-an.json")
COT = {
    "M1_chuoi_latex": lambda t, b: L.m1(t, b),
    "M2_trich_xuat_sympy_gia_tri": lambda t, b: L.m23(t, b, False),
    "M3_trich_xuat_sympy_ngu_canh": lambda t, b: L.m23(t, b, True),
}


def test_khop_bo_tin_nhan():
    bo = yaml.safe_load(open(BO, encoding="utf-8"))
    da_do = json.load(open(DA_DO, encoding="utf-8"))
    mong = {d["id"]: d for d in da_do["tung_tin_nhan"]}
    assert len(bo["tin_nhan"]) == len(mong) == 70
    lech = []
    for tn in bo["tin_nhan"]:
        bai = bo["bai"][tn["bai"]]
        cu = mong[tn["id"]]
        for ten, fn in COT.items():
            hits = fn(tn["text"], bai)
            chan = bool(hits)
            if chan != cu[ten]["chan"]:
                lech.append((tn["id"], ten, chan, cu[ten]["chan"]))
    print("LEAK lech", len(lech))
    assert lech == []
    # In lại số đã đo — không bịa thêm.
    for ten, block in da_do["tong_hop"].items():
        print(ten, "recall_tat_ca", block["recall_tat_ca_ca_lo"][0], "chan_nham", block["chan_nham"][0])
