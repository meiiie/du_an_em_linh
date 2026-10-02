package vn.hoctoanai.core.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Định danh người dùng. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
