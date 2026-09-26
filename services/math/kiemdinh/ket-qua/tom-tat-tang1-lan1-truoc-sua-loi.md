# Tóm tắt kết quả Tầng 1 (sinh tự động từ ket-qua-tang1.json)

- Thời điểm chạy: 2026-09-27T00:00:01+07:00; SymPy 1.14.0; Python 3.13.5; bộ đề sha256 `814a329c0f34a00909ccfaf3d39a11ba50c3692f14036ee9d95b6eb79b26cc56`
- Thời gian: nạp SymPy + bộ kiểm 0.20 s; chạy 102 ca 1.90 s (trung bình 18.6 ms/ca)

| Chỉ số | Giá trị |
|---|---|
| Tổng số ca | 102 (69 có lỗi, 33 đúng) |
| Lỗi bắt được (SAI) | 67/69 → recall = 97.1% |
| Lỗi bị chấm ĐẠT (lọt) | 0 |
| Lỗi → KHÔNG KIỂM ĐƯỢC | 2 |
| Recall trên ca hình thức hóa được | 67/67 = 100.0% |
| Ca đúng bị báo SAI (báo nhầm) | 1/33 = 3.0% |
| Ca đúng → KHÔNG KIỂM ĐƯỢC | 1 |
| Tỉ lệ KHÔNG KIỂM ĐƯỢC (mọi ca) | 2.9% |
| Định vị bước sai (ca nhiều dòng có nhãn) | 5/5 đúng dòng |

## Theo nhóm lỗi

| Nhóm | Số ca | Bắt được | Lọt (ĐẠT) | Không kiểm được | Đúng→ĐẠT | Báo nhầm |
|---|---|---|---|---|---|---|
| sai_dau | 5 | 5 | 0 | 0 | 0 | 0 |
| dieu_kien_xac_dinh | 9 | 9 | 0 | 0 | 0 | 0 |
| don_vi | 4 | 4 | 0 | 0 | 0 | 0 |
| bia_cong_thuc | 7 | 7 | 0 | 0 | 0 | 0 |
| mat_thua_nghiem | 4 | 4 | 0 | 0 | 0 | 0 |
| chia_cho_0 | 4 | 4 | 0 | 0 | 0 | 0 |
| luong_giac | 5 | 5 | 0 | 0 | 0 | 0 |
| bpt_nhan_so_am | 5 | 5 | 0 | 0 | 0 | 0 |
| tich_phan_nguyen_ham | 6 | 6 | 0 | 0 | 0 | 0 |
| to_hop_xac_suat | 5 | 5 | 0 | 0 | 0 | 0 |
| dao_ham_don_dieu_cuc_tri | 13 | 13 | 0 | 0 | 0 | 0 |
| loi_van_khong_kiem_duoc | 2 | 0 | 0 | 2 | 0 | 0 |
| doi_chung | 33 | 0 | 0 | 1 | 31 | 1 |

## Ca lỗi không bắt được

- K01 (loi_van_khong_kiem_duoc): KHONG_KIEM_DUOC – Tầng 1 không kiểm được lập luận bằng lời; không được coi là ĐẠT
- K02 (loi_van_khong_kiem_duoc): KHONG_KIEM_DUOC – Tầng 1 không kiểm được ảnh viết tay chưa số hóa; không được coi là ĐẠT

## Ca đúng không được ĐẠT

- DC7: SAI – giá trị cực đại: AI {1}, máy {1, 1}
- KC1: KHONG_KIEM_DUOC – Tầng 1 không kiểm được lập luận bằng lời; không được coi là ĐẠT

## Định vị bước sai

- B01: nhãn dòng 3, máy báo dòng 3 (SAI)
- B02: nhãn dòng 2, máy báo dòng 2 (SAI)
- B03: nhãn dòng 3, máy báo dòng 3 (SAI)
- B04: nhãn dòng 5, máy báo dòng 5 (SAI)
- B05: nhãn dòng 3, máy báo dòng 3 (SAI)

## Từng ca

