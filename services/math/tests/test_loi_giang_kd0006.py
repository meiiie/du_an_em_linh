# -*- coding: utf-8 -*-
"""KD-0006: câu gợi mở không khẳng định quan hệ; quy tắc và kết quả vẫn đóng mặc định."""
import json
from pathlib import Path
import re

import pytest

from app.loi_giang import kiem_loi_giang


CORPUS = json.loads((Path(__file__).parent / "data" / "ban-nhap-glm-2026-10-14.json").read_text(encoding="utf-8"))
BANG = CORPUS["bang_cong_thuc"]
HAM = "x**2-6*x+4"


def _chay(cau, bang=BANG, ham=HAM):
    return kiem_loi_giang({"cau": cau, "bang_cong_thuc": bang, "ham": ham, "bai_lam_hoc_sinh": []})


# Câu nguyên văn hoặc câu con của corpus, không chọn giữ một mệnh đề khẳng định kèm câu hỏi.
@pytest.mark.parametrize("cau", [
    "Em hãy viết rõ điều kiện xác định trước khi dùng quy tắc đạo hàm.",
    "Có giá trị thực nào làm hàm không xác định không?",
    "Từ đó tập xác định của em sẽ là tập nào?",
    "Em xem lại hàm số $y = x^{2} - 6 x + 4$: có giá trị $x$ nào làm hàm này không xác định không, em?",
    "Em đang ở bước tìm tập xác định: em hãy xem hàm số này có bị cấm giá trị nào của biến hay không, và kết luận tập xác định là tập nào?",
    "Em cũng kiểm tra xem đạo hàm có điểm nào không xác định không dựa theo [6].",
    "Đừng quên kiểm tra xem $y'$ có điểm nào không xác định trong tập xác định không nhé, em có thể xem lại khái niệm điểm tới hạn ở [6].",
    "Em hãy nghĩ xem đạo hàm của hàm số này là gì trước đã, rồi từ đó tìm những điểm nào có thể làm dấu đạo hàm thay đổi nhé?",
    "Em đã thử tính đạo hàm của hàm số theo các công thức [1] và [2] chưa?",
    "Sau khi tính đạo hàm bằng [1] và [2], em cần giải phương trình đạo hàm bằng không để tìm điểm tới hạn, xem [6] — em đã thử giải phương trình đó chưa?",
    "Bước này em cần lập bảng xét dấu của đạo hàm — em đã tìm được nghiệm của $y'$ để chia trục số thành các khoảng chưa?",
    "Nghiệm tìm được sẽ chia trục số thành những khoảng nào để em lập bảng xét dấu?",
    "Nghiệm đạo hàm chia trục số thành những khoảng nào?",
    "Em đã thử thay một giá trị đại diện vào từng khoảng trong bảng chưa?",
    "Em xem quy tắc đơn điệu ở [4] và quy tắc cực trị ở [5], rồi tự điền dấu vào từng khoảng nhé.",
    "Ở bước này, em dựa vào dấu của $y'$ để kết luận: em đã xem xét $y'$ dương trên khoảng nào và âm trên khoảng nào chưa?",
    "Với hàm đa thức dạng này, em thử nghĩ xem có giá trị nào của biến làm hàm số không xác định không, và từ đó tập xác định là tập nào?",
    "Với hàm đa thức này, em hãy cho mình biết giá trị của biến có bị cấm ở điểm nào không, và tập xác định của hàm là tập nào nhé?",
    "Em hãy thử lấy đạo hàm của cả hàm số trên theo từng hạng tử, xem [1] và [2] nhé.",
    "Hãy lấy đạo hàm từng hạng tử của hàm bậc hai đã cho.",
    "Mức 1: Trước khi lập bảng xét dấu, em hãy cho mình biết đạo hàm $y'$ của hàm số $y = - x^{2} - 2 x + 5$ bằng bao nhiêu đã nhé?",
    "Trên mỗi khoảng đó, em cần tìm gì để điền vào bảng xét dấu?",
    "Em đã có dấu của $y'$ trên từng khoảng rồi chưa?",
    "Em đã lập được bảng dấu của đạo hàm $y'$ chưa?",
    "Em lập bảng xét dấu cho $f'(x)$ nhé.",
    "Em đã tính đạo hàm của $y = x^{2} - 6 x + 4$ chưa, và em cần tìm những giá trị nào để giải phương trình $y' = 0$ ở bước này?",
    "Em đã có đạo hàm $y'$ chưa, và theo bước này em cần giải phương trình $y' = 0$ với $y'$ của hàm $y = x^{2} - 6 x + 4$ đã tính ở bước trước?",
    "Em cần giải phương trình $y' = 0$ như thế nào?",
], ids=lambda cau: cau[:65])
def test_cau_goi_mo_duoc_giu(cau):
    ham = "-x**2-2*x+5" if "- x^{2}" in cau else HAM
    kq = _chay(cau, ham=ham)
    assert kq["cau_sach"] == cau
    assert kq["thay_bang_goi_y"] is False
    assert all(p["trang_thai"] == "DAT" for p in kq["bieu_thuc"])


