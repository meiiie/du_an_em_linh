package vn.hoctoanai.mau.xau.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import vn.hoctoanai.mau.xau.domain.model.HocSinh;

/** Vi phạm: repository Spring Data quản lý model domain («Not a managed type»). */
public interface HocSinhJpaRepository extends CrudRepository<HocSinh, UUID> {}
