package vn.hoctapcanman.mau.tot.application.port;

import java.util.Optional;
import java.util.UUID;

/** Cổng module «học sinh» mở cho module khác. */
public interface DanhBaHocSinh {

    Optional<String> ten(UUID id);
}
