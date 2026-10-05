package vn.hoctapcanman.core.content.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Lời giải mẫu viết cho học sinh sau khi nộp (FR-006, ADR 003): đường duy nhất để chữ lời giải rời module nội dung. Cổng chỉ
 * kiểm phần của nội dung (bài đang phát hành ở lớp, đúng phiên bản); không kiểm học sinh, bài làm đã nộp hay cờ lớp, nên chỉ
 * {@code MoLoiGiai} của practice được dùng (ArchUnit {@code LOI_GIAI_CHI_QUA_CUA_MO}). Dữ kiện bảo vệ không bao giờ ra khỏi
 * nội dung.
 */
public interface LoiGiaiSauKhiNop {

    /**
     * Lời giải của bài {@code problemId} viết cho học sinh lớp {@code lopId}, khi bài đang phát hành ở lớp
     * ({@code DA_PHAT_HANH}) và phiên bản nội dung hiện tại đúng là {@code phienBan} (bài làm đã nộp cho đúng đề này), viết
     * như v0 ({@code loiGiaiHocSinh}). Chưa phát hành, đề đã đổi, không có lời giải hay lời giải không viết được thì rỗng.
     */
    Optional<String> vanBan(UUID lopId, UUID problemId, int phienBan);
}
