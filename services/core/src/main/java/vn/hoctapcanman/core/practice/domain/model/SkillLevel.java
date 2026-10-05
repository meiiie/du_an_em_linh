package vn.hoctapcanman.core.practice.domain.model;

import java.util.Objects;
import java.util.Set;

/**
 * Kỹ năng và mức (4 mức) của bài lúc nộp, ghim vào bài làm đã nộp (Codex #142): sửa hai trường này ở bài không đổi
 * {@code content_hash}, nên không tăng phiên bản nội dung; bài đã nộp vẫn mang phân loại nó được chấm theo.
 */
public record SkillLevel(String skillCode, String level4) {

    private static final Set<String> MUC = Set.of("NHAN_BIET", "THONG_HIEU", "VAN_DUNG", "VAN_DUNG_CAO");

    public SkillLevel {
        Objects.requireNonNull(skillCode, "skillCode");
        Objects.requireNonNull(level4, "level4");
        if (skillCode.isBlank() || skillCode.length() > 32) {
            throw new IllegalArgumentException("Mã kỹ năng phải có chữ, tối đa 32 ký tự");
        }
        if (!MUC.contains(level4)) {
            throw new IllegalArgumentException("Mức không hợp lệ: " + level4);
        }
    }
}
