package vn.hoctoanai.core.identity.application.usecase;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctoanai.core.identity.application.dto.UserDto;
import vn.hoctoanai.core.identity.application.exception.AuthenticationFailedException;
import vn.hoctoanai.core.identity.domain.model.User;
import vn.hoctoanai.core.identity.domain.model.UserId;
import vn.hoctoanai.core.identity.domain.repository.UserRepository;

/** Người dùng của access token hiện tại. Chỉ trả chính người đó, không nhận id từ đường dẫn (chống IDOR). */
@Service
public class GetCurrentUserUseCase {

    private final UserRepository users;

    public GetCurrentUserUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserDto execute(UUID userId) {
        return users.findById(new UserId(userId))
                .filter(User::enabled)
                .map(UserDto::from)
                .orElseThrow(() -> new AuthenticationFailedException(AuthenticationFailedException.PHIEN_HET_HAN));
    }
}
