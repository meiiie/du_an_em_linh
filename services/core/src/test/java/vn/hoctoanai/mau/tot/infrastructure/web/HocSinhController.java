package vn.hoctoanai.mau.tot.infrastructure.web;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctoanai.mau.tot.application.dto.HocSinhDto;
import vn.hoctoanai.mau.tot.application.dto.TaoHocSinhDto;
import vn.hoctoanai.mau.tot.application.usecase.GetHocSinhUseCase;
import vn.hoctoanai.mau.tot.application.usecase.TaoHocSinhUseCase;

@RestController
@RequestMapping("/hoc-sinh")
public class HocSinhController {
    private final TaoHocSinhUseCase tao;
    private final GetHocSinhUseCase lay;

    public HocSinhController(TaoHocSinhUseCase tao, GetHocSinhUseCase lay) {
        this.tao = tao;
        this.lay = lay;
    }

    @GetMapping
    public ResponseEntity<List<HocSinhDto>> danhSach() {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public Optional<HocSinhDto> mot(@PathVariable UUID id) {
        return lay.thucHien(id);
    }

    @PostMapping
    public HocSinhDto tao(@RequestBody TaoHocSinhDto lenh) {
        return tao.thucHien(lenh);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> xoa(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }
}
