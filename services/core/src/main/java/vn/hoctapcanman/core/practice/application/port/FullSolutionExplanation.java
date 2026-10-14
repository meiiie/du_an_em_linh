package vn.hoctapcanman.core.practice.application.port;
import java.util.*;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
/** Called only after the explicit reveal gate. Input contains the published exercise, never teacher solutions. */
public interface FullSolutionExplanation {
    Optional<String> explain(UUID classId,BaiChoLamBai problem);
}
