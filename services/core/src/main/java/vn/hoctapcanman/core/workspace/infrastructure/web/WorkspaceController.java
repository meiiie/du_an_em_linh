package vn.hoctapcanman.core.workspace.infrastructure.web;

import java.io.IOException;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.service.WorkspaceService;

@RestController public class WorkspaceController {
    private final WorkspaceService service;
    public WorkspaceController(WorkspaceService service) {
        this.service=service;
    }

    @PostMapping("/api/gv/soan-nhap") public Result draft(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody DraftRequest request) {
        return service.draft(actor(jwt),request);
    }

    @GetMapping("/api/hs/chu-de") public List<Item> topics(@AuthenticationPrincipal Jwt jwt) {return service.topics(actor(jwt));}
    @GetMapping("/api/hs/tu-hoc") public List<Item> selfStudy(@AuthenticationPrincipal Jwt jwt,@RequestParam String chuDe) {return service.selfStudy(actor(jwt),chuDe);}
    @PostMapping("/api/hs/tu-hoc/{code}") public Result chooseStudy(@AuthenticationPrincipal Jwt jwt,@PathVariable String code) {return service.chooseStudy(actor(jwt),code);}

    private UUID actor(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @GetMapping("/api/gv/lop") public List<Item> classes(@AuthenticationPrincipal Jwt jwt) {
        return service.classes(actor(jwt));
    }

    @GetMapping("/api/gv/canh-bao") public List<Item> stuckAlerts(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.stuckAlerts(actor(jwt),lopId);
    }

    @GetMapping("/api/hs/ke-hoach-tuan") public WeeklyPlan weeklyPlan(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int tuan) {
        return service.weeklyPlan(actor(jwt),tuan);
    }

    @GetMapping("/api/gv/tong-quan") public Dashboard dashboard(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.dashboard(actor(jwt),lopId);
    }

    @GetMapping("/api/gv/tien-do") public List<Item> students(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.students(actor(jwt),lopId);
    }

    @GetMapping("/api/gv/hoc-sinh/{id}") public Student student(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam UUID lopId) {
        return service.student(actor(jwt),lopId,id);
    }

    @PostMapping("/api/gv/muc-hieu") public Result override(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody MasteryOverride request) {
        return service.override(actor(jwt),request);
    }

    @PostMapping("/api/gv/ngan-hang") public Result createProblem(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody NewProblem request) {
        return service.createProblem(actor(jwt),request);
    }

    @PostMapping("/api/gv/goi-bai") public Result recommend(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody NextProblem request) {
        return service.recommend(actor(jwt),request);
    }

    @GetMapping("/api/gv/ky-nang") public List<Item> skills(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.skillCatalog(actor(jwt),lopId);
    }

    @GetMapping("/api/gv/ngan-hang") public org.springframework.http.ResponseEntity<List<Item>> bank(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId,
        @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String level4,
        @RequestParam(defaultValue="") String skillCode,@RequestParam(defaultValue="") String gate,
        @RequestParam(defaultValue="-1") int page,@RequestParam(defaultValue="20") int size) {
        if(page==-1)return org.springframework.http.ResponseEntity.ok(service.bank(actor(jwt),lopId,q,level4,skillCode,gate));
        var result=service.bankPage(actor(jwt),lopId,q,level4,skillCode,gate,page,size);
        return org.springframework.http.ResponseEntity.ok().header("X-Total-Count",Long.toString(result.total())).body(result.items());
    }

    @GetMapping("/api/gv/giao") public List<Item> assignments(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.assignments(actor(jwt),lopId);
    }

    @PostMapping("/api/gv/giao") public Result assign(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Assign request) {
        return service.assign(actor(jwt),request);
    }

    @GetMapping("/api/hs/che-do") public List<Item> modes(@AuthenticationPrincipal Jwt jwt) {
        return service.modes(actor(jwt));
    }

    @GetMapping("/api/hs/goi-bai") public List<Item> recommend(@AuthenticationPrincipal Jwt jwt) {
        return service.recommendations(actor(jwt));
    }

    @GetMapping("/api/hs/kho") public Library library(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="") String q) {
        return service.library(actor(jwt),q);
    }

    @GetMapping("/api/gv/kho") public Library teacherLibrary(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId,@RequestParam(defaultValue="") String q) {
        return service.library(actor(jwt),lopId,q);
    }

    @GetMapping("/api/hs/flashcards/export") public Export export(@AuthenticationPrincipal Jwt jwt) {
        return service.export(actor(jwt));
    }

