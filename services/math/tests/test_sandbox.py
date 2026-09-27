# -*- coding: utf-8 -*-
import json
import subprocess
import sys

from app.sandbox import run_sympy_job


def test_api_khong_import_sympy():
    code = "import app.main, sys; raise SystemExit(0 if 'sympy' not in sys.modules else 1)"
    r = subprocess.run([sys.executable, "-c", code], cwd=".", capture_output=True, text=True)
    assert r.returncode == 0, r.stderr


def test_timeout_kill():
    r = run_sympy_job("spin", {"giay": 30}, timeout=1)
    assert r["loai_ket_qua"] == "KHONG_KIEM_DUOC"


def test_grade_qua_tien_trinh():
    r = run_sympy_job("grade", {
        "ham": "x**2",
        "nop_toi": "B.DH.TXD",
        "cac_buoc": [{"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": "R"}]}],
    })
    assert r["ket_qua"] == "DAT"


def test_filter_chan_dap_an():
    r = run_sympy_job("filter", {
        "ban_nhap": "Hàm đồng biến trên khoảng (-∞; 0) và cực đại tại x = 0, giá trị cực đại bằng 2.",
        "su_kien": [
            {"loai": "DB", "gia_tri": "(-oo;0)"},
            {"loai": "DCD", "gia_tri": "0"},
            {"loai": "GTCD", "gia_tri": "2"},
        ],
    })
    assert r["cho_phep"] is False
    assert "khop" not in r


def test_filter_goi_y_an_toan():
    r = run_sympy_job("filter", {
        "ban_nhap": "Em hãy xét dấu của đạo hàm trên từng khoảng.",
        "su_kien": [{"loai": "DB", "gia_tri": "(-oo;0)"}, {"loai": "GTCD", "gia_tri": "2"}],
    })
    assert r["cho_phep"] is True
    assert "khop" not in json.dumps(r)


def test_filter_khong_chan_luy_thua_di_1():
    """M2 chặn nhầm số 1 trong «giảm số mũ đi 1» khi đề có nghiệm / cực tại 1."""
    su_kien = [
        {"loai": "DB", "gia_tri": "(-oo;1)"},
        {"loai": "NB", "gia_tri": "(1;3)"},
        {"loai": "DCD", "gia_tri": "1"},
        {"loai": "DCT", "gia_tri": "3"},
        {"loai": "GTCD", "gia_tri": "6"},
        {"loai": "GTCT", "gia_tri": "2"},
        {"loai": "NGHIEM", "gia_tri": "1"},
        {"loai": "NGHIEM", "gia_tri": "3"},
    ]
    an = run_sympy_job("filter", {
        "ban_nhap": "Nguyên lý lũy thừa: (xⁿ)′ = n·xⁿ⁻¹, nghĩa là nhân hệ số với số mũ rồi giảm số mũ đi 1.",
        "su_kien": su_kien,
    })
    assert an["cho_phep"] is True
    lo = run_sympy_job("filter", {
        "ban_nhap": "Cực đại tại x = 1, cực tiểu tại x = 3, đồng biến trên (3;+∞).",
        "su_kien": su_kien,
    })
    assert lo["cho_phep"] is False