@pytest.mark.parametrize("cau", [
    "Đạo hàm của hằng số bằng 1 đúng không?",
    "Em hãy nhớ đạo hàm của tổng bằng tích các đạo hàm nhé.",
    "Em cần kiểm tra đạo hàm của tích bằng tích các đạo hàm chưa?",
    "Em thử xem đạo hàm của thương bằng tổng các đạo hàm đúng không?",
    "Có phải đạo hàm của tổng bằng tích các đạo hàm không?",
    "Đạo hàm của hằng số bằng một đúng không?",
    "Đạo hàm của tích là tích các đạo hàm phải không?",
    "Em hãy lấy đạo hàm của tích lấy tích đạo hàm nhé.",
    "Em hãy dùng đạo hàm của hằng số khác không nhé.",
    "Em hãy tìm điểm nào, đạo hàm của hằng số không xác định.",
    "Đạo hàm của hằng số không xác định, em cần tìm điểm nào?",
    "Có giá trị nào làm đạo hàm của hằng số bằng một không xác định không?",
    "Em hãy nhớ nếu đạo hàm âm thì hàm số đồng biến.",
    "Nếu đạo hàm dương thì hàm số nghịch biến đúng không?",
    "Đạo hàm âm suy ra hàm số đồng biến đúng không?",
    "Em hãy nhớ đạo hàm không đổi dấu thì hàm đạt cực đại nhé.",
    "Em hãy nhớ mọi nghiệm đạo hàm đều là cực trị nhé.",
    "Em thử xem đạo hàm của tích: tích các đạo hàm đúng không?",
    "Với tích cũng vậy phải không?",
    "Luỹ thừa là thế đúng không?",
    "dao ham cua hang so bang mot dung khong?",
    "Em hãy nhớ đạo hàm của tổng b​ằng tích các đạo hàm nhé.",
    "Em hãy nhớ đạo hàm của tích bằng đạo hàm u nhân v cộng u nhân đạo hàm v.",
    "Nếu đạo hàm dương trên một khoảng thì hàm số đồng biến trên khoảng đó đúng không?",
    "Hàm đã cho là đa thức, em nghĩ tập xác định là gì?",
    "Hàm đã cho là đa thức.",
    "Đáp án là 0.",
    "Đáp án là hai đúng không?",
    "Em hãy viết đáp án là hai nhé.",
    "Nghiệm là 0.",
    "Nghiệm 0 nhé.",
    "Em hãy chọn giá trị 0 nhé.",
    "Em cần thử giá trị -2 nhé.",
    "Em hãy thử $0$ nhé.",
    "Em hãy thử $-2$ nhé.",
    "Em có giá trị $x$ nào bằng $0$ không?",
    "Em hãy giải phương trình $x = 0$.",
    "Em hãy giải phương trình $y' = -2$.",
    "Em hãy giải phương trình $y' = 1$.",
    "Em hãy giải phương trình $y' = 0 + 0$.",
    "Ta có $y' = 0$.",
    "Đạo hàm là $y' = 0$ đúng không?",
    "Em tìm nghiệm của $y' = 0$ nhé.",
    "Em thử đặt $y' = 0$ nhé.",
    "Em cần giải phương trình $y' = 0$, nghiệm là $x = 3$ nhé.",
    "Em cần giải phương trình $y' = 0$ vì đạo hàm của hằng số bằng một nhé.",
    "Hàm đồng biến trên $(0; 3)$ đúng không?",
    "Em hãy xét khoảng (-2; 0) nhé.",
    "Em hãy xét khoảng ]0; 3[ nhé.",
    "Em hãy kết luận hàm nghịch biến trên khoảng từ không đến ba nhé.",
    "Đạo hàm dương tại $x = 0$ đúng không?",
    "$y'$ âm giữa 0 và 3 đúng không?",
    "Cực đại tại hai.",
    "Cực tiểu ở ba đúng không?",
    "Em hãy kết luận cực đại bằng hai nhé.",
    "Em xem giá trị $y'(0)$ nhé.",
    "Em xem lại $x_0$ nhé.",
    "Em cần giải phương trình $y' = 0$ và thử giá trị 0 nhé.",
    "Em hãy xem $x = 3$ là nghiệm nào?",
    "Kết quả ba nhé.",
    "Ta có hai.",
    "Giá trị âm hai nhé.",
    "Em hãy nhớ đạo hàm của tổng là tổng của từng đạo hàm nhé.",
    "Đạo hàm của tổng là cộng các đạo hàm phải không?",
    "Em hãy nhớ đạo hàm của thương bằng hiệu các đạo hàm nhé.",
    "Em hãy nhớ đạo hàm của hằng số bằng hai rồi tìm tập xác định là tập nào?",
    "Em hãy giải phương trình y'=0 nhé.",
    "Em cần giải phương trình $y' = 0$ nhưng đạo hàm âm suy ra đồng biến phải không?",
    "Em hãy chọn giá trị 0.5 nhé.",
    "Em hãy chọn giá trị ０ nhé.",
    "Em hãy nhớ đạo hàm của tổng bằ​ng tích các đạo hàm, em tìm nghiệm nào?",
    "Em hãy xác định đạo hàm đổi   dấu nhé.",
    "Em hãy xem đạo hàm của tổng tương    tự đạo hàm của tích nhé.",
    "Em hãy xác định đạo hàm không\txác định nhé.",
    "Em hãy xem hàm số đồng   biến nhé.",
    "Hàm xác định trên toàn bộ số thực.",
    "Hàm không có nghiệm đúng không?",
    "Phương trình vô nghiệm.",
    "Em hãy nhớ đạo hàm của tổng cho tích đạo hàm nhé.",
    "Đạo hàm của tổng nhận tích đạo hàm phải không?",
    "Em hãy thử đạo hàm của tổng ra tích đạo hàm nhé.",
], ids=lambda cau: cau[:65])
def test_quy_tac_ket_qua_va_cau_mo_ho_van_bi_bo(cau):
    kq = _chay(cau)
    assert kq["cau_sach"] == ""
    assert kq["thay_bang_goi_y"] is True
    assert any(p["trang_thai"] != "DAT" for p in kq["bieu_thuc"])


