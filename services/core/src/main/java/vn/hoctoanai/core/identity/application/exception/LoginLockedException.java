package vn.hoctoanai.core.identity.application.exception;

import java.time.Duration;

/** Đã sai mật khẩu đủ ngưỡng trong cửa sổ: tạm khóa đăng nhập theo email + IP, kể cả khi lần này đúng mật khẩu. */
public class LoginLockedException extends RuntimeException {

    /** Như v0 ({@code apps/web/components/login-form.tsx}). */
    public static final String THONG_BAO =
            "Sai mật khẩu quá nhiều lần (5 lần). Tài khoản này tạm khóa 15 phút trên máy này, em thử lại sau.";

    private final Duration thuLaiSau;

    public LoginLockedException(Duration thuLaiSau) {
        super(THONG_BAO);
        this.thuLaiSau = thuLaiSau;
    }

    public Duration thuLaiSau() {
        return thuLaiSau;
    }
}
