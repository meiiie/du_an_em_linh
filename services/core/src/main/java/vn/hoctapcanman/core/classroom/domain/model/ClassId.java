package vn.hoctapcanman.core.classroom.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Định danh lớp. */
public record ClassId(UUID value) {

    public ClassId {
        Objects.requireNonNull(value, "value");
    }

    public static ClassId newId() {
        return new ClassId(UUID.randomUUID());
    }
}
