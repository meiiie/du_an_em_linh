package vn.hoctapcanman.mau.xau.infrastructure.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Vi phạm: @RequestBody là String. */
@RestController
public class NhanChuoiController {
    @PostMapping("/nhan")
    public void tao(@RequestBody String noiDung) {}
}
