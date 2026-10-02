# -*- coding: utf-8 -*-
"""Chạy mỗi job SymPy trong một tiến trình con, có timeout. Tiến trình API không gọi sympify."""
import json
import logging
import math
import os
import signal
import subprocess
import sys
import time

import threading

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

# F-01: giới hạn tài nguyên cho mỗi job (tiến trình con) + số job đồng thời.
BO_NHO_TOI_DA = int(os.environ.get("MATH_JOB_MEM_MB", "512")) * 1024 * 1024
SO_JOB_DONG_THOI = int(os.environ.get("MATH_JOB_CONCURRENCY", "2"))
TIMEOUT_TOI_DA = 20
CHAY_TOI_THIEU = 0.5  # giây; còn ít hơn thì báo bận, không chạy job
_CHO = threading.BoundedSemaphore(SO_JOB_DONG_THOI)
# Tiến trình con chỉ nhận biến môi trường tối thiểu: không DATABASE_URL, không khoá API.
_ENV_GIU = ("PATH", "LANG", "LC_ALL", "PYTHONPATH", "PYTHONHASHSEED", "HOME", "TMPDIR")


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


def _thoi_gian_don(timeout):
    """Giây để giết và thu tiến trình con sau khi hết giờ: 10 % hạn chót, trong khoảng 0,2–2 s."""
    return min(2.0, max(0.2, 0.1 * timeout))


def run_sympy_job(kind, payload, timeout=8):
    """Một hạn chót tuyệt đối `timeout` giây (#104) cho cả chờ suất, chạy job và dọn tiến trình. Thời gian chờ suất
    trừ vào thời gian chạy; còn dưới CHAY_TOI_THIEU thì trả «máy bận». Nơi gọi đặt timeout_s nhỏ hơn hết giờ của
    mình là đủ để job không giữ suất sau khi nơi gọi thôi chờ."""
    timeout = max(1, min(int(timeout or 8), TIMEOUT_TOI_DA))
    han = time.monotonic() + timeout
    don = _thoi_gian_don(timeout)
    if not _CHO.acquire(timeout=max(0.0, timeout - don - CHAY_TOI_THIEU)):
        return _qua_tai()
    try:
        if han - time.monotonic() - don < CHAY_TOI_THIEU:
            return _qua_tai()
        return _chay(kind, payload, han, don)
    finally:
        _CHO.release()


def _chay(kind, payload, han, don=2.0):
    """Chạy job trong tiến trình con; mọi khoảng chờ tính lại từ hạn chót tuyệt đối `han` (time.monotonic)."""
    ngan_sach = max(CHAY_TOI_THIEU, han - time.monotonic() - don)
    proc = subprocess.Popen(
        [sys.executable, "-m", "app.job_runner"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        cwd=ROOT,
        start_new_session=True,
        env=_env_toi_thieu(),
        preexec_fn=_gioi_han(int(math.ceil(ngan_sach))),
    )
    data = json.dumps({"kind": kind, "payload": payload}, ensure_ascii=False).encode("utf-8")
    try:
        # Tính lại sau Popen: thời gian khởi động tiến trình con cũng trừ vào ngân sách chạy.
        out, err = proc.communicate(data, timeout=max(0.05, han - time.monotonic() - don))
    except subprocess.TimeoutExpired:
        try:
            os.killpg(proc.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        try:
            proc.wait(timeout=max(0.05, min(don, han - time.monotonic())))
        except subprocess.TimeoutExpired:  # pragma: no cover - tiến trình kẹt trong nhân, để hệ điều hành thu sau
            logging.getLogger("math.sandbox").warning("job %s không thu kịp sau khi giết", kind)
        return {
            "ket_qua": "KHONG_KIEM_DUOC",
            "loai_ket_qua": "KHONG_KIEM_DUOC",
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Job SymPy vượt quá %.1fs và đã bị dừng." % ngan_sach,
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
