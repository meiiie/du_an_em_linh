package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;

/**
 * Bài đang phát hành ở một lớp cùng phiên bản nội dung ({@code problems.content_version}, V5) và chủ đề của kỹ năng chính
 * (để lấy khung bước), đọc trong một câu lệnh: bài làm của học sinh gắn đúng phiên bản này, nội dung đổi thì bài làm cũ
 * thôi được chấm.
 */
public record ReleasedProblem(Problem problem, int contentVersion, String topicCode) {

    public ReleasedProblem {
        Objects.requireNonNull(problem, "problem");
        Objects.requireNonNull(topicCode, "topicCode");
        if (contentVersion < 1) {
            throw new IllegalArgumentException("contentVersion phải dương");
        }
    }
}
