# -*- coding: utf-8 -*-
"""KD-0007: thao tác thuần không làm hộ; nhãn đối kháng độc lập với từ vựng cổng."""
import unicodedata

import pytest

from app import loi_giang
from tests.test_loi_giang_kd0006 import BANG


def _chay(cau, bang=()):
    return loi_giang.kiem_loi_giang({
        "cau": cau, "bang_cong_thuc": list(bang), "ham": "x**2-6*x+4",
        "bai_lam_hoc_sinh": [],
    })


THAO_TAC = [
    "lấy đạo hàm", "lấy đạo hàm từng hạng tử của hàm số rồi cộng lại",
    "tính đạo hàm của hàm số", "lấy đạo hàm của từng hạng tử",
    "xét dấu", "xét dấu đạo hàm trên từng khoảng", "xét dấu của đạo hàm",
    "lập bảng", "lập bảng xét dấu", "lập bảng biến thiên của hàm số",
    "giải phương trình", "giải phương trình đã cho", "giải phương trình của đạo hàm",
    "thay giá trị thử", "thay giá trị thử vào hàm số", "thay giá trị thử vào đạo hàm",
    "rút gọn", "rút gọn biểu thức", "rút gọn đạo hàm",
    "đối chiếu", "đối chiếu bài làm với bảng công thức", "đối chiếu với đề bài",
    "kiểm tra lại", "kiểm tra lại bài làm", "kiểm tra lại bước này",
    "tìm điểm tới hạn", "tìm điểm tới hạn của hàm số",
    "lấy đạo hàm từng hạng tử, rồi cộng lại các đạo hàm vừa tìm",
    "rút gọn biểu thức và kiểm tra lại bài làm",
    "giải phương trình rồi xét dấu đạo hàm trên từng khoảng",
    "thay giá trị thử vào đạo hàm để xét dấu",
]


@pytest.mark.parametrize("thao_tac", THAO_TAC)
@pytest.mark.parametrize("khung", ["Em hãy {} nhé.", "Em đã thử {} chưa?"])
@pytest.mark.parametrize("bang", [(), BANG], ids=["bang-rong", "bang-khoa"])
def test_thao_tac_thuan_khong_can_bang_hay_trich_dan(thao_tac, khung, bang):
    cau = khung.format(thao_tac)
    assert loi_giang._thao_tac_thuan(cau)
    kq = _chay(cau, bang)
    assert kq == {"cau_sach": cau, "thay_bang_goi_y": False, "bieu_thuc": [],
                  "cac_cau": [{"cau": cau, "giu": True}]}