| id | nhóm | nhãn | Tầng 1 | bước | loại kiểm | ms |
|---|---|---|---|---|---|---|
| L01 | sai_dau | sai | SAI | 1 | khong_tuong_duong | 88.3 |
| L02 | sai_dau | sai | SAI | 1 | dao_ham_sai | 16.1 |
| L03 | sai_dau | sai | SAI | 1 | thua_nghiem_khong_thoa | 1.5 |
| L04 | sai_dau | sai | SAI | 1 | khong_tuong_duong | 8.4 |
| L05 | dieu_kien_xac_dinh | sai | SAI | 1 | thua_nghiem_vi_pham_dkxd | 11.9 |
| L06 | dieu_kien_xac_dinh | sai | SAI | 1 | thua_nghiem_khong_thoa | 2.0 |
| L07 | dieu_kien_xac_dinh | sai | SAI | 1 | khac_tap | 16.5 |
| L08 | dieu_kien_xac_dinh | sai | SAI | 1 | sai_mien_xac_dinh | 12.3 |
| L09 | dieu_kien_xac_dinh | sai | SAI | 1 | thua_nghiem_vi_pham_dkxd | 1.7 |
| L10 | dieu_kien_xac_dinh | sai | SAI | 1 | thua_nghiem_vi_pham_dkxd | 3.0 |
| L11 | dieu_kien_xac_dinh | sai | SAI | 1 | thua_nghiem_vi_pham_dkxd | 4.8 |
| L12 | don_vi | sai | SAI | 1 | sai_gia_tri_doi_don_vi | 12.3 |
| L13 | don_vi | sai | SAI | 1 | sai_gia_tri_doi_don_vi | 7.9 |
| L14 | don_vi | sai | SAI | 1 | sai_thu_nguyen | 0.9 |
| L15 | don_vi | sai | SAI | 1 | sai_gia_tri_doi_don_vi | 4.7 |
| L16 | bia_cong_thuc | sai | SAI | 1 | khong_tuong_duong | 9.5 |
| L17 | bia_cong_thuc | sai | SAI | 1 | khong_tuong_duong | 16.3 |
| L18 | bia_cong_thuc | sai | SAI | 1 | khong_tuong_duong | 34.7 |
| L19 | bia_cong_thuc | sai | SAI | 1 | khong_tuong_duong | 10.3 |
| L20 | bia_cong_thuc | sai | SAI | 1 | dao_ham_sai | 29.6 |
| L21 | bia_cong_thuc | sai | SAI | 1 | khong_tuong_duong | 22.7 |
| L22 | bia_cong_thuc | sai | SAI | 1 | dao_ham_sai | 14.2 |
| L24 | mat_thua_nghiem | sai | SAI | 1 | mat_nghiem | 3.3 |
| L25 | mat_thua_nghiem | sai | SAI | 1 | mat_nghiem | 6.1 |
| L26 | mat_thua_nghiem | sai | SAI | 1 | thua_nghiem_khong_thoa | 1.3 |
| L27 | mat_thua_nghiem | sai | SAI | 1 | mat_nghiem | 12.3 |
| L23 | chia_cho_0 | sai | SAI | 1 | mat_nghiem | 6.4 |
| L28 | chia_cho_0 | sai | SAI | 1 | mat_nghiem | 8.1 |
| L29 | chia_cho_0 | sai | SAI | 1 | mat_ho_nghiem | 83.2 |
| L31 | luong_giac | sai | SAI | 1 | mat_ho_nghiem | 30.4 |
| L32 | luong_giac | sai | SAI | 1 | chu_ky_sai | 3.3 |
| L33 | luong_giac | sai | SAI | 1 | mat_ho_nghiem | 22.3 |
| L34 | luong_giac | sai | SAI | 1 | mat_ho_nghiem | 23.1 |
| L35 | luong_giac | sai | SAI | 1 | thua_nghiem_khong_thoa | 6.2 |
| L36 | bpt_nhan_so_am | sai | SAI | 1 | khac_tap | 12.1 |
| L37 | bpt_nhan_so_am | sai | SAI | 1 | khac_tap | 13.2 |
| L38 | bpt_nhan_so_am | sai | SAI | 1 | khac_tap | 15.7 |
| L39 | bpt_nhan_so_am | sai | SAI | 1 | khac_tap | 32.2 |
| L40 | tich_phan_nguyen_ham | sai | SAI | 1 | thieu_hang_so_C | 0.6 |
| L41 | tich_phan_nguyen_ham | sai | SAI | 1 | tich_phan_sai | 24.7 |
| L42 | tich_phan_nguyen_ham | sai | SAI | 1 | ham_khong_xac_dinh_tren_doan | 5.3 |
| L43 | tich_phan_nguyen_ham | sai | SAI | 1 | nguyen_ham_sai_mien | 1.4 |
| L44 | tich_phan_nguyen_ham | sai | SAI | 1 | nguyen_ham_sai | 33.5 |
| L45 | tich_phan_nguyen_ham | sai | SAI | 1 | tich_phan_sai | 9.1 |
| L46 | to_hop_xac_suat | sai | SAI | 1 | dem_sai | 0.5 |
| L47 | to_hop_xac_suat | sai | SAI | 1 | xac_suat_sai | 0.4 |
| L48 | to_hop_xac_suat | sai | SAI | 1 | dem_sai | 0.4 |
| L49 | to_hop_xac_suat | sai | SAI | 1 | xac_suat_sai | 0.4 |
| L50 | to_hop_xac_suat | sai | SAI | 1 | xac_suat_sai | 0.4 |
| D01 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | dao_ham_sai | 4.8 |
| D02 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | don_dieu_sai | 7.1 |
| D03 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | cuc_tri_sai | 2.1 |
| D04 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | cuc_tri_sai | 8.8 |
| D05 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | cuc_tri_sai | 6.0 |
| D06 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | cuc_tri_sai | 57.2 |
| D07 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | don_dieu_sai | 36.5 |
| D08 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | don_dieu_sai | 24.6 |
| D09 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | cuc_tri_sai | 4.0 |
| D10 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | cuc_tri_sai | 3.1 |
| D11 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | don_dieu_sai | 19.7 |
| D12 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | don_dieu_sai | 31.4 |
| D13 | dao_ham_don_dieu_cuc_tri | sai | SAI | 1 | cuc_tri_sai | 11.5 |
| B01 | sai_dau | sai | SAI | 3 | khong_tuong_duong | 25.6 |
| B02 | chia_cho_0 | sai | SAI | 2 | mat_nghiem | 8.7 |
| B03 | bpt_nhan_so_am | sai | SAI | 3 | khong_tuong_duong | 23.2 |
| B04 | dieu_kien_xac_dinh | sai | SAI | 5 | ket_luan_sai | 58.2 |
| B05 | dieu_kien_xac_dinh | sai | SAI | 3 | ket_luan_sai | 54.5 |
| K01 | loi_van_khong_kiem_duoc | sai | KHONG_KIEM_DUOC |  | khong_hinh_thuc_hoa | 0.0 |
| K02 | loi_van_khong_kiem_duoc | sai | KHONG_KIEM_DUOC |  | khong_hinh_thuc_hoa | 0.0 |
| C01 | doi_chung | dung | DAT |  | dong_nhat | 34.1 |
| C02 | doi_chung | dung | DAT |  | dong_nhat | 79.0 |
| C03 | doi_chung | dung | DAT |  | dong_nhat | 34.9 |
| C04 | doi_chung | dung | DAT |  | tap_nghiem | 6.6 |
| C05 | doi_chung | dung | DAT |  | tap_nghiem | 28.3 |
| C06 | doi_chung | dung | DAT |  | tap_nghiem | 15.4 |
| C07 | doi_chung | dung | DAT |  | ho_nghiem | 37.8 |
| C08 | doi_chung | dung | DAT |  | tap_nghiem_bpt | 34.1 |
| C09 | doi_chung | dung | DAT |  | tap_nghiem_bpt | 54.8 |
| C10 | doi_chung | dung | DAT |  | nguyen_ham | 17.1 |
| C11 | doi_chung | dung | DAT |  | tich_phan | 6.1 |
| C12 | doi_chung | dung | DAT |  | don_vi | 5.2 |
| C13 | doi_chung | dung | DAT |  | dem | 0.4 |
| C14 | doi_chung | dung | DAT |  | xac_suat | 0.3 |
| C15 | doi_chung | dung | DAT |  | dao_ham | 21.8 |
| C16 | doi_chung | dung | DAT |  | nguyen_ham | 47.6 |
| C17 | doi_chung | dung | DAT |  | ho_nghiem | 21.8 |
| C18 | doi_chung | dung | DAT |  | chu_ky | 2.6 |
| C19 | doi_chung | dung | DAT |  | ho_nghiem | 18.7 |
| C20 | doi_chung | dung | DAT |  | dong_nhat | 29.5 |
| C21 | doi_chung | dung | DAT |  | dong_nhat | 7.8 |
| C22 | doi_chung | dung | DAT |  | nguyen_ham | 51.3 |
| C23 | doi_chung | dung | DAT |  | tap_nghiem_bpt | 87.0 |
| DC1 | doi_chung | dung | DAT |  | don_dieu | 6.1 |
| DC2 | doi_chung | dung | DAT |  | cuc_tri | 3.9 |
| DC3 | doi_chung | dung | DAT |  | cuc_tri | 2.3 |
| DC4 | doi_chung | dung | DAT |  | cuc_tri | 6.7 |
| DC5 | doi_chung | dung | DAT |  | don_dieu | 37.6 |
| DC6 | doi_chung | dung | DAT |  | don_dieu | 18.4 |
| DC7 | doi_chung | dung | SAI | 1 | cuc_tri_sai | 9.1 |
| BC1 | doi_chung | dung | DAT |  | bien_doi_bieu_thuc | 72.1 |
| BC2 | doi_chung | dung | DAT |  | bien_doi_phuong_trinh | 23.4 |
| KC1 | doi_chung | dung | KHONG_KIEM_DUOC |  | khong_hinh_thuc_hoa | 0.0 |
