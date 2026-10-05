package vn.hoctapcanman.core.practice.application.exception;

/** Bài làm chưa nộp được (web: 409, contracts/api-core.md); không ghi gì. Thông điệp nói học sinh phải làm gì. */
public class BaiChuaNopDuocException extends RuntimeException {

    public enum LyDo {
        /** Chưa có bài làm, hay bài làm thiếu một bước từ bước bắt đầu tới bước kết luận. */
        CHUA_LAM_DU_BUOC("Em làm đủ các bước, tới bước kết luận, rồi hãy nộp bài."),
        /** Đủ bước nhưng bước kết luận chưa có phán quyết cho nội dung hiện tại: chấm lỗi, hay đã sửa một bước sau lần chấm đó. */
        CHUA_CHAM_BUOC_KET_LUAN("Em nộp bước kết luận và chờ máy chấm xong rồi hãy nộp bài."),
        /** Bài làm dở là của đề cũ: nội dung bài đã đổi. */
        DE_DA_DOI("Đề bài vừa được cập nhật. Em làm lại theo đề mới nhé.");

        private final String thongBao;

        LyDo(String thongBao) {
            this.thongBao = thongBao;
        }

        public String thongBao() {
            return thongBao;
        }
    }

    private final LyDo lyDo;

    public BaiChuaNopDuocException(LyDo lyDo) {
        super(lyDo.thongBao());
        this.lyDo = lyDo;
    }

    public LyDo lyDo() {
        return lyDo;
    }
}
