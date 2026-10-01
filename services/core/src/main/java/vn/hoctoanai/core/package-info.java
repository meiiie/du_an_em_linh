/**
 * Dịch vụ nghiệp vụ v2 (ADR 011). Mỗi module nghiệp vụ là một gói con chia ba tầng
 * {@code domain} → {@code application} → {@code infrastructure}; luật tầng do ArchUnit kiểm.
 *
 * <p>Mọi gói đánh dấu {@link org.jspecify.annotations.NullMarked}: kiểu mặc định không null,
 * giá trị có thể null phải khai {@code @Nullable}. {@code NullMarkedPackagesTest} chặn gói thiếu.
 */
@NullMarked
package vn.hoctoanai.core;

import org.jspecify.annotations.NullMarked;
