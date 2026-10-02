package vn.hoctoanai.mau.tot.application.usecase;

import java.util.Optional;
import java.util.UUID;
import vn.hoctoanai.mau.tot.application.dto.HocSinhDto;
import vn.hoctoanai.mau.tot.infrastructure.persistence.HocSinhJpaRepository;

/** Use case đọc được đọc thẳng persistence (ngoại lệ CQRS). */
public class GetHocSinhUseCase {
    private final HocSinhJpaRepository bang;

    public GetHocSinhUseCase(HocSinhJpaRepository bang) {
        this.bang = bang;
    }

    public Optional<HocSinhDto> thucHien(UUID id) {
        return bang.findById(id).map(entity -> new HocSinhDto("a"));
    }
}