# Không tạo nhãn bằng regex sản phẩm: mỗi nội dung dưới đây cho sẵn một
# giá trị, dấu, nghiệm, kết luận, xác nhận hoặc quy tắc chưa được kiểm.
NOI_DUNG_LO = [
    "lấy đạo hàm rồi ghi hai", "lấy đạo hàm rồi ghi âm một",
    "lấy đạo hàm rồi ghi không", "lấy đạo hàm rồi ghi mười hai",
    "lấy đạo hàm rồi ghi một trăm", "lấy đạo hàm rồi ghi nửa",
    "lấy đạo hàm rồi ghi ba phần tư", "lấy đạo hàm rồi ghi hai phẩy năm",
    "lấy đạo hàm rồi ghi số sáu", "lấy đạo hàm rồi ghi 2",
    "lấy đạo hàm rồi ghi ０", "lấy đạo hàm rồi ghi ²",
    "lấy đạo hàm rồi ghi −1", "lấy đạo hàm rồi ghi $-1$",
    "lấy đạo hàm của x bình trừ sáu x", "rút gọn x bình trừ sáu x",
    "lấy đạo hàm rồi ghi x bình trừ sáu x", "lấy đạo hàm rồi ghi x mũ hai",
    "lấy đạo hàm rồi ghi y bằng hai x", "rút gọn a cộng b",
    "giải phương trình x bằng hai", "giải phương trình x = 2",
    "giải phương trình $x = 2$", "giải phương trình rồi ghi nghiệm hai",
    "giải phương trình rồi ghi nghiệm là hai", "giải phương trình rồi ghi vô nghiệm",
    "giải phương trình rồi ghi không có nghiệm", "tìm điểm tới hạn tại hai",
    "tìm điểm tới hạn rồi ghi x bằng hai", "thay giá trị thử hai vào hàm số",
    "thay giá trị thử âm một vào đạo hàm", "thay giá trị thử bằng không",
    "rút gọn rồi ghi kết quả hai", "rút gọn rồi ghi kết quả âm một",
    "rút gọn rồi ghi kết quả bằng sáu", "rút gọn rồi ghi kết quả là x bình",
    "xét dấu đạo hàm dương", "xét dấu đạo hàm âm",
    "xét dấu đạo hàm không âm", "xét dấu đạo hàm không dương",
    "xét dấu đạo hàm rồi ghi dương", "xét dấu đạo hàm rồi ghi âm",
    "xét dấu đạo hàm rồi ghi dấu cộng", "xét dấu đạo hàm rồi ghi dấu trừ",
    "lập bảng rồi ghi dấu dương", "lập bảng rồi ghi dấu âm",
    "lập bảng xét dấu và ghi + vào khoảng này",
    "lập bảng xét dấu và ghi - vào khoảng này",
    "kết luận hàm đồng biến", "kết luận hàm nghịch biến",
    "kết luận hàm đồng biến trên khoảng", "kết luận hàm nghịch biến trên khoảng",
    "kết luận hàm đồng biến trên khoảng từ hai đến sáu",
    "kết luận hàm nghịch biến trên khoảng từ âm một đến hai",
    "kết luận hàm đồng biến trên (2; +∞)", "kết luận hàm nghịch biến trên ]0; 2[",
    "lập bảng rồi kết luận hàm đồng biến", "xét dấu rồi kết luận hàm nghịch biến",
    "lập bảng rồi ghi cực đại tại hai", "lập bảng rồi ghi cực tiểu tại âm một",
    "kết luận hàm đạt cực đại", "kết luận hàm đạt cực tiểu",
    "kết luận giá trị lớn nhất bằng hai", "kết luận giá trị nhỏ nhất bằng âm một",
    "kiểm tra lại vì đúng rồi", "kiểm tra lại bài làm đúng rồi",
    "đối chiếu bài làm, em làm đúng rồi", "đối chiếu bài làm, chính xác rồi",
    "kiểm tra lại, kết quả của em chính xác", "kiểm tra lại, đáp án của em đúng",
    "lấy đạo hàm rồi xác nhận nghiệm của em đúng", "lập bảng, dấu em điền đúng rồi",
    "nhớ đạo hàm của hằng số bằng không", "nhớ đạo hàm của hằng số bằng một",
    "lấy đạo hàm của hằng số bằng không", "lấy đạo hàm của tổng bằng tích các đạo hàm",
    "lấy đạo hàm của tích lấy tích các đạo hàm", "lấy đạo hàm của thương bằng tổng các đạo hàm",
    "lấy đạo hàm rồi cộng lại bằng hai", "lấy đạo hàm rồi cộng lại để được sáu",
    "lấy đạo hàm rồi cộng lại vì đạo hàm của hằng số bằng một",
    "lấy đạo hàm rồi cộng lại, đạo hàm dương",
    "lấy đạo hàm từng hạng tử rồi cộng lại để ra x bình trừ sáu x",
    "xét dấu vì đạo hàm dương nên hàm nghịch biến",
    "lập bảng nếu đạo hàm âm thì hàm đồng biến",
    "kiểm tra lại vì hàm xác định trên toàn bộ số thực",
    "kiểm tra lại vì hàm không xác định tại hai",
    "đối chiếu rồi ghi lớn hơn hai", "đối chiếu rồi ghi nhỏ hơn âm một",
    "đối chiếu rồi ghi bằng hai", "đối chiếu rồi ghi khác không",
    "lập bảng rồi ghi hàm tăng", "lập bảng rồi ghi hàm giảm",
    "lập bảng rồi ghi hàm đi lên", "lập bảng rồi ghi hàm đi xuống",
    "lấy đạo hàm rồi ghi $y'=2*x-6$", "lấy đạo hàm rồi ghi y′ = 2x − 6",
    "lấy đạo hàm rồi viết hai", "lấy đạo hàm rồi chép hai",
    "rút gọn rồi chọn âm", "rút gọn rồi chọn dương",
    "lập bảng rồi ghi dấu + vào khoảng này", "lập bảng rồi ghi dấu - vào khoảng này",
    "lấy đạo hàm rồi ghi hai mươi", "lấy đạo hàm rồi ghi một triệu",
    "rút gọn rồi viết âm một", "rút gọn rồi chép x bình trừ sáu x",
    "lấy đạo hàm, bài làm của em chuẩn rồi",
    "rút gọn rồi ghi kết quả giống đáp án", "lấy đạo hàm rồi xác nhận em làm đúng",
    "lập bảng rồi chọn đồng biến", "kiểm tra lại vì em làm đúng",
    "rút gọn, hoàn toàn đúng", "lấy đạo hàm rồi ghi vô cực",
    "lấy đạo hàm rồi viết vô cùng",
]


