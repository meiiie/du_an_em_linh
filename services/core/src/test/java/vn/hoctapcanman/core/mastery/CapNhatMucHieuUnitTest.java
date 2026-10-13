package vn.hoctapcanman.core.mastery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.classroom.application.port.CanhBaoGiaoVien;
import vn.hoctapcanman.core.content.application.port.DanhMucKyNang;
import vn.hoctapcanman.core.mastery.application.service.CapNhatMucHieuSauKhiNop;
import vn.hoctapcanman.core.mastery.domain.model.*;
import vn.hoctapcanman.core.mastery.domain.repository.MasteryRepository;
import vn.hoctapcanman.core.practice.application.dto.*;
import vn.hoctapcanman.core.practice.application.dto.hocsinh.ViTriSai;

/** Chạy luồng nộp → BKT → cảnh báo thật, chỉ thay cổng lưu trữ và cảnh báo; không cần PostgreSQL. */
class CapNhatMucHieuUnitTest {
    final MasteryRepository repo = mock(MasteryRepository.class);
    final DanhMucKyNang catalog = mock(DanhMucKyNang.class);
    final CanhBaoGiaoVien alerts = mock(CanhBaoGiaoVien.class);
    final UUID student=UUID.randomUUID(), cl=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-14T08:00:00Z");
    final BktConfig config=new BktConfig(1,.12,.2,.1,.65,3,new BktConfig.Thresholds(.4,.62,.82));
    final CapNhatMucHieuSauKhiNop service=new CapNhatMucHieuSauKhiNop(repo,catalog,alerts);
    final AtomicReference<MasteryState> state=new AtomicReference<>(MasteryState.initial(student,"K1",config,now));
    final Map<UUID,MasteryEvent> events=new HashMap<>();
    final Map<String,MasteryState> states=new HashMap<>();
    CapNhatMucHieuUnitTest() {
        when(repo.config()).thenReturn(config);
        when(repo.recentEventsOf(eq(student),anyInt())).thenAnswer(i->events.values().stream()
            .sorted(java.util.Comparator.comparing(MasteryEvent::createdAt).reversed()).limit((int)i.getArgument(1)).toList());
        when(repo.eventOf(any())).thenAnswer(i->Optional.ofNullable(events.get(i.getArgument(0))));
        when(repo.lock(any())).thenAnswer(i->{MasteryState initial=i.getArgument(0);return states.computeIfAbsent(initial.skillCode(),key->initial);});
        when(catalog.kyNangCuaMaLoi("ERR1")).thenReturn(Optional.of("K1"));
        doAnswer(i->{MasteryState after=i.getArgument(0);states.put(after.skillCode(),after);if(after.skillCode().equals("K1"))state.set(after);MasteryEvent e=i.getArgument(1);events.put(e.submissionId(),e);return null;}).when(repo).save(any(),any());
    }
    BaiDaNop submission(KetQuaBai result,int n) {
        return new BaiDaNop(UUID.randomUUID(),student,cl,UUID.randomUUID(),"K1","NHAN_BIET",result,
            result==KetQuaBai.SAI?new ViTriSai("B.DH.XETDAU",null,null,null):null,
            result==KetQuaBai.SAI?"ERR1":null,.9,false,false,now.plusSeconds(n));
    }
    @Test void sameStepRepeatedToThresholdAlertsTeacherAndReplayDoesNotCountAgain() {
        service.sauKhiNop(submission(KetQuaBai.SAI,1));
        service.sauKhiNop(submission(KetQuaBai.SAI,2));
        verifyNoInteractions(alerts);
        var third=submission(KetQuaBai.SAI,3);
        var result=service.sauKhiNop(third);
        assertThat(state.get().stuck(config)).isTrue();
        assertThat(events.get(third.baiLamId()).wrongStep()).isEqualTo("B.DH.XETDAU");
        verify(alerts).ghiKet(eq(student),eq("K1"),isNull(),isNull(),contains("3 lượt"));
        assertThat(service.sauKhiNop(third)).isEqualTo(result);
        assertThat(state.get().attempts()).isEqualTo(3);
        verifyNoMoreInteractions(alerts);
    }
    @Test void failuresOnThreeDifferentStepsDoNotTriggerSameStepAlert() {
        int n=0;
        for (var step:List.of("B.DH.TXD","B.DH.DAOHAM","B.DH.NGHIEM")) {
            var b=submission(KetQuaBai.SAI,++n);
            service.sauKhiNop(new BaiDaNop(b.baiLamId(),b.hocSinhId(),b.lopId(),b.problemId(),b.kyNang(),b.mucDo(),b.ketQua(),
                new ViTriSai(step,null,null,null),b.maLoi(),b.doTinCay(),b.toanDung(),b.nghiDoanMo(),b.nopLuc()));
        }
        assertThat(state.get().stuckCounter()).isEqualTo(3); // BKT vẫn giữ đếm của v0.
        verifyNoInteractions(alerts);
    }
    @Test void stuckUsesConfiguredThresholdInsteadOfHardcodedThree() {
        when(repo.config()).thenReturn(new BktConfig(2,.12,.2,.1,.65,2,new BktConfig.Thresholds(.4,.62,.82)));
        service.sauKhiNop(submission(KetQuaBai.SAI,1)); verifyNoInteractions(alerts);
        service.sauKhiNop(submission(KetQuaBai.SAI,2));
        verify(alerts).ghiKet(eq(student),eq("K1"),isNull(),isNull(),contains("2 lượt"));
    }
    @Test void sameStepAcrossConfidenceBasedSkillMappingStillAlertsTeacher() {
        when(catalog.kyNangCuaBuoc("B.DH.XETDAU")).thenReturn(Optional.of("K2"));
        when(catalog.kyNangCuaMaLoi("ERR2")).thenReturn(Optional.of("K1"));
        service.sauKhiNop(submission(KetQuaBai.SAI,1));
        var second=submission(KetQuaBai.SAI,2);
        service.sauKhiNop(new BaiDaNop(second.baiLamId(),student,cl,second.problemId(),"K1","NHAN_BIET",KetQuaBai.SAI,
            second.buocSai(),"ERR1",.3,false,false,second.nopLuc()));
        verifyNoInteractions(alerts);
        var third=submission(KetQuaBai.SAI,3);
        service.sauKhiNop(new BaiDaNop(third.baiLamId(),student,cl,third.problemId(),"K1","NHAN_BIET",KetQuaBai.SAI,
            third.buocSai(),"ERR2",.9,false,false,third.nopLuc()));
        assertThat(states.get("K1").attempts()).isEqualTo(2);
        assertThat(states.get("K2").attempts()).isEqualTo(1);
        verify(alerts).ghiKet(eq(student),eq("K1"),isNull(),isNull(),contains("3 lượt"));
    }
    @Test void successResetsStuckCounter() {
        service.sauKhiNop(submission(KetQuaBai.SAI,1));
        service.sauKhiNop(submission(KetQuaBai.SAI,2));
        service.sauKhiNop(submission(KetQuaBai.DAT,3));
        assertThat(state.get().stuckCounter()).isZero();
        verifyNoInteractions(alerts);
    }
    @Test void unverifiedWorkDoesNotChangeMasteryOrTriggerStuckAlert() {
        assertThat(service.sauKhiNop(submission(KetQuaBai.KHONG_KIEM_DUOC,1))).isEmpty();
        assertThat(state.get().attempts()).isZero();
        verifyNoInteractions(alerts);
    }
}
