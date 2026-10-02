package vn.hoctoanai.mau.tot.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class HocSinhJpaEntity {
    @Id
    private UUID id;
}
