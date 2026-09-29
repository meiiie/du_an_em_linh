# -*- coding: utf-8 -*-
"""Chạy mỗi job SymPy trong một tiến trình con, có timeout. Tiến trình API không gọi sympify."""
import json
import logging
import os
import signal
import subprocess
import sys

import threading

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

# F-01: giới hạn tài nguyên cho mỗi job (tiến trình con) + số job đồng thời.
BO_NHO_TOI_DA = int(os.environ.get("MATH_JOB_MEM_MB", "512")) * 1024 * 1024
SO_JOB_DONG_THOI = int(os.environ.get("MATH_JOB_CONCURRENCY", "2"))
TIMEOUT_TOI_DA = 20
_CHO = threading.BoundedSemaphore(SO_JOB_DONG_THOI)
# Tiến trình con chỉ nhận biến môi trường tối thiểu: không DATABASE_URL, không khoá API.
_ENV_GIU = ("PATH", "LANG", "LC_ALL", "PYTHONPATH", "PYTHONHASHSEED", "HOME", "TMPDIR", "HOC_TOAN_CHO_KIEM_DEM")


def _env_toi_thieu():
    env = {k: os.environ[k] for k in _ENV_GIU if k in os.environ}
    env["PYTHONDONTWRITEBYTECODE"] = "1"
    env["OMP_NUM_THREADS"] = "1"
    return env


def _gioi_han(timeout):
    def ap():
        try:
            import resource
            resource.setrlimit(resource.RLIMIT_AS, (BO_NHO_TOI_DA, BO_NHO_TOI_DA))
            resource.setrlimit(resource.RLIMIT_CPU, (timeout + 1, timeout + 2))
            resource.setrlimit(resource.RLIMIT_FSIZE, (0, 0))
            resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
        except Exception:  # pragma: no cover - nền tảng không hỗ trợ
            pass
    return ap


def _qua_tai():
    return {
        "ket_qua": "KHONG_KIEM_DUOC",
        "loai_ket_qua": "KHONG_KIEM_DUOC",
        "trang_thai": "KHONG_KIEM_DUOC",
        "ly_do": "Máy đang bận, thử lại sau ít giây.",
        "cho_phep": False,
    }


def run_sympy_job(kind, payload, timeout=8):
    timeout = max(1, min(int(timeout or 8), TIMEOUT_TOI_DA))
    if not _CHO.acquire(timeout=timeout):
        return _qua_tai()
    try:
        return _chay(kind, payload, timeout)
    finally:
        _CHO.release()


def _chay(kind, payload, timeout):
    proc = subprocess.Popen(
        [sys.executable, "-m", "app.job_runner"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        cwd=ROOT,
        start_new_session=True,
        env=_env_toi_thieu(),
        preexec_fn=_gioi_han(timeout),
    )
    data = json.dumps({"kind": kind, "payload": payload}, ensure_ascii=False).encode("utf-8")
    try:
        out, err = proc.communicate(data, timeout=timeout)
    except subprocess.TimeoutExpired:
        try:
            os.killpg(proc.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        proc.wait(timeout=2)
        return {
            "ket_qua": "KHONG_KIEM_DUOC",
            "loai_ket_qua": "KHONG_KIEM_DUOC",
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Job SymPy vượt quá %ss và đã bị dừng." % timeout,
            "cho_phep": False,
        }
    if proc.returncode != 0:
        logging.getLogger("math.sandbox").warning(
            "job %s lỗi mã %s: %s", kind, proc.returncode, (err or b"").decode("utf-8", "replace")[-800:]
        )
        return {
            "ket_qua": "KHONG_KIEM_DUOC",
            "loai_ket_qua": "KHONG_KIEM_DUOC",
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Job SymPy lỗi (mã %s)." % proc.returncode,
            "cho_phep": False,
        }
    try:
        return json.loads(out.decode("utf-8"))
    except json.JSONDecodeError:
        return {
            "ket_qua": "KHONG_KIEM_DUOC",
            "loai_ket_qua": "KHONG_KIEM_DUOC",
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Job SymPy không trả JSON.",
            "cho_phep": False,
        }
