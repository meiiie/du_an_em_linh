package vn.hoctoanai.mau.tot.domain.repository;

import java.util.Optional;
import java.util.UUID;
import vn.hoctoanai.mau.tot.domain.model.HocSinh;

public interface HocSinhRepository {
    Optional<HocSinh> tim(UUID id);
}
