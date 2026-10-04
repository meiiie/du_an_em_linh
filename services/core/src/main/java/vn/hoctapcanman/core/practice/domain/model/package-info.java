/**
 * Model của module practice (#87, specs/001-lat-cat-doc/data-model.md §practice): giao bài, bài làm theo khung bước, kết quả
 * chấm. Java thuần, không Spring / JPA. Mã trạng thái chấm giữ như v0 ({@code DAT}, {@code SAI}, {@code KHONG_KIEM_DUOC}),
 * thêm {@code KHONG_CHAM_DUOC} khi dịch vụ toán lỗi (không bao giờ là đạt, FR-009).
 */
@NullMarked
package vn.hoctapcanman.core.practice.domain.model;

import org.jspecify.annotations.NullMarked;
