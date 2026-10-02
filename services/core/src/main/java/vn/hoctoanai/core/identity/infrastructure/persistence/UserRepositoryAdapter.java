package vn.hoctoanai.core.identity.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import vn.hoctoanai.core.identity.domain.model.Email;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.model.UserId;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;
import vn.hoctoanai.core.identity.infrastructure.persistence.entity.UserJpaEntity;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(UserJpaEntity::toDomain);
    }

    @Override
    public User save(User user) {
        return jpa.save(UserJpaEntity.from(user)).toDomain();
    }
}
