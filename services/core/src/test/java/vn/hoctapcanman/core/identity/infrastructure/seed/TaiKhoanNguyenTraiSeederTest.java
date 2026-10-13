package vn.hoctapcanman.core.identity.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

class TaiKhoanNguyenTraiSeederTest {
    @TempDir Path temp;

    private MockEnvironment environment() {
        return new MockEnvironment().withProperty("MATHL_DEV_SEED_PASSWORD_FILE", temp.resolve("password").toString());
    }

    @Test void thieuHoacTrangThiSinhMatKhauNgauNhienVaTaiSuDung() throws IOException {
        for (String value : new String[]{"", " ", "absent"}) {
            Files.deleteIfExists(temp.resolve("password"));
            var users = mock(UserRepository.class);
            var hasher = mock(PasswordHasher.class);
            when(users.findByEmail(any())).thenReturn(Optional.empty());
            when(hasher.hash(any())).thenReturn("hash-in-memory");
            var env = environment();
            if (!value.equals("absent")) env.withProperty("MATHL_DEV_SEED_PASSWORD", value);
            var seed = new TaiKhoanNguyenTraiSeeder(users, hasher, Clock.systemUTC(), env);
            seed.run(new DefaultApplicationArguments());
            String generated = Files.readString(temp.resolve("password"));
            assertThat(generated).matches("[A-Za-z0-9_-]{43}");
            assertThat(Files.getPosixFilePermissions(temp.resolve("password")))
                .isEqualTo(PosixFilePermissions.fromString("rw-------"));
            var passwords = ArgumentCaptor.forClass(String.class);
            verify(hasher, times(35)).hash(passwords.capture());
            assertThat(passwords.getAllValues()).containsOnly(generated);
            seed.run(new DefaultApplicationArguments());
            assertThat(Files.readString(temp.resolve("password"))).isEqualTo(generated);
        }
    }

    @Test void khongLuuDuocMatKhauThiBoQuaTaiKhoan() {
        var users = mock(UserRepository.class);
        var hasher = mock(PasswordHasher.class);
        var env = environment().withProperty("MATHL_DEV_SEED_PASSWORD_FILE", temp.resolve("missing/password").toString());
        new TaiKhoanNguyenTraiSeeder(users, hasher, Clock.systemUTC(), env).run(new DefaultApplicationArguments());
        verifyNoInteractions(users, hasher);
    }

    @Test void khongGhiQuaLienKetTuongTrung() throws IOException {
        Path target = Files.createFile(temp.resolve("target"));
        Files.createSymbolicLink(temp.resolve("password"), target);
        var users = mock(UserRepository.class);
        var hasher = mock(PasswordHasher.class);
        new TaiKhoanNguyenTraiSeeder(users, hasher, Clock.systemUTC(), environment()).run(new DefaultApplicationArguments());
        verifyNoInteractions(users, hasher);
        assertThat(Files.readString(target)).isEmpty();
    }

    @Test void tao35TaiKhoanTongHopVaChayLaiKhongDoiMatKhau() throws IOException {
        var users=mock(UserRepository.class);var hasher=mock(PasswordHasher.class);
        var saved=new HashMap<Email,User>();
        when(users.findByEmail(any())).thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0))));
        when(users.save(any())).thenAnswer(i -> {User u=i.getArgument(0);saved.put(u.email(),u);return u;});
        when(hasher.hash(any())).thenReturn("hash-in-memory");
        String ephemeral=UUID.randomUUID().toString();
        var seed=new TaiKhoanNguyenTraiSeeder(users,hasher,Clock.systemUTC(),environment().withProperty("MATHL_DEV_SEED_PASSWORD",ephemeral));
        seed.run(new DefaultApplicationArguments());seed.run(new DefaultApplicationArguments());
        assertThat(Files.readString(temp.resolve("password"))).isEqualTo(ephemeral);
        assertThat(Files.getPosixFilePermissions(temp.resolve("password")))
            .isEqualTo(PosixFilePermissions.fromString("rw-------"));
        assertThat(saved).hasSize(35);
        assertThat(saved.values()).allSatisfy(u -> assertThat(u.synthetic()).isTrue());
        assertThat(saved.values()).filteredOn(u -> u.role().name().equals("STUDENT")).hasSize(30);
        assertThat(saved.values()).filteredOn(u -> u.role().name().equals("TEACHER")).hasSize(4);
        verify(hasher,times(35)).hash(ephemeral);verify(users,times(35)).save(any());
    }
    @Test void taiKhoanTrungKhongBiNangQuyen() {
        var users=mock(UserRepository.class);var hasher=mock(PasswordHasher.class);
        when(users.findByEmail(any())).thenReturn(Optional.of(mock(User.class)));
        new TaiKhoanNguyenTraiSeeder(users,hasher,Clock.systemUTC(),environment().withProperty("MATHL_DEV_SEED_PASSWORD",UUID.randomUUID().toString()))
            .run(new DefaultApplicationArguments());
        verify(users,never()).save(any());verifyNoInteractions(hasher);
    }
}
