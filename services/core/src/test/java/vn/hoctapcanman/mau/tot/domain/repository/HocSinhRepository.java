package vn.hoctapcanman.mau.tot.domain.repository;

import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.mau.tot.domain.model.HocSinh;

public interface HocSinhRepository {
    Optional<HocSinh> tim(UUID id);
}
