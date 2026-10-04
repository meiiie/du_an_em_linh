/**
 * DTO học sinh thấy (FR-006, ADR 003): đề và cách làm, không bao giờ lời giải, đáp án hay dữ kiện bảo vệ. Gói này chỉ
 * được dùng kiểu Java và DTO học sinh khác (ArchUnit {@code DTO_HOC_SINH_KHONG_MANG_LOI_GIAI}), nên không thể vô tình thêm
 * trường kiểu {@code Solution} hay bất kỳ kiểu domain nào.
 */
@NullMarked
package vn.hoctapcanman.core.content.application.dto.hocsinh;

import org.jspecify.annotations.NullMarked;
