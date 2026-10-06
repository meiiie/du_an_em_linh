package vn.hoctapcanman.core.identity.application.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.identity.application.port.UserDirectory;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.model.UserId;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/** Hiện thực {@link UserDirectory} trên kho người dùng của identity. */
@Service
@Transactional(readOnly = true)
public class UserDirectoryService implements UserDirectory {

    private final UserRepository users;

    public UserDirectoryService(UserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserSummary> findByEmail(String email) {
        Email hopLe;
        try {
            hopLe = new Email(email);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return users.findByEmail(hopLe).map(UserDirectoryService::tomTat);
    }

    @Override
    public Optional<UserSummary> findById(UUID id) {
        return users.findById(new UserId(id)).map(UserDirectoryService::tomTat);
    }

    @Override
    public Optional<String> tenHienThi(UUID id) {
        return users.findById(new UserId(id)).map(User::displayName);
    }

    private static UserSummary tomTat(User user) {
        return new UserSummary(user.id().value(), user.role().name(), user.synthetic());
    }
}
