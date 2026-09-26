# -*- coding: utf-8 -*-
"""Chạy mỗi job SymPy trong một tiến trình con, có timeout. Tiến trình API không gọi sympify."""
import json
import os
import signal
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def run_sympy_job(kind, payload, timeout=8):
    proc = subprocess.Popen(
        [sys.executable, "-m", "app.job_runner"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        cwd=ROOT,
        start_new_session=True,
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
        return {
            "ket_qua": "KHONG_KIEM_DUOC",
            "loai_ket_qua": "KHONG_KIEM_DUOC",
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Job SymPy lỗi (mã %s)." % proc.returncode,
            "stderr": (err or b"").decode("utf-8", "replace")[-2000:],
        }
    try:
        return json.loads(out.decode("utf-8"))
    except json.JSONDecodeError:
        return {
            "ket_qua": "KHONG_KIEM_DUOC",
            "loai_ket_qua": "KHONG_KIEM_DUOC",
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Job SymPy không trả JSON.",
            "stdout": (out or b"").decode("utf-8", "replace")[-2000:],
        }
