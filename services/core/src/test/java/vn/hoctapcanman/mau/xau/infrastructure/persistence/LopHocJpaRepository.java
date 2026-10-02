package vn.hoctapcanman.mau.xau.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import vn.hoctapcanman.mau.xau.infrastructure.persistence.entity.LopHocJpaEntity;

/** Vi phạm: quản lý lớp thiếu @Entity. */
public interface LopHocJpaRepository extends CrudRepository<LopHocJpaEntity, UUID> {}
