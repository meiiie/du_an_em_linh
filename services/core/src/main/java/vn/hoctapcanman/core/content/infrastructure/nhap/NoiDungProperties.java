package vn.hoctapcanman.core.content.infrastructure.nhap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.content.*}: thư mục nội dung ({@code source}, chứa {@code supham/} và {@code v0/}; ảnh core:
 * {@code /app/noi-dung}, máy dev: {@code ../../data}) và có nhập khi khởi động hay không ({@code import-on-startup}).
 * {@code source} là chuỗi, không phải {@code Path}: Spring đổi chuỗi sang {@code Path} qua tài nguyên của ngữ cảnh web, và
 * đường dẫn tương đối như {@code ../../data} thành tài nguyên servlet {@code /../../data}, hỏng khi khởi động. Chuỗi được
 * phân giải theo thư mục chạy ở {@link NguonNoiDung}.
 */
@ConfigurationProperties("app.content")
public record NoiDungProperties(@DefaultValue("/app/noi-dung") String source, @DefaultValue("false") boolean importOnStartup) {}
