package vn.hoctapcanman.mau.xau.infrastructure.persistence;

import vn.hoctapcanman.mau.xau.application.dto.HocSinhDto;
import vn.hoctapcanman.mau.xau.domain.repository.HocSinhRepository;

/** Vi phạm: adapter của repository domain không kết thúc bằng Adapter. */
public class KhoHocSinh implements HocSinhRepository {
    @Override
    public HocSinhDto lay() {
        return new HocSinhDto("a");
    }
}
