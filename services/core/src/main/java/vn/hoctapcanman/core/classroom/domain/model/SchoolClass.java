package vn.hoctapcanman.core.classroom.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lớp học, ví dụ «12A1 thử» khối 12 năm học 2026-2027. Tên lớp không trùng trong một năm học. */
public record SchoolClass(ClassId id, String name, int grade, String schoolYear, Instant createdAt) {

    private static final int TEN_DAI_TOI_DA = 60;
    private static final Pattern NAM_HOC = Pattern.compile("(\\d{4})-(\\d{4})");

    public SchoolClass {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(schoolYear, "schoolYear");
        Objects.requireNonNull(createdAt, "createdAt");
        name = name.strip();
        if (name.isEmpty() || name.length() > TEN_DAI_TOI_DA) {
            throw new IllegalArgumentException("Tên lớp không hợp lệ");
        }
        if (grade < 1 || grade > 12) {
            throw new IllegalArgumentException("Khối lớp phải từ 1 đến 12");
        }
        Matcher m = NAM_HOC.matcher(schoolYear);
        if (!m.matches() || Integer.parseInt(m.group(2)) != Integer.parseInt(m.group(1)) + 1) {
            throw new IllegalArgumentException("Năm học phải có dạng 2026-2027");
        }
    }

    public static SchoolClass create(String name, int grade, String schoolYear, Instant now) {
        return new SchoolClass(ClassId.newId(), name, grade, schoolYear, now);
    }
}
