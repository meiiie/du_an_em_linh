package vn.hoctapcanman.core.identity.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

class TaiKhoanNguyenTraiSeederTest {
    @Test void thieuHoacTrangThiKhongTaoTaiKhoan() {
        for (String value:new String[]{"", " "}) {
            var users=mock(UserRepository.class); var hasher=mock(PasswordHasher.class);
            var env=new MockEnvironment().withProperty("MATHL_DEV_SEED_PASSWORD",value);
            new TaiKhoanNguyenTraiSeeder(users,hasher,Clock.systemUTC(),env).run(new DefaultApplicationArguments());
            verifyNoInteractions(users,hasher);
        }
        var users=mock(UserRepository.class);var hasher=mock(PasswordHasher.class);
        new TaiKhoanNguyenTraiSeeder(users,hasher,Clock.systemUTC(),new MockEnvironment()).run(new DefaultApplicationArguments());
        verifyNoInteractions(users,hasher);
    }
    @Test void tao35TaiKhoanTongHopVaChayLaiKhongDoiMatKhau() {
        var users=mock(UserRepository.class);var hasher=mock(PasswordHasher.class);
        var saved=new HashMap<Email,User>();
        when(users.findByEmail(any())).thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0))));
        when(users.save(any())).thenAnswer(i -> {User u=i.getArgument(0);saved.put(u.email(),u);return u;});
        when(hasher.hash(any())).thenReturn("hash-in-memory");
        String ephemeral=UUID.randomUUID().toString();
        var seed=new TaiKhoanNguyenTraiSeeder(users,hasher,Clock.systemUTC(),new MockEnvironment().withProperty("MATHL_DEV_SEED_PASSWORD",ephemeral));
        seed.run(new DefaultApplicationArguments());seed.run(new DefaultApplicationArguments());
        assertThat(saved).hasSize(35);
        assertThat(saved.values()).allSatisfy(u -> assertThat(u.synthetic()).isTrue());
        assertThat(saved.values()).filteredOn(u -> u.role().name().equals("STUDENT")).hasSize(30);
        assertThat(saved.values()).filteredOn(u -> u.role().name().equals("TEACHER")).hasSize(4);
        verify(hasher,times(35)).hash(ephemeral);verify(users,times(35)).save(any());
    }
    @Test void taiKhoanTrungKhongBiNangQuyen() {
        var users=mock(UserRepository.class);var hasher=mock(PasswordHasher.class);
        when(users.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        new TaiKhoanNguyenTraiSeeder(users,hasher,Clock.systemUTC(),new MockEnvironment().withProperty("MATHL_DEV_SEED_PASSWORD",UUID.randomUUID().toString()))
            .run(new DefaultApplicationArguments());
        verify(users,never()).save(any());verifyNoInteractions(hasher);
    }
}
