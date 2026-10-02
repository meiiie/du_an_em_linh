package vn.hoctapcanman.mau.xau.infrastructure.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vi phạm: endpoint trả Map. */
@RestController
public class TraMapController {
    @GetMapping("/map")
    public Map<String, Object> lay() {
        return Map.of();
    }
}