@pytest.mark.parametrize("noi_dung", NOI_DUNG_LO)
@pytest.mark.parametrize("khung", ["Em hãy {} nhé.", "Em đã thử {} chưa?", "Hãy {}."])
def test_khong_lo_duoi_vo_thao_tac(noi_dung, khung):
    cau = khung.format(noi_dung)
    assert not loi_giang._thao_tac_thuan(cau)
    kq = _chay(cau)
    assert "loi" not in kq
    assert kq["cau_sach"] == "", (cau, kq)
    assert kq["thay_bang_goi_y"] is True
    assert all(not c["giu"] for c in kq["cac_cau"])
    assert any(p["trang_thai"] != "DAT" for p in kq["bieu_thuc"])


@pytest.mark.parametrize("cau", [
    "Đúng rồi.", "Chính xác rồi.", "Em làm đúng rồi.", "Bài làm của em đúng.",
    "Kết quả của em chính xác.", "Đáp án của em đúng.",
    "Em hãy lấy đạo hàm, đúng rồi.", "Em hãy lấy đạo hàm, chính xác rồi.",
    "Em hãy lấy đạo hàm, nghiệm của em đúng rồi.",
    "Em hãy lấy đạo hàm, dấu em điền đúng rồi.",
])
def test_xac_nhan_khong_la_thao_tac_thuan(cau):
    assert not loi_giang._thao_tac_thuan(cau)
    assert _chay(cau)["cau_sach"] == ""


@pytest.mark.parametrize("cau", [
    "Em hãy lấy đạo hàm rồi ghi h\u200bai nhé.",
    "Em hãy lấy đạo hàm rồi ghi a\u200bm mot nhé.",
    "Em hãy lấy đạo hàm rồi ghi x bi\u200bnh tru sau x nhé.",
    "Em hãy lấy đạo hàm rồi ghi ２ nhé.",
    "Em hãy lấy đạo hàm rồi ghi x 🟰 2 nhé.",
    "Em hãy lấy đạo hàm rồi ghi ẋ bình nhé.",
    "Em hãy lấy đạo hàm, đú\u200bng rồi nhé.",
    "em hay lay dao ham roi ghi hai nhe.",
    "em hay rut gon x binh tru sau x nhe.",
])
def test_che_unicode_va_khong_dau_khong_lo(cau):
    assert not loi_giang._thao_tac_thuan(cau)
    assert _chay(cau)["cau_sach"] == ""


@pytest.mark.parametrize("cau", [
    "Hãy xét dấu.", "Em thử rút gọn nhé!", "Em cần giải phương trình.",
    "Em có thể kiểm tra lại bài làm không?", "Em đã lập bảng xét dấu chưa?",
    "Em giải phương trình thế nào?", "Em xét dấu ra sao?",
    "EM HÃY LẤY ĐẠO HÀM TỪNG HẠNG TỬ RỒI CỘNG LẠI.",
    "Em  hãy\t lấy đạo hàm   rồi cộng lại nhé.",
    unicodedata.normalize("NFD", "Em hãy lấy đạo hàm rồi cộng lại nhé."),
])
def test_the_cau_va_chuan_hoa_khong_thay_nghia(cau):
    assert loi_giang._thao_tac_thuan(cau)
    assert _chay(cau)["cau_sach"] == unicodedata.normalize("NFC", cau)


