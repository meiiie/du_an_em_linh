/**
 * Model của nội dung chủ đề (#85, specs/001-lat-cat-doc/data-model.md §content): bài, lời giải, thang gợi ý, trạng thái
 * phát hành theo lớp, bảng công thức có phiên bản, lượt kiểm 3 tầng gắn lớp và bảng đã dùng. Java thuần, không Spring / JPA.
 * Mã trạng thái giữ như v0 ({@code DAT}, {@code SAI}, {@code KHONG_KIEM_DUOC}, {@code GV_DUYET}; {@code NHAP},
 * {@code DA_PHAT_HANH}…) để đối chiếu được.
 */
@NullMarked
package vn.hoctapcanman.core.content.domain.model;

import org.jspecify.annotations.NullMarked;
