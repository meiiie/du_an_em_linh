package vn.hoctapcanman.core.content.application.dto.hocsinh;

/** Metadata công khai của một bước; không chứa dữ kiện chấm hay đáp án. */
public record MoTaBuoc(String maBuoc, String ten, String viec, String kieu) {
    /** Khung cũ dùng cho bài v0 và bài luyện trên giấy. Khung mới lấy tên/việc theo từng bài. */
    public static MoTaBuoc cu(String ma) {
        return switch (ma) {
            case "B.DH.TXD" -> new MoTaBuoc(ma, "Tập xác định", "Viết tập xác định của hàm số", "TXD");
            case "B.DH.DAOHAM" -> new MoTaBuoc(ma, "Đạo hàm", "Tính đạo hàm của hàm số", "DAO_HAM");
            case "B.DH.NGHIEM" -> new MoTaBuoc(ma, "Nghiệm y′", "Tìm nghiệm y′ = 0 và điểm y′ không xác định", "NGHIEM");
            case "B.DH.XETDAU" -> new MoTaBuoc(ma, "Xét dấu", "Xét dấu y′ và chiều biến thiên", "BANG_XET_DAU");
            case "B.DH.KETLUAN" -> new MoTaBuoc(ma, "Kết luận", "Kết luận khoảng đơn điệu và cực trị", "KET_LUAN_DH");
            default -> new MoTaBuoc(ma, ma.endsWith(".LAPLUAN") ? "Lập luận" : ma.endsWith(".KETLUAN") ? "Kết luận" : "Phân tích đề", "", "GIAY");
        };
    }
}
