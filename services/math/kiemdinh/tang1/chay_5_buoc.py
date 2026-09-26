# -*- coding: utf-8 -*-
"""Chạy thử nghiệm định vị bước sai trên bài làm 5 bước, xuất ket-qua/ket-qua-5-buoc.json."""
import json, time, hashlib, os, sys, datetime, platform, signal
from collections import OrderedDict
import yaml, sympy
sys.path.insert(0, os.path.dirname(__file__))
import kiem_tang1 as K
GOC = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
BO = os.path.join(GOC, 'bo-de-kiem-thu', 'cac-ca-5-buoc.yaml')


def _al(s, f):
    raise TimeoutError()


def main():
    raw = open(BO, 'rb').read()
    bo = yaml.safe_load(raw.decode())
    signal.signal(signal.SIGALRM, _al)
    ds = []
    t0 = time.perf_counter()
    for ca in bo['ca']:
        t = time.perf_counter()
        signal.alarm(30)
        try:
            r = K.kiem_5_buoc(ca['bai_lam'])
        except Exception as ex:
            r = K.ket_qua('KHONG_KIEM_DUOC', 'loi_cong_cu', '%s: %s' % (type(ex).__name__, ex))
        finally:
            signal.alarm(0)
        nh = ca['buoc_sai_nhan']
        bs = r['buoc_sai'] if r['trang_thai'] == 'SAI' else None
        dung_ma = (nh is None and bs is None) or (nh is not None and bs is not None and nh['ma_buoc'] == bs['ma_buoc'])
        dung_vi_tri = dung_ma and (nh is None or (nh.get('dong') == bs.get('dong') and
                                                  ((nh.get('o') is None and bs.get('o') is None) or
                                                   (nh.get('o') and bs.get('o') and nh['o'].get('k') == bs['o'].get('k') and nh['o']['hang'] == bs['o']['hang']))))
        ds.append(OrderedDict(id=ca['id'], mo_ta=ca['mo_ta'], nhan=ca['nhan'], buoc_sai_nhan=nh, trang_thai=r['trang_thai'],
                              buoc_sai=bs, loai_ket_qua=K.loai_ket_qua(r), cac_van_de=r.get('cac_van_de'), loai_kiem=r['loai_kiem'], chi_tiet=r['chi_tiet'], phan_chung=r['phan_chung'],
                              ma_loi=None, do_tin_cay_ma_loi=None,
                              dung_ma_buoc=dung_ma, dung_dong_hoac_o=dung_vi_tri, thoi_gian_ms=round((time.perf_counter() - t) * 1000, 1)))
    tt = time.perf_counter() - t0
    loi = [d for d in ds if d['nhan'] == 'sai']
    dung = [d for d in ds if d['nhan'] == 'dung']
    th = OrderedDict(so_ca=len(ds), so_ca_loi=len(loi), so_ca_dung=len(dung),
                     loi_bat_duoc=sum(d['trang_thai'] == 'SAI' for d in loi),
                     dung_ma_buoc_tren_ca_loi=sum(d['dung_ma_buoc'] for d in loi),
                     dung_ca_ma_buoc_va_dong_o_tren_ca_loi=sum(d['dung_dong_hoac_o'] for d in loi),
                     ca_dung_bao_nham=sum(d['trang_thai'] == 'SAI' for d in dung),
                     khong_kiem_duoc=sum(d['trang_thai'] == 'KHONG_KIEM_DUOC' for d in ds),
                     thoi_gian_s=round(tt, 2))
    out = OrderedDict(truy_vet=OrderedDict(bo_de=os.path.relpath(BO, GOC), sha256_bo_de=hashlib.sha256(raw).hexdigest(),
                                           phien_ban_sympy=sympy.__version__, phien_ban_python=platform.python_version(),
                                           phien_ban_bang_cong_thuc=None, tai_lieu_da_dung=[],
                                           thoi_diem_kiem=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=7))).isoformat(timespec='seconds')),
                      tong_hop=th, ket_qua_tung_ca=ds)
    json.dump(out, open(os.path.join(GOC, 'ket-qua', 'ket-qua-5-buoc.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1, default=str)
    print(json.dumps(th, ensure_ascii=False, indent=1))
    for d in ds:
        print(d['id'], d['nhan'], d['trang_thai'], json.dumps(d['buoc_sai'], ensure_ascii=False, default=str), 'nhan=', json.dumps(d['buoc_sai_nhan'], ensure_ascii=False), d['dung_dong_hoac_o'], '|', d['chi_tiet'][:140])


if __name__ == '__main__':
    main()
