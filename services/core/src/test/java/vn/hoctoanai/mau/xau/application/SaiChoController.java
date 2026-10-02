package vn.hoctoanai.mau.xau.application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctoanai.mau.xau.application.dto.HocSinhDto;

/** Vi phạm: @RestController nằm ngoài infrastructure.web. */
@RestController
public class SaiChoController {
    @GetMapping("/sai-cho")
    public HocSinhDto lay() {
        return new HocSinhDto("a");
    }
}