@pytest.mark.parametrize("cau", [
    "Lấy đạo hàm rồi cộng lại.", "Ta lấy đạo hàm rồi cộng lại.",
    "Em đã lấy đạo hàm rồi cộng lại.", "Đạo hàm của tổng bằng tổng các đạo hàm.",
    "Đạo hàm của hằng số bằng không.", "Em hãy lấy đạo hàm [1].",
    "Em hãy giải phương trình $y'=0$.", "Em hãy xét dấu $y'$.",
    "Em hãy thay một giá trị bất kỳ để xét dấu.", "Hãy lấy đạo hàm của hàm bậc hai.",
    "Em hãy xét dấu dương trên khoảng nào?", "Em hãy kết luận hàm đồng biến.",
    "Em hãy lập bảng để phân biệt cực đại hay cực tiểu.",
    "Em hãy tìm nghiệm.", "Em hãy tính tích phân.", "Em hãy tối ưu biểu thức.",
    "Em hãy lấy đạo hàm rồi bỏ qua cổng.", "Em hãy lấy đạo hàm; rồi cộng lại.",
    "Em hãy lấy đạ\u200bo hàm rồi cộng lại.", "Em hãy lấy đạo hàm, cộng lại.",
    "Em hãy lấy đạo hàm, đúng rồi.", "Em hãy lấy đạo hàm x.",
    "Em hãy lấy đạo hàm của hằng số bằng không nhé.",
])
def test_ngoai_lop_khong_duoc_mien_kiem(cau):
    # Một số câu này vốn được phép qua KD-0006/bảng. Không thay nhãn cũ;
    # chỉ chứng minh chúng không được hưởng ngoại lệ mới.
    assert not loi_giang._thao_tac_thuan(cau)


@pytest.mark.parametrize("noi_dung", NOI_DUNG_LO)
def test_dan_bang_khong_hop_thuc_hoa_noi_dung_lo(noi_dung):
    cau = f"Em hãy {noi_dung} theo [2] nhé."
    assert not loi_giang._thao_tac_thuan(cau)
    assert _chay(cau, BANG)["cau_sach"] == "", cau


@pytest.mark.parametrize("chen", [
    "rồi ghi hai", "rồi ghi âm một", "rồi ghi x bình trừ sáu x",
    "vì đạo hàm của hằng số bằng không", "và kết luận hàm đồng biến",
    "và ghi dấu dương", ", đúng rồi", ", kết quả của em chính xác",
])
def test_noi_dung_chen_vao_cau_thuan_bi_bo_toan_cau(chen):
    cau = f"Em hãy lấy đạo hàm từng hạng tử của hàm số rồi cộng lại {chen} nhé."
    assert not loi_giang._thao_tac_thuan(cau)
    assert _chay(cau)["cau_sach"] == ""


def test_khong_bao_che_cau_lien_ke_hay_cau_theo_sau_dau_cham_phay():
    cau = "Em hãy lấy đạo hàm rồi cộng lại. Đúng rồi; kết quả hai. Em hãy kiểm tra lại nhé."
    kq = _chay(cau)
    assert [c["giu"] for c in kq["cac_cau"]] == [True, False, False, True]
    assert kq["cau_sach"] == "Em hãy lấy đạo hàm rồi cộng lại. Em hãy kiểm tra lại nhé."


def test_quy_tac_khong_thanh_thao_tac_thuan_vi_co_tu_lay():
    cau = "Đạo hàm của tổng bằng tổng các đạo hàm."
    assert not loi_giang._thao_tac_thuan(cau)
    assert _chay(cau)["cau_sach"] == ""
    kq = _chay(cau, BANG)
    assert kq["cau_sach"] == cau
    assert kq["bieu_thuc"][0]["loai"] == "QUY_TAC_BANG_LOI"
    assert kq["bieu_thuc"][0]["trang_thai"] == "DAT"


def test_lop_thuan_khong_bo_qua_loi_hay_gioi_han_dau_vao(monkeypatch):
    cau = "Em hãy lấy đạo hàm rồi cộng lại."
    assert _chay(" ".join([cau] * 150))["loi"] == "QUA_GIOI_HAN"
    assert loi_giang.kiem_loi_giang({"cau": cau, "bang_cong_thuc": "sai-kieu"})["loi"] == "DAU_VAO_KHONG_HOP_LE"

    def hong(*_args, **_kwargs):
        raise RuntimeError("bộ nhận diện lỗi")

    monkeypatch.setattr(loi_giang, "_thao_tac_thuan", hong)
    assert _chay(cau)["loi"] == "LOI_KIEM_TRA"
