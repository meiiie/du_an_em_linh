package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.content.*}: thư mục nội dung ({@code source}, chứa {@code supham/} và {@code v0/}; ảnh core:
 * {@code /app/noi-dung}, máy dev: {@code ../../data}) và có nhập khi khởi động hay không ({@code import-on-startup}).
 */
@ConfigurationProperties("app.content")
public record NoiDungProperties(@DefaultValue("/app/noi-dung") Path source, @DefaultValue("false") boolean importOnStartup) {}
