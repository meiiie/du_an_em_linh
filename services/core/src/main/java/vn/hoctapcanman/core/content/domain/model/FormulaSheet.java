package vn.hoctapcanman.core.content.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Một phiên bản bảng công thức của lớp (data-model §content, ADR 013). Bảng nháp ({@code NHAP}) giáo viên sửa; khóa
 * ({@code KHOA}) khi mọi dòng {@code DAT} ở tầng 1 và tầng 2 có trích dẫn, và bảng phải có ít nhất một dòng. Bảng đã
 * khóa không sửa được: muốn đổi thì tạo bảng nháp phiên bản mới ({@link #newDraft}). Bảng đang dùng của lớp là bảng
 * {@code KHOA} có phiên bản lớn nhất.
 */
public record FormulaSheet(
        UUID id,
        UUID classId,
        int version,
        SheetStatus status,
        @Nullable String note,
        @Nullable String fingerprint,
        @Nullable Instant lockedAt,
        @Nullable UUID lockedBy,
        Instant createdAt,
        List<Formula> rows) {

    private static final int GHI_CHU_DAI_TOI_DA = 500;

    public FormulaSheet {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(rows, "rows");
        if (version < 1) {
            throw new IllegalArgumentException("Phiên bản bảng phải từ 1");
        }
        if (note != null) {
            Kiem.toiDa(note, GHI_CHU_DAI_TOI_DA, "Ghi chú của bảng");
        }
        rows = rows.stream().sorted(Comparator.comparingInt(Formula::ordinal)).toList();
        Set<Integer> thuTu = new HashSet<>();
        Set<String> ma = new HashSet<>();
        for (Formula dong : rows) {
            if (!thuTu.add(dong.ordinal()) || !ma.add(dong.code())) {
                throw new IllegalArgumentException("Hai dòng trùng thứ tự hoặc trùng mã");
            }
        }
        if (status == SheetStatus.KHOA) {
            Objects.requireNonNull(fingerprint, "fingerprint");
            Objects.requireNonNull(lockedAt, "lockedAt");
            Kiem.sha256(fingerprint, "Dấu vân tay của bảng");
            if (rows.isEmpty() || !rows.stream().allMatch(Formula::passes)) {
                throw new IllegalArgumentException("Bảng khóa phải có dòng và mọi dòng DAT ở tầng 1 và tầng 2");
            }
        } else if (fingerprint != null || lockedAt != null || lockedBy != null) {
            throw new IllegalArgumentException("Bảng nháp không có dấu vân tay hay người khóa");
        }
    }

    /** Bảng nháp mới của lớp, các dòng chưa kiểm. */
    public static FormulaSheet draft(UUID classId, int version, @Nullable String note, List<Formula> rows, Instant now) {
        return new FormulaSheet(UUID.randomUUID(), classId, version, SheetStatus.NHAP, note, null, null, null, now, rows);
    }

    /**
     * Ghi kết quả {@code kiem-dong-cong-thuc} vào bảng nháp, theo mã dòng. Dòng không có kết quả thì kết quả cũ bị bỏ:
     * dòng đó chưa kiểm, nên bảng chưa khóa được.
     */
    public FormulaSheet withCheckResults(Map<String, FormulaCheck> results) {
        requireDraft();
        List<Formula> dongMoi = rows.stream().map(dong -> dong.withCheck(results.get(dong.code()))).toList();
        return new FormulaSheet(id, classId, version, status, note, null, null, null, createdAt, dongMoi);
    }

    /** Mã các dòng chưa đạt cả hai tầng, theo thứ tự dòng; rỗng (và bảng có dòng) thì khóa được. */
    public List<String> rowsNotPassing() {
        return rows.stream().filter(dong -> !dong.passes()).map(Formula::code).toList();
    }

    /**
     * Khóa bảng nháp. Bảng trống hay còn dòng chưa đạt thì {@link IllegalStateException} kèm mã các dòng chưa qua (nơi gọi
     * trả 422 với danh sách đó); không bao giờ khóa thiếu. {@code by} trống khi importer khóa bảng của v0.
     */
    public FormulaSheet lock(@Nullable UUID by, Instant now) {
        requireDraft();
        if (rows.isEmpty()) {
            throw new IllegalStateException("Bảng công thức trống, không khóa được");
        }
        List<String> chuaQua = rowsNotPassing();
        if (!chuaQua.isEmpty()) {
            throw new IllegalStateException("Dòng chưa đạt cả tầng 1 và tầng 2: " + String.join(", ", chuaQua));
        }
        return new FormulaSheet(id, classId, version, SheetStatus.KHOA, note, fingerprintOf(rows), now, by, createdAt, rows);
    }

    /** Bảng nháp phiên bản {@code nextVersion}, chép các dòng của bảng đã khóa này, chưa kiểm (sửa rồi khóa lại). */
    public FormulaSheet newDraft(int nextVersion, Instant now) {
        if (status != SheetStatus.KHOA) {
            throw new IllegalStateException("Chỉ tạo bảng nháp mới từ một bảng đã khóa");
        }
        if (nextVersion <= version) {
            throw new IllegalArgumentException("Phiên bản mới phải lớn hơn " + version);
        }
        return draft(classId, nextVersion, note, rows.stream().map(Formula::copyUnchecked).toList(), now);
    }

    public boolean isLocked() {
        return status == SheetStatus.KHOA;
    }

    /**
     * SHA-256 (hex) của các dòng theo thứ tự: mã, kỹ năng, tiêu đề, LaTeX, lời phát biểu. Mỗi trường mang độ dài đứng
     * trước, nên hai bảng khác nhau không bao giờ cho cùng một chuỗi, dù chữ chứa ký tự gì. Không gồm kết quả kiểm: cùng
     * nội dung thì cùng dấu vân tay.
     */
    public static String fingerprintOf(List<Formula> rows) {
        StringBuilder chuoi = new StringBuilder();
        rows.stream().sorted(Comparator.comparingInt(Formula::ordinal)).forEach(dong -> {
            for (String truong : new String[] {
                    dong.code(), Objects.requireNonNullElse(dong.skillCode(), ""), dong.title(), dong.latex(), dong.statement()}) {
                chuoi.append(truong.length()).append(':').append(truong);
            }
            chuoi.append(';');
        });
        try {
            byte[] bam = MessageDigest.getInstance("SHA-256").digest(chuoi.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bam);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e);
        }
    }

    private void requireDraft() {
        if (status != SheetStatus.NHAP) {
            throw new IllegalStateException("Bảng đã khóa không sửa được; tạo bảng nháp phiên bản mới");
        }
    }
}
