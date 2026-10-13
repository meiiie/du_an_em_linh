package vn.hoctapcanman.core.classroom.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import vn.hoctapcanman.core.classroom.domain.model.*;
import vn.hoctapcanman.core.classroom.domain.repository.*;
import vn.hoctapcanman.core.identity.application.port.UserDirectory;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;

class LopNguyenTraiSeederTest {
    @Test void tenLopTrungKhacIdThiKhongGhiDanhHoacSuaCaiDat() {
        var classes=mock(SchoolClassRepository.class);var enrollments=mock(EnrollmentRepository.class);
        var settings=mock(ClassSettingsRepository.class);var users=mock(UserDirectory.class);
        when(classes.findByNameAndSchoolYear(any(),any())).thenReturn(Optional.of(SchoolClass.create("12A3",12,"2026-2027",Instant.now())));
        new LopNguyenTraiSeeder(classes,enrollments,settings,users,Clock.systemUTC()).run(new DefaultApplicationArguments());
        verify(classes,never()).save(any());verifyNoInteractions(enrollments,settings,users);
    }
    @Test void khongCoTaiKhoanThiKhongTaoLopRong() {
        var classes=mock(SchoolClassRepository.class);var enrollments=mock(EnrollmentRepository.class);
        var settings=mock(ClassSettingsRepository.class);var users=mock(UserDirectory.class);
        when(classes.findByNameAndSchoolYear(any(),any())).thenReturn(Optional.empty());
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        new LopNguyenTraiSeeder(classes,enrollments,settings,users,Clock.systemUTC()).run(new DefaultApplicationArguments());
        verify(classes,never()).save(any());verifyNoInteractions(enrollments,settings);
    }
    @Test void thanhVienKhongTongHopThiKhongBoSungLop() {
        var classes=mock(SchoolClassRepository.class);var enrollments=mock(EnrollmentRepository.class);
        var settings=mock(ClassSettingsRepository.class);var users=mock(UserDirectory.class);
        UUID real=UUID.randomUUID();
        when(classes.findByNameAndSchoolYear(any(),any())).thenAnswer(i -> {
            String name=i.getArgument(0);
            return Optional.of(new SchoolClass(new ClassId(TruongNguyenTrai.id("lop:"+name)),name,Integer.parseInt(name.substring(0,2)),TruongNguyenTrai.NAM_HOC,Instant.now()));
        });
        when(enrollments.findByClass(any(),any())).thenAnswer(i -> List.of(new Enrollment(i.getArgument(0),real,i.getArgument(1),Instant.now())));
        when(users.findById(real)).thenReturn(Optional.of(new UserDirectory.UserSummary(real,"STUDENT",false)));
        new LopNguyenTraiSeeder(classes,enrollments,settings,users,Clock.systemUTC()).run(new DefaultApplicationArguments());
        verify(classes,never()).save(any());verify(enrollments,never()).save(any());verifyNoInteractions(settings);
    }
}
