package vn.hoctapcanman.core.tutor.application.dto.hocsinh;

/** Không chứa định danh hoặc dữ kiện bảo vệ. Không ghi nội dung prompt vào log. */
public record HintPrompt(String system, String user) {
    @Override public String toString() { return "HintPrompt[đã ẩn nội dung]"; }
}
