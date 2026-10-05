package vn.hoctapcanman.core.practice.application.dto;

/** Mức hiểu của một kỹ năng trước và sau bài vừa nộp; mức là mã 4 mức ({@code NHAN_BIET} … {@code VAN_DUNG_CAO}). */
public record ThayDoiMucHieu(String kyNang, String muc4Truoc, String muc4Sau) {}
