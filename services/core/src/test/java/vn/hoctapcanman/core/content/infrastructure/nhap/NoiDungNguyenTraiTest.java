package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.Solution;
import vn.hoctapcanman.core.content.domain.repository.*;

class NoiDungNguyenTraiTest {
    @Test void nguonTruongKhongDungMaySinhVaGhiLoiGiaiDayDuMotLan() {
        var problems=mock(ProblemRepository.class);var solutions=mock(SolutionRepository.class);
        var catalog=mock(TopicCatalogRepository.class);var hints=mock(HintLevelRepository.class);
        var math=mock(GiaiToan.class);var manager=mock(PlatformTransactionManager.class);
        var saved=new HashMap<String,Problem>();
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        when(problems.findByCode(any())).thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0))));
        doAnswer(i -> {Problem p=i.getArgument(0);saved.put(p.code(),p);return null;}).when(problems).save(any());
        var source=new NguonNoiDung(new NoiDungProperties(Path.of("../../data").toString(),true));
        var importer=new NhapNoiDungChung(source,math,catalog,problems,solutions,hints,manager,mock(ApplicationEventPublisher.class),Clock.systemUTC());
        var first=importer.nhapTruong();
        assertThat(first.bai()).hasSize(74);
        assertThat(saved.values()).filteredOn(Problem::isFiveStep).hasSize(10);
        assertThat(saved.values()).filteredOn(p -> !p.isFiveStep()).hasSize(64);
        var skills=ArgumentCaptor.forClass(vn.hoctapcanman.core.content.domain.model.Skill.class);
        verify(catalog,atLeastOnce()).saveSkill(skills.capture());
        assertThat(skills.getAllValues()).extracting(vn.hoctapcanman.core.content.domain.model.Skill::name)
            .contains("Cực trị hàm bậc ba", "Thể tích khối chóp", "Cấp số cộng");
        var records=ArgumentCaptor.forClass(Solution.class);
        verify(solutions,times(74)).save(records.capture());
        assertThat(records.getAllValues()).allSatisfy(s -> {
            assertThat(s.finalAnswer()).isNotBlank();assertThat(s.workedSolutionJson()).isNotBlank();
        });
        importer.nhapTruong();
        verify(problems,times(74)).save(any()); // Lần đọc thứ hai không sửa nội dung.
        verifyNoInteractions(math);
        assertThat(first.ketQua().bai()).doesNotHaveDuplicates();
    }
}
