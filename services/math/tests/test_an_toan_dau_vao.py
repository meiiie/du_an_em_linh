# -*- coding: utf-8 -*-
"""F-01: chuỗi HS không bao giờ được thực thi như Python. Payload độc phải bị TỪ CHỐI (KHONG_KIEM_DUOC), không chấm."""
import os

import pytest

from app.grader import grade
from app.normalizer import normalize_expr
from app.paths import load_kiem
from app.sandbox import run_sympy_job

K = load_kiem()

DOC = [
    "().__class__.__name__.__len__()",
    "x.__class__",
    "__import__('os')",
    "[x for x in ()]",
    "2*x*().__class__.__name__.__len__()/5",
    "lambda: 1",
    "getattr(x, 'y')",
    "open('/etc/passwd')",
    "x.func",
    "exec('1')",
    "Symbol('x').subs",
]
# Biểu thức toán "hợp lệ" nhưng cố làm cạn tài nguyên: normalizer có thể để qua, P phải từ chối.
TON = ["9**9**9", "(x+1)**100000"]


@pytest.mark.parametrize("s", DOC)
def test_normalizer_tu_choi(s):
    assert normalize_expr(s) is None


@pytest.mark.parametrize("s", DOC + TON)
def test_P_tu_choi(s):
    with pytest.raises(Exception):
        K.P(s)


@pytest.mark.parametrize("s", ["3*x**2-12*x+9", "((3)/((x+3)**(2)))", "sqrt(x)+Abs(x-1)", "Rational(1,2)*x", "-oo", "x > 1"])
def test_P_van_nhan_bieu_thuc_toan(s):
    K.P(s)


def _payload_dao_ham(dao_ham):
    return {
        "ham": "x**2",
        "nop_toi": "B.DH.DAOHAM",
        "cac_buoc": [
            {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": "\\mathbb{R}"}]},
            {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": dao_ham}]},
        ],
    }


def test_grade_khong_thuc_thi_chuoi_hs():
    # Trước khi sửa: chuỗi này được eval và chấm DAT
    r = grade(_payload_dao_ham("2*x*().__class__.__name__.__len__()/5"))
    assert r["ket_qua"] != "DAT"
    r2 = grade(_payload_dao_ham("2x"))
    assert r2["ket_qua"] == "DAT", r2


def test_frac_long_ngoac_khong_thanh_bien():
    assert normalize_expr("frac3*(x+3)**2") is None
    assert normalize_expr("\\frac{3}{(x+3)^{2}}") is not None


def test_job_ton_bo_nho_bi_dung():
    # Job cố ý tốn tài nguyên bị dừng; tiến trình gọi vẫn sống.
    r = run_sympy_job("spin", {"giay": 30}, timeout=2)
    assert r["ket_qua"] == "KHONG_KIEM_DUOC"


def test_tien_trinh_con_khong_co_bien_bi_mat(monkeypatch):
    monkeypatch.setenv("DATABASE_URL", "postgres://bi-mat")
    from app import sandbox
    env = sandbox._env_toi_thieu()
    assert "DATABASE_URL" not in env
    assert not any("KEY" in k or "TOKEN" in k for k in env)


def test_timeout_bi_chan_tran():
    from app.schemas import JobIn
    with pytest.raises(Exception):
        JobIn(timeout_s=60)
