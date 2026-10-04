package vn.hoctapcanman.core.classroom.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.DanhSachLop;
import vn.hoctapcanman.core.classroom.domain.model.ClassId;
import vn.hoctapcanman.core.classroom.domain.repository.SchoolClassRepository;

@Service
@Transactional(readOnly = true)
public class DanhSachLopService implements DanhSachLop {

    private final SchoolClassRepository classes;

    public DanhSachLopService(SchoolClassRepository classes) {
        this.classes = classes;
    }

    @Override
    public List<UUID> moiLop() {
        return classes.findAllIds().stream().map(ClassId::value).toList();
    }
}
