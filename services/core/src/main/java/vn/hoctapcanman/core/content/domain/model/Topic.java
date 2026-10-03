package vn.hoctapcanman.core.content.domain.model;

import java.util.Objects;

/** Chủ đề. Mã giữ như v0 ({@code DH12}), để đối chiếu với {@code data/v0} và tệp vàng của v0. */
public record Topic(String code, String name, int grade) {

    public Topic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Kiem.ma(code, 32, "chủ đề");
        Kiem.toiDa(Kiem.khongTrong(name, "Tên chủ đề"), 300, "Tên chủ đề");
        if (grade < 1 || grade > 12) {
            throw new IllegalArgumentException("Khối lớp ngoài 1–12");
        }
    }
}
