# -*- coding: utf-8 -*-
"""Chạy bộ kiểm Tầng 1 trên toàn bộ bộ đề, xuất ket-qua/ket-qua-tang1.json và ket-qua/tom-tat-tang1.md."""
import json, time, hashlib, signal, sys, platform, traceback, datetime, os
from collections import OrderedDict, defaultdict
import yaml

T0 = time.perf_counter()
import sympy
sys.path.insert(0, os.path.dirname(__file__))
import kiem_tang1 as K
T_IMPORT = time.perf_counter() - T0

GOC = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
BO_DE = os.path.join(GOC, 'bo-de-kiem-thu', 'cac-ca.yaml')
RA = os.path.join(GOC, 'ket-qua')
TIMEOUT_S = 30


class HetGio(Exception):
    pass


def _alarm(signum, frame):
    raise HetGio()


def main():
    raw = open(BO_DE, 'rb').read()
    sha = hashlib.sha256(raw).hexdigest()
    bo = yaml.safe_load(raw.decode('utf-8'))
    signal.signal(signal.SIGALRM, _alarm)
    tz = datetime.timezone(datetime.timedelta(hours=7))
    bat_dau = datetime.datetime.now(tz).isoformat(timespec='seconds')
    ds = []
    t_all = time.perf_counter()
    for ca in bo['ca']:
        t = time.perf_counter()
        signal.alarm(TIMEOUT_S)
        try:
            r = K.kiem(ca['kiem'])
        except HetGio:
            r = K.ket_qua('KHONG_KIEM_DUOC', 'qua_thoi_gian', 'Quá %ds' % TIMEOUT_S)
        except Exception as ex:
            r = K.ket_qua('KHONG_KIEM_DUOC', 'loi_cong_cu', 'Ngoại lệ: %s: %s' % (type(ex).__name__, ex))
            r['traceback'] = traceback.format_exc(limit=3)
        finally:
            signal.alarm(0)
        ms = round((time.perf_counter() - t) * 1000, 1)
        out = OrderedDict(
            id=ca['id'], nhom_loi=ca['nhom_loi'], nhan=ca['nhan'], buoc_sai_nhan=ca.get('buoc_sai_nhan'),
            tang=1, trang_thai=r['trang_thai'], loai_ket_qua=K.loai_ket_qua(r, ca['kiem']['kieu']),
            # thông tin CHẮC CHẮN (máy): trạng thái, bước sai, loại kiểm, phản chứng
            buoc_sai=r['buoc_sai'] if r['trang_thai'] == 'SAI' else None,
            loai_kiem=r['loai_kiem'], chi_tiet=r['chi_tiet'], phan_chung=r['phan_chung'], bang_chung=r['bang_chung'],
            canh_bao=r['canh_bao'],
            # thông tin SUY ĐOÁN (phân loại mã lỗi) – để null vì chưa có bảng mã của Sư phạm
            ma_loi=None, do_tin_cay_ma_loi=None,
            thoi_gian_ms=ms)
        if 'traceback' in r:
            out['traceback'] = r['traceback']
        # kết cục đúng/sai của bộ kiểm so với nhãn
        if ca['nhan'] == 'sai':
            out['ket_cuc'] = {'SAI': 'bat_duoc', 'DAT': 'bo_lot', 'KHONG_KIEM_DUOC': 'khong_kiem_duoc'}[r['trang_thai']]
        else:
            out['ket_cuc'] = {'SAI': 'bao_nham', 'DAT': 'dat_dung', 'KHONG_KIEM_DUOC': 'khong_kiem_duoc'}[r['trang_thai']]
        # trạng thái nội dung nếu áp bảng quyết định với Tầng 2/3 hiện = KHÔNG KIỂM ĐƯỢC (chưa có tài liệu/bảng)
        out['trang_thai_noi_dung_mo_phong'] = 'BI_CHAN' if r['trang_thai'] == 'SAI' else 'CHO_GIAO_VIEN_DUYET'
        ds.append(out)
    t_tong = time.perf_counter() - t_all

    loi = [d for d in ds if d['nhan'] == 'sai']
    dung = [d for d in ds if d['nhan'] == 'dung']
    def dem(lst, kc):
        return sum(1 for d in lst if d['ket_cuc'] == kc)
    tong_hop = OrderedDict(
        so_ca=len(ds), so_ca_loi=len(loi), so_ca_dung=len(dung),
        loi_bat_duoc=dem(loi, 'bat_duoc'), loi_bo_lot_DAT=dem(loi, 'bo_lot'), loi_khong_kiem_duoc=dem(loi, 'khong_kiem_duoc'),
        recall=round(dem(loi, 'bat_duoc') / len(loi), 4),
        ti_le_loi_bi_cham_DAT=round(dem(loi, 'bo_lot') / len(loi), 4),
        dung_dat=dem(dung, 'dat_dung'), dung_bao_nham=dem(dung, 'bao_nham'), dung_khong_kiem_duoc=dem(dung, 'khong_kiem_duoc'),
        ti_le_bao_nham=round(dem(dung, 'bao_nham') / len(dung), 4),
        ti_le_khong_kiem_duoc_tren_tat_ca=round(sum(1 for d in ds if d['trang_thai'] == 'KHONG_KIEM_DUOC') / len(ds), 4),
    )
    # chỉ số trên các ca hình thức hóa được (bỏ K01, K02, KC1)
    hh = [d for d in ds if d['nhom_loi'] != 'loi_van_khong_kiem_duoc' and d['id'] not in ('KC1',)]
    loi_hh = [d for d in hh if d['nhan'] == 'sai']
    tong_hop['recall_tren_ca_hinh_thuc_hoa_duoc'] = OrderedDict(so_ca_loi=len(loi_hh), bat_duoc=dem(loi_hh, 'bat_duoc'),
                                                              recall=round(dem(loi_hh, 'bat_duoc') / len(loi_hh), 4))
    theo_nhom = OrderedDict()
    for d in ds:
        g = theo_nhom.setdefault(d['nhom_loi'], OrderedDict(so_ca=0, bat_duoc=0, bo_lot=0, khong_kiem_duoc=0, dat_dung=0, bao_nham=0))
        g['so_ca'] += 1
        g[d['ket_cuc']] += 1
    # định vị bước sai trên ca nhiều dòng có nhãn bước
    buoc = [d for d in ds if d['buoc_sai_nhan'] is not None]
    dinh_vi = OrderedDict(so_ca=len(buoc), dung_buoc=sum(1 for d in buoc if d['buoc_sai'] == d['buoc_sai_nhan']),
                          chi_tiet=[dict(id=d['id'], nhan=d['buoc_sai_nhan'], may=d['buoc_sai'], trang_thai=d['trang_thai']) for d in buoc])
    ket = OrderedDict(
        truy_vet=OrderedDict(
            bo_de=os.path.relpath(BO_DE, GOC), sha256_bo_de=sha, phien_ban_bo_de=bo.get('phien_ban'),
            phien_ban_sympy=sympy.__version__, phien_ban_python=platform.python_version(),
            phien_ban_bo_kiem='tang1-thu-nghiem-0.1', phien_ban_bang_cong_thuc=None, tai_lieu_da_dung=[],
            thoi_diem_kiem=bat_dau, timeout_moi_ca_s=TIMEOUT_S),
        thoi_gian=OrderedDict(nap_sympy_va_bo_kiem_s=round(T_IMPORT, 2), chay_toan_bo_s=round(t_tong, 2),
                              trung_binh_ms_moi_ca=round(t_tong * 1000 / len(ds), 1),
                              cham_nhat=sorted(((d['thoi_gian_ms'], d['id']) for d in ds), reverse=True)[:5]),
        tong_hop=tong_hop, theo_nhom=theo_nhom, dinh_vi_buoc_sai=dinh_vi,
        ca_loi_khong_bat_duoc=[dict(id=d['id'], nhom_loi=d['nhom_loi'], trang_thai=d['trang_thai'], chi_tiet=d['chi_tiet']) for d in loi if d['ket_cuc'] != 'bat_duoc'],
        ca_dung_khong_dat=[dict(id=d['id'], trang_thai=d['trang_thai'], chi_tiet=d['chi_tiet']) for d in dung if d['ket_cuc'] != 'dat_dung'],
        ket_qua_tung_ca=ds)
    os.makedirs(RA, exist_ok=True)
    with open(os.path.join(RA, 'ket-qua-tang1.json'), 'w', encoding='utf-8') as fo:
        json.dump(ket, fo, ensure_ascii=False, indent=1, default=str)
    # bảng tóm tắt
    L = []
    th = tong_hop
    L.append('# Tóm tắt kết quả Tầng 1 (sinh tự động từ ket-qua-tang1.json)\n')
    L.append('- Thời điểm chạy: %s; SymPy %s; Python %s; bộ đề sha256 `%s`' % (bat_dau, sympy.__version__, platform.python_version(), sha))
    L.append('- Thời gian: nạp SymPy + bộ kiểm %.2f s; chạy %d ca %.2f s (trung bình %.1f ms/ca)\n' % (T_IMPORT, len(ds), t_tong, t_tong * 1000 / len(ds)))
    L.append('| Chỉ số | Giá trị |\n|---|---|')
    L.append('| Tổng số ca | %d (%d có lỗi, %d đúng) |' % (th['so_ca'], th['so_ca_loi'], th['so_ca_dung']))
    L.append('| Lỗi bắt được (SAI) | %d/%d → recall = %.1f%% |' % (th['loi_bat_duoc'], th['so_ca_loi'], 100 * th['recall']))
    L.append('| Lỗi bị chấm ĐẠT (lọt) | %d |' % th['loi_bo_lot_DAT'])
    L.append('| Lỗi → KHÔNG KIỂM ĐƯỢC | %d |' % th['loi_khong_kiem_duoc'])
    L.append('| Recall trên ca hình thức hóa được | %d/%d = %.1f%% |' % (th['recall_tren_ca_hinh_thuc_hoa_duoc']['bat_duoc'], th['recall_tren_ca_hinh_thuc_hoa_duoc']['so_ca_loi'], 100 * th['recall_tren_ca_hinh_thuc_hoa_duoc']['recall']))
    L.append('| Ca đúng bị báo SAI (báo nhầm) | %d/%d = %.1f%% |' % (th['dung_bao_nham'], th['so_ca_dung'], 100 * th['ti_le_bao_nham']))
    L.append('| Ca đúng → KHÔNG KIỂM ĐƯỢC | %d |' % th['dung_khong_kiem_duoc'])
    L.append('| Tỉ lệ KHÔNG KIỂM ĐƯỢC (mọi ca) | %.1f%% |' % (100 * th['ti_le_khong_kiem_duoc_tren_tat_ca']))
    L.append('| Định vị bước sai (ca nhiều dòng có nhãn) | %d/%d đúng dòng |\n' % (dinh_vi['dung_buoc'], dinh_vi['so_ca']))
    L.append('## Theo nhóm lỗi\n\n| Nhóm | Số ca | Bắt được | Lọt (ĐẠT) | Không kiểm được | Đúng→ĐẠT | Báo nhầm |\n|---|---|---|---|---|---|---|')
    for g, v in theo_nhom.items():
        L.append('| %s | %d | %d | %d | %d | %d | %d |' % (g, v['so_ca'], v['bat_duoc'], v['bo_lot'], v['khong_kiem_duoc'], v['dat_dung'], v['bao_nham']))
    L.append('\n## Ca lỗi không bắt được\n')
    for d in ket['ca_loi_khong_bat_duoc']:
        L.append('- %s (%s): %s – %s' % (d['id'], d['nhom_loi'], d['trang_thai'], d['chi_tiet']))
    L.append('\n## Ca đúng không được ĐẠT\n')
    for d in ket['ca_dung_khong_dat']:
        L.append('- %s: %s – %s' % (d['id'], d['trang_thai'], d['chi_tiet']))
    L.append('\n## Định vị bước sai\n')
    for d in dinh_vi['chi_tiet']:
        L.append('- %s: nhãn dòng %s, máy báo dòng %s (%s)' % (d['id'], d['nhan'], d['may'], d['trang_thai']))
    L.append('\n## Từng ca\n\n| id | nhóm | nhãn | Tầng 1 | bước | loại kiểm | ms |\n|---|---|---|---|---|---|---|')
    for d in ds:
        L.append('| %s | %s | %s | %s | %s | %s | %s |' % (d['id'], d['nhom_loi'], d['nhan'], d['trang_thai'], d['buoc_sai'] or '', d['loai_kiem'], d['thoi_gian_ms']))
    open(os.path.join(RA, 'tom-tat-tang1.md'), 'w', encoding='utf-8').write('\n'.join(L) + '\n')
    print(json.dumps(tong_hop, ensure_ascii=False, indent=1))
    print('dinh_vi', dinh_vi['dung_buoc'], '/', dinh_vi['so_ca'])
    print('t_tong', round(t_tong, 2))


if __name__ == '__main__':
    main()
