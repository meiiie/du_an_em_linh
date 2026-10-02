package vn.hoctoanai.mau.tot.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import vn.hoctoanai.mau.tot.domain.model.HocSinh;
import vn.hoctoanai.mau.tot.domain.repository.HocSinhRepository;

public class HocSinhRepositoryAdapter implements HocSinhRepository {
    @Override
    public Optional<HocSinh> tim(UUID id) {
        return Optional.empty();
    }
}
