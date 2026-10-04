package vn.hoctapcanman.core.practice.application.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Nộp một bước ({@code POST …/buoc}): mã bước, các dòng (bước kiểu dòng), các ô (bước kiểu bảng, loại bảng luôn là
 * {@code XET_DAU} như v0, không lấy từ máy học sinh), sự kiện nhập mới kể từ lần nộp trước. Nộp lại cùng bước thì thay
 * trọn nội dung bước đó.
 */
public record NopBuocRequest(String maBuoc, @Nullable List<DongNop> dong, @Nullable List<ONop> bang, @Nullable List<SuKienNop> suKien) {}
