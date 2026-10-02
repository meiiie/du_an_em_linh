package vn.hoctapcanman.mau.tot.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.mau.tot.domain.model.HocSinh;
import vn.hoctapcanman.mau.tot.domain.repository.HocSinhRepository;

public class HocSinhRepositoryAdapter implements HocSinhRepository {
    @Override
    public Optional<HocSinh> tim(UUID id) {
        return Optional.empty();
    }
}
