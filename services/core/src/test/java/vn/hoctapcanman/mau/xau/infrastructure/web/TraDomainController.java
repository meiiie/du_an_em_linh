package vn.hoctapcanman.mau.xau.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctapcanman.mau.xau.domain.model.HocSinh;

/** Vi phạm: controller dùng model domain (record nhưng không ở application.dto). */
@RestController
public class TraDomainController {
    @GetMapping("/domain")
    public ResponseEntity<HocSinh> lay() {
        return ResponseEntity.ok(new HocSinh("a"));
    }
}
