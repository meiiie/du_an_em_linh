# -*- coding: utf-8 -*-
"""F-01 phía dịch vụ: không eval, container không root, giới hạn tài nguyên/thời gian/đồng thời của sandbox,
môi trường tiến trình con không có bí mật, cổng danh sách trắng của bộ chấm."""
import os
import re
import subprocess
import sys
import threading

import pytest
from fastapi.testclient import TestClient

from app import sandbox
from app.dau_vao import ly_do_tu_choi
from app.grader import grade

MATH = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
REPO = os.path.abspath(os.path.join(MATH, "..", ".."))


def _ma_khong_chu_thich(p):
    out = []
    for dong in open(p, encoding="utf-8"):
        out.append(dong.split("#")[0])
    return "\n".join(out)


@pytest.mark.parametrize("rel", ["app", "kiemdinh/tang1/kiem_tang1.py", "kiemdinh/loc-lo-dap-an/loc.py"])
def test_khong_eval_exec(rel):
    p = os.path.join(MATH, rel)
    tep = [os.path.join(p, f) for f in os.listdir(p) if f.endswith(".py")] if os.path.isdir(p) else [p]
    for f in tep:
        ma = _ma_khong_chu_thich(f)
        assert not re.search(r"(?<![\w.])(eval|exec)\(", ma), f
        # parse_expr chỉ được gọi qua bộ phân tích an toàn (tên gốc đổi thành _parse_expr_goc) hoặc verify.py (có kiểm cây)
        if not f.endswith("verify.py"):
            assert not re.search(r"(?<![\w.])parse_expr\(", ma), f


@pytest.mark.parametrize("rel", ["Dockerfile", "services/math/Dockerfile"])
def test_container_khong_root(rel):
    dong = [d.strip() for d in open(os.path.join(REPO, rel), encoding="utf-8") if d.strip() and not d.strip().startswith("#")]
    users = [i for i, d in enumerate(dong) if d.upper().startswith("USER ")]
    cmd = [i for i, d in enumerate(dong) if d.upper().startswith("CMD")]
    assert users and cmd, rel
    assert users[-1] < cmd[-1], rel
    ten = dong[users[-1]].split()[1]
    assert ten not in ("root", "0", "0:0"), rel


def test_env_tien_trinh_con_khong_co_bi_mat(monkeypatch):
    for k in ("DATABASE_URL", "OPENAI_API_KEY", "APP_ENC_KEY", "AUTH_SECRET", "HOC_TOAN_CHO_KIEM_DEM"):
        monkeypatch.setenv(k, "bi-mat")
    env = sandbox._env_toi_thieu()
    assert set(env) <= set(sandbox._ENV_GIU) | {"PYTHONDONTWRITEBYTECODE", "OMP_NUM_THREADS"}
    assert "bi-mat" not in env.values()


def test_gioi_han_tai_nguyen_ap_vao_tien_trinh_con():
    ma = "import resource as r;print(r.getrlimit(r.RLIMIT_AS)[0], r.getrlimit(r.RLIMIT_CPU)[0], r.getrlimit(r.RLIMIT_FSIZE)[0])"
    out = subprocess.run([sys.executable, "-c", ma], capture_output=True, text=True, preexec_fn=sandbox._gioi_han(5), timeout=20)
    mem, cpu, fsize = (int(v) for v in out.stdout.split())
    assert mem == sandbox.BO_NHO_TOI_DA
    assert cpu == 6
    assert fsize == 0  # tiến trình con không ghi được file


def test_tien_trinh_con_khong_ghi_duoc_file(tmp_path):
    f = tmp_path / "co.txt"
    ma = "open(%r, 'w').write('1' * 10)" % str(f)
    out = subprocess.run([sys.executable, "-c", ma], capture_output=True, text=True, preexec_fn=sandbox._gioi_han(5), timeout=20)
    # RLIMIT_FSIZE = 0: không ghi được byte nào (tạo file rỗng vẫn được; chặn thực thi là việc của bộ phân tích an toàn)
    assert "File too large" in out.stderr
    assert not f.exists() or f.stat().st_size == 0


def test_het_bo_nho_thi_khong_kiem_duoc(monkeypatch):
    monkeypatch.setattr(sandbox, "BO_NHO_TOI_DA", 64 * 1024 * 1024)
    r = sandbox.run_sympy_job("grade", {"ham": "x**2", "cac_buoc": []}, timeout=10)
    # 64 MB không đủ nạp SymPy: job phải kết thúc gọn là KHONG_KIEM_DUOC, không treo, không làm sập API
    assert r.get("ket_qua") == "KHONG_KIEM_DUOC"


def test_tran_timeout_20s():
    assert sandbox.TIMEOUT_TOI_DA == 20
    from app.main import app
    c = TestClient(app)
    assert c.post("/v1/grade", json={"timeout_s": 999}).status_code == 422


def test_qua_so_job_dong_thoi_thi_tu_choi(monkeypatch):
    monkeypatch.setattr(sandbox, "_CHO", threading.BoundedSemaphore(1))
    sandbox._CHO.acquire()
    try:
        r = sandbox.run_sympy_job("spin", {"giay": 0}, timeout=1)
    finally:
        sandbox._CHO.release()
    assert r["ket_qua"] == "KHONG_KIEM_DUOC" and r["cho_phep"] is False


@pytest.mark.parametrize("payload", [{}, {"ham": "x**2"}, {"ham": "x**2", "cac_buoc": "abc"}])
def test_payload_sai_khuon_khong_kiem_duoc(payload):
    r = grade(payload)
    assert r["ket_qua"] == "KHONG_KIEM_DUOC" and r["ly_do"] == "DAU_VAO_KHONG_HOP_LE"


@pytest.mark.parametrize("s", ["x^{2} - 3x + 2", "\\frac{1}{x-1}", "(-\\infty; 0) \\cup (2; +\\infty)", "x = 1; x = 3",
                               "cực đại tại x = 0, y_{CĐ} = 2", "Hàm số đồng biến trên (0;2)", "y' = 3x^2 - 6x",
                               "y' không xác định tại x = 1", "\\mathbb{R} \\setminus \\{1\\}", "Hàm số không có cực trị",
                               "dong bien tren khoang (0; 2)", "x_{1} = 0", "1 \\pm \\sqrt{2}", "TANG"])
def test_cong_nhan_bai_lam_binh_thuong(s):
    assert ly_do_tu_choi(s) is None, s


@pytest.mark.parametrize("s", ["x.subs(x, 1)", "open('/tmp/a')", "x; import os", "\"x\"", "x_1 + 1", "9**9**9", "10^10^10",
                               "lambda: 1", "factorial(10**8)", "Symbol('y')", "x + 1" * 100, "().__class__"])
def test_cong_tu_choi_chuoi_la(s):
    assert ly_do_tu_choi(s) is not None, s