@pytest.mark.parametrize("ky", ["x", "y"])
def test_ten_bien_trong_cau_hoi_khong_mang_gia_tri(ky):
    cau = "Có giá trị $%s$ nào làm hàm không xác định không?" % ky
    kq = _chay(cau, bang=[])
    assert kq["cau_sach"] == cau
    assert [(p["loai"], p["trang_thai"]) for p in kq["bieu_thuc"]] == [("KY_HIEU", "DAT")]


@pytest.mark.parametrize("doi", ["rỗng", "thiếu mảnh", "thiếu trích dẫn", "sai tầng một"])
@pytest.mark.parametrize("cau", ["Em cần giải phương trình $y' = 0$ nhé.",
                                  "Em cần giải phương trình đạo hàm bằng không nhé."])
def test_ten_phuong_trinh_chi_duoc_giu_khi_co_manh_trong_dong_dung_duoc(doi, cau):
    bang = [dict(d) for d in BANG if d["id"] == "d-4"]
    if doi == "rỗng":
        bang = []
    elif doi == "thiếu mảnh":
        bang[0]["latex"] = r"y' > 0 \Rightarrow \text{đồng biến}"
    elif doi == "thiếu trích dẫn":
        bang[0]["trich_dan"] = None
    else:
        bang[0]["latex"] = r"y'=0 \text{ trên } K \Rightarrow \text{nghịch biến trên } K"
        bang[0]["phat_bieu"] = "Nếu đạo hàm bằng 0 trên khoảng K thì hàm nghịch biến trên K."
    assert _chay(cau, bang=bang)["cau_sach"] == ""


def test_ten_phuong_trinh_ghi_ro_dong_bang_va_ba_tang():
    kq = _chay("Em cần giải phương trình $y' = 0$ nhé.")
    assert kq["bieu_thuc"] == [{"doan": "y' = 0", "loai": "CONG_THUC_TONG_QUAT", "trang_thai": "DAT",
                                "dong_bang": "d-4", "tang": {"1": "DAT", "2": "DAT", "3": "DAT"}, "cau": 0}]


def test_cau_hoi_khong_cuu_menh_de_sai_trong_cung_cau():
    cau = "Đạo hàm của hằng số bằng một, em thấy tập xác định là tập nào?"
    assert _chay(cau)["cau_sach"] == ""
    kq = _chay("Đạo hàm của hằng số bằng một. Tập xác định của em sẽ là tập nào?")
    assert [c["giu"] for c in kq["cac_cau"]] == [False, True]
    assert kq["cau_sach"] == "Tập xác định của em sẽ là tập nào?"


def test_corpus_ty_le_bo_toan_bo_va_khong_giu_gia_tri_so():
    bo_het = 0
    so_da_kiem = 0
    # Xác định giá trị số độc lập với nhãn của bộ nhận diện; không tính hệ số trong trích đề,
    # chỉ số [n], mức/bước, hay tên phương trình đã kiểm theo mảnh của bảng.
    gia_tri_so = re.compile(r"\$\s*(-?\d+(?:[.,]\d+)?)\s*\$")
    for d in CORPUS["ban_nhap"]:
        kq = _chay(d["draft"], ham=d["ham"])
        assert "loi" not in kq
        assert kq == _chay(d["draft"], ham=d["ham"])
        bo_het += not bool(kq["cau_sach"])
        for cau in kq["cac_cau"]:
            if gia_tri_so.search(cau["cau"]):
                assert cau["giu"] is False
        for m in gia_tri_so.finditer(d["draft"]):
            so_da_kiem += 1
            assert m.group(0) not in kq["cau_sach"]
            assert any(p["doan"] == m.group(1) and p["trang_thai"] != "DAT" for p in kq["bieu_thuc"])
    assert so_da_kiem == 4
    assert bo_het / len(CORPUS["ban_nhap"]) <= 0.20
