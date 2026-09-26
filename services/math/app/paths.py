# -*- coding: utf-8 -*-
import importlib.util
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
KIEMDINH = os.path.join(ROOT, "kiemdinh")
TANG1 = os.path.join(KIEMDINH, "tang1")


def load_kiem():
    if TANG1 not in sys.path:
        sys.path.insert(0, TANG1)
    import kiem_tang1
    return kiem_tang1


def load_loc():
    path = os.path.join(KIEMDINH, "loc-lo-dap-an", "loc.py")
    spec = importlib.util.spec_from_file_location("loc_lo_dap_an", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod
