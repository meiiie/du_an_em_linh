package vn.hoctoanai.mau.xau.infrastructure.persistence;

import vn.hoctoanai.mau.xau.application.dto.HocSinhDto;
import vn.hoctoanai.mau.xau.domain.repository.HocSinhRepository;

/** Vi phạm: adapter của repository domain không kết thúc bằng Adapter. */
public class KhoHocSinh implements HocSinhRepository {
    @Override
    public HocSinhDto lay() {
        return new HocSinhDto("a");
    }
}
