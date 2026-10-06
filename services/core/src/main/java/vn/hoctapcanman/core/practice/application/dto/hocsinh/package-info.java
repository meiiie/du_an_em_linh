/**
 * DTO học sinh thấy khi làm bài (FR-006, ADR 003): danh sách bài, đề và bài làm của chính em, kết quả chấm một bước. Không có
 * giá trị đúng, lời giải hay dữ kiện bảo vệ. Gói chỉ được dùng kiểu Java và DTO học sinh khác (ArchUnit
 * {@code DTO_HOC_SINH_KHONG_MANG_LOI_GIAI}), nên không thể vô tình thêm trường kiểu domain. Lời giải sau khi nộp nằm ở
 * {@code KetQuaNopBai}, ngoài gói này.
 */
@NullMarked
package vn.hoctapcanman.core.practice.application.dto.hocsinh;

import org.jspecify.annotations.NullMarked;
