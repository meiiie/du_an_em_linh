package vn.hoctapcanman.mau.tot.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import vn.hoctapcanman.mau.tot.infrastructure.persistence.entity.HocSinhJpaEntity;

public interface HocSinhJpaRepository extends CrudRepository<HocSinhJpaEntity, UUID> {}
