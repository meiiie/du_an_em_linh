package vn.hoctapcanman.mau.xau.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vi phạm: endpoint trả String, không phải record DTO. */
@RestController
public class TraChuoiController {
    @GetMapping("/chuoi")
    public String lay() {
        return "";
    }
}