    @GetMapping(value="/api/hs/flashcards/export.csv",produces="text/csv;charset=UTF-8")
    public void download(@AuthenticationPrincipal Jwt jwt,jakarta.servlet.http.HttpServletResponse response)throws IOException {
        Export file=service.export(actor(jwt));
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=\""+file.filename()+"\"");
        response.getOutputStream().write(file.content().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/api/hs/cong-thuc") public List<Item> studentFormulas(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="") String q) {
        return service.library(actor(jwt),q).formulas();
    }

    @GetMapping("/api/gv/tai-lieu") public List<Item> documents(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId,@RequestParam(defaultValue="") String q) {
        return service.library(actor(jwt),lopId,q).documents();
    }

    @PostMapping(value="/api/gv/tai-lieu",consumes="multipart/form-data") public Result upload(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId,@RequestParam String title,@RequestParam(defaultValue="tu_soan") String licenseStatus,@RequestParam MultipartFile file)throws IOException {
        return service.upload(actor(jwt),lopId,title,licenseStatus,Objects.requireNonNullElse(file.getOriginalFilename(),""),file.getBytes());
    }

    @GetMapping("/api/gv/cong-thuc") public List<Item> formulas(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId,@RequestParam(defaultValue="") String q) {
        return service.library(actor(jwt),lopId,q).formulas();
    }

    @PostMapping("/api/gv/cong-thuc") public Result formula(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Formula request) {
        return service.formula(actor(jwt),request);
    }

    @PostMapping("/api/gv/cong-thuc/kiem") public Result check(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.checkFormulas(actor(jwt),lopId);
    }

    @GetMapping("/api/gv/duyet") public List<Item> reviews(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.reviews(actor(jwt),lopId);
    }

    @PostMapping("/api/gv/duyet/{id}") public Result review(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Review request) {
        return service.review(actor(jwt),id,request);
    }

    @GetMapping("/api/gv/cai-dat") public Settings settings(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID lopId) {
        return service.settings(actor(jwt),lopId);
    }

    @PutMapping("/api/gv/cai-dat") public Result settings(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Settings request) {
        return service.settings(actor(jwt),request);
    }

    @GetMapping("/api/hs/lich") public List<Item> schedule(@AuthenticationPrincipal Jwt jwt) {
        return service.schedule(actor(jwt));
    }

    @PostMapping("/api/hs/lich") public Result schedule(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Schedule request) {
        return service.schedule(actor(jwt),request);
    }

    @PostMapping("/api/hs/lich/{id}/xong") public Result complete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        return service.complete(actor(jwt),id);
    }

    @GetMapping("/api/hs/nhac") public List<Item> reminders(@AuthenticationPrincipal Jwt jwt) {
        return service.reminders(actor(jwt));
    }

    @GetMapping("/api/qt/nguoi-dung") public List<Item> users(@AuthenticationPrincipal Jwt jwt) {
        return service.users(actor(jwt));
    }

    @PostMapping("/api/qt/nguoi-dung") public Result user(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody NewUser request) {
        return service.createUser(actor(jwt),request);
    }

    @PostMapping("/api/qt/nguoi-dung/bulk") public Result bulk(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Bulk request) {
        return service.bulk(actor(jwt),request);
    }

    @PutMapping("/api/qt/nguoi-dung/{id}/khoa") public Result lockUser(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Lock request) {
        return service.lockUser(actor(jwt),id,request);
    }

    @PutMapping("/api/qt/lop/{id}/khoa") public Result lockClass(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Lock request) {
        return service.lockClass(actor(jwt),id,request);
    }

    @GetMapping("/api/qt/lop") public List<Item> allClasses(@AuthenticationPrincipal Jwt jwt) {
        return service.allClasses(actor(jwt));
    }

    @PostMapping("/api/qt/lop") public Result newClass(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody NewClass request) {
        return service.createClass(actor(jwt),request);
    }

    @PostMapping("/api/qt/lop/phan-cong") public Result enroll(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Enrollment request) {
        return service.enroll(actor(jwt),request);
    }

    @PostMapping("/api/gv/giao/luu-tru") public Result archiveAssignments(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ArchiveSet request) {
        return service.archiveAssignments(actor(jwt),request);
    }
    @DeleteMapping("/api/gv/tai-lieu/{id}") public Result archiveDocument(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam UUID lopId) {
        return service.archiveDocument(actor(jwt),lopId,id);
    }
    @DeleteMapping("/api/qt/nguoi-dung/{id}") public Result archiveUser(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        return service.archiveUser(actor(jwt),id);
    }
    @PutMapping("/api/qt/nguoi-dung/{id}/mat-khau") public Result password(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ResetPassword request) {
        return service.resetPassword(actor(jwt),id,request);
    }

    @GetMapping("/api/qt/nhat-ky") public List<Item> audit(@AuthenticationPrincipal Jwt jwt) {
        return service.audit(actor(jwt));
    }
}
