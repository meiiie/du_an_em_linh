package vn.hoctapcanman.core.identity.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Email đăng nhập: bỏ khoảng trắng hai đầu, chữ thường; một «@» và tên miền có dấu chấm. */
public record Email(String value) {

    private static final int DAI_TOI_DA = 254;
    private static final Pattern DANG = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public Email {
        Objects.requireNonNull(value, "value");
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > DAI_TOI_DA || !DANG.matcher(value).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
    }

    @Override
    public String toString() {
        return "Email[***]";
    }
}
