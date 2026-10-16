package vn.hoctapcanman.core.workspace.application.service;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;
import vn.hoctapcanman.core.workspace.application.port.*;

@Service @Transactional public class WorkspaceService {
    private final WorkspaceStore store;
    private final ClassMembership membership;
    private final ContentWorkflow content;
    private final PasswordHasher passwords;
    private final Clock clock;
    public WorkspaceService(WorkspaceStore store,ClassMembership membership,ContentWorkflow content,PasswordHasher passwords,Clock clock) {
        this.store=store;
        this.membership=membership;
        this.content=content;
        this.passwords=passwords;
        this.clock=clock;
    }

    @org.springframework.beans.factory.annotation.Autowired private ContentDraftStore drafts;
    @org.springframework.beans.factory.annotation.Autowired private ContentDraftModel draftModel;
    @org.springframework.beans.factory.annotation.Autowired private jakarta.validation.Validator validator;

    public Result draft(UUID actor,DraftRequest request) {
        teacher(actor,request.lopId());
        String material=drafts.material(request.lopId(),request.documentId()).orElseThrow(()->new WorkspaceException(404,"Chọn tài liệu có quyền sử dụng trong lớp."));
        if(material.length()>20000)throw new WorkspaceException(400,"Chọn tài liệu tối đa 20.000 ký tự để soạn nháp.");
        var skills=drafts.authoringSkills(request.lopId());
        String skill=request.skillCode()==null||request.skillCode().isBlank()?skills.stream().findFirst()
            .orElseThrow(()->new WorkspaceException(400,"Lớp chưa có kỹ năng của chủ đề đang học.")):request.skillCode();
        if(!skills.contains(skill))throw new WorkspaceException(400,"Chọn kỹ năng trong danh mục của lớp.");
        var generated=draftModel.generate(request.lopId(),material,skill).orElseThrow(()->new WorkspaceException(503,"AI soạn nháp chưa sẵn sàng. Thầy cô có thể nhập bài thủ công."));
        var p=generated.problem();
        if(!skills.contains(p.skillCode())||!skill.equals(p.skillCode()))
            throw new WorkspaceException(422,"AI chưa dùng đúng kỹ năng đã chọn. Hãy thử lại.");
        var problem=new NewProblem(request.lopId(),"AI."+UUID.randomUUID().toString().replace("-",""),p.skillCode(),p.level4(),p.statement(),p.latex(),p.function());
        if(!validator.validate(problem).isEmpty()||generated.solution()==null||generated.solution().isBlank()||generated.solution().length()>10000||generated.inputTokens()<0||generated.outputTokens()<0)
            throw new WorkspaceException(422,"Bản nháp chưa đủ nội dung. Hãy thử lại.");
        drafts.save(actor,request,new GeneratedDraft(problem,generated.solution(),generated.provider(),generated.inputTokens(),generated.outputTokens()),clock.instant());
        return ok("Đã đưa đề và lời giải nháp vào hàng chờ duyệt.",1);
    }

    public UUID studentClass(UUID student) {
        return membership.lopHoc(student).orElseThrow(()->new WorkspaceException(404,"Em chưa được ghi danh vào lớp."));
    }

    private void teacher(UUID actor,UUID cl) {
        membership.kiemGiaoVien(actor,cl);
    }

    public List<Item> classes(UUID actor) {
        return store.classes(actor,false);
    }

    public Dashboard dashboard(UUID actor,UUID cl) {
        teacher(actor,cl);
        return store.dashboard(cl);
    }

    public List<Item> students(UUID actor,UUID cl) {
        teacher(actor,cl);
        return store.students(cl);
    }

    public Student student(UUID actor,UUID cl,UUID id) {
        teacher(actor,cl);
        Item st=store.students(cl).stream().filter(x->x.id().equals(id.toString())).findFirst().orElseThrow(()->new WorkspaceException(404,"Không tìm thấy học sinh trong lớp."));
        return new Student(st,store.history(id,cl));
    }

    public Result createProblem(UUID actor,NewProblem r) {
        teacher(actor,r.lopId());
        content.createProblem(actor,r);
        store.log(actor,"NHAP_BAI",r.lopId().toString(),r.code(),clock.instant());
        return ok("Đã nhập và kiểm bài. Xem trạng thái trong ngân hàng.",1);
    }

    public Result recommend(UUID actor,NextProblem r) {
        teacher(actor,r.lopId());
        if(!membership.laHocSinh(r.studentId(),r.lopId()))throw new WorkspaceException(403,"Học sinh không thuộc lớp.");
        if(store.bank(r.lopId()).stream().noneMatch(i->i.id().equals(r.problemId().toString())&&i.status().equals("DA_PHAT_HANH")))throw new WorkspaceException(400,"Chọn bài đã phát hành cho lớp.");
        store.recommend(actor,r,clock.instant());
        return ok("Đã chọn bài tiếp theo cho học sinh.",1);
    }

    public List<Item> skillCatalog(UUID actor,UUID cl) {
        teacher(actor,cl);
        return drafts.authoringSkillCatalog(cl);
    }

    public List<Item> bank(UUID actor,UUID cl) {
        teacher(actor,cl);
        return store.bank(cl);
    }

    public List<Item> bank(UUID actor,UUID cl,String q,String level4,String skillCode,String gate) {
        teacher(actor,cl);
        String query=searchQuery(q);
        if(!level4.isEmpty()&&!Set.of("NHAN_BIET","THONG_HIEU","VAN_DUNG","VAN_DUNG_CAO").contains(level4)
            ||!gate.isEmpty()&&!Set.of("DAT","SAI","KHONG_KIEM_DUOC","GV_DUYET").contains(gate))
            throw new WorkspaceException(400,"Chọn mức và trạng thái kiểm hợp lệ.");
        return store.bank(cl).stream().filter(i->matches(i,query))
            .filter(i->level4.isEmpty()||level4.equals(i.details().get("level4")))
            .filter(i->skillCode.isEmpty()||skillCode.equals(i.details().get("skillCode")))
            .filter(i->gate.isEmpty()||gate.equals(i.details().get("gate"))).toList();
    }

    public BankPage bankPage(UUID actor,UUID cl,String query,String level,String skillCode,String gate,int page,int size) {
        teacher(actor,cl);
        if(page<1||page>100000||size<1||size>100)throw new WorkspaceException(400,"Chọn trang và số bài từ 1 đến 100.");
        if(!level.isEmpty()&&!Set.of("NHAN_BIET","THONG_HIEU","VAN_DUNG","VAN_DUNG_CAO").contains(level)
            ||!gate.isEmpty()&&!Set.of("DAT","SAI","KHONG_KIEM_DUOC","GV_DUYET").contains(gate))
            throw new WorkspaceException(400,"Chọn mức và kết quả kiểm hợp lệ.");
        return store.bankPage(cl,searchQuery(query),level,skillCode,gate,page,size);
    }

    public Library library(UUID actor,UUID cl,String q) {
        teacher(actor,cl);
        return searchLibrary(store.library(cl,true),q);
    }

    public Library library(UUID student,String q) {
        return searchLibrary(library(student),q);
    }

    private static Library searchLibrary(Library library,String q) {
        String query=searchQuery(q);
        return new Library(library.formulas().stream().filter(i->matches(i,query)).toList(),
            library.documents().stream().filter(i->matches(i,query)).toList());
    }

    private static String searchQuery(String q) {
        if(q.length()>200)throw new WorkspaceException(400,"Từ khóa tìm kiếm tối đa 200 ký tự.");
        return fold(q.strip());
    }

    private static String fold(String value) {
        return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace('đ','d');
    }

    private static boolean matches(Item i,String query) {
        return query.isEmpty()||fold(i.title()+" "+i.subtitle()+" "+i.details()).contains(query);
    }

    public List<Item> assignments(UUID actor,UUID cl) {
        teacher(actor,cl);
        return store.assignments(cl);
    }

    public Result assign(UUID actor,Assign r) {
        teacher(actor,r.lopId());
        if(r.dueAt()!=null&&!r.dueAt().isAfter(clock.instant()))throw new WorkspaceException(400,"Hạn nộp phải ở tương lai.");
        List<UUID> all=membership.hocSinhCuaLop(actor,r.lopId());
        List<UUID> selected=r.studentIds().isEmpty()?all:r.studentIds().stream().distinct().toList();
        if(!all.containsAll(selected))throw new WorkspaceException(403,"Chỉ giao bài cho học sinh trong lớp.");
        if(selected.isEmpty())throw new WorkspaceException(400,"Lớp chưa có học sinh.");
        Set<String> published=new HashSet<>();
        store.bank(r.lopId()).stream().filter(i->i.status().equals("DA_PHAT_HANH")).forEach(i->published.add(i.id()));
        if(!r.problemIds().stream().allMatch(id->published.contains(id.toString())))throw new WorkspaceException(400,"Chỉ giao bài đã phát hành cho lớp.");
        store.assign(actor,r,selected,clock.instant());
        return ok("Đã giao bài.",selected.size()*r.problemIds().stream().distinct().toList().size());
    }

    public Result override(UUID actor,MasteryOverride r) {
        teacher(actor,r.lopId());
        if(!membership.laHocSinh(r.studentId(),r.lopId()))throw new WorkspaceException(403,"Học sinh không thuộc lớp.");
        store.override(actor,r,clock.instant());
        return ok("Đã ghi nhận mức và lý do.",1);
    }

    public List<Item> topics(UUID student) { return store.topics(student,studentClass(student)); }
    public List<Item> selfStudy(UUID student,String topic) { return store.selfStudy(student,studentClass(student),topic); }
    public Result chooseStudy(UUID student,String code) {
        store.chooseStudy(student,studentClass(student),code,clock.instant());
        return ok("Đã chọn bài tự học.",1);
    }

    public List<Item> modes(UUID student) {
        return store.modes(student,studentClass(student));
    }

    public List<Item> recommendations(UUID student) {
        return store.recommendations(student,studentClass(student));
    }

    public Library library(UUID actor,UUID cl) {
        teacher(actor,cl);
        return store.library(cl,true);
    }

    public Library library(UUID student) {
        return store.library(studentClass(student),false);
    }

    public Export export(UUID student) {
        StringBuilder csv=new StringBuilder("\uFEFFthuật ngữ,định nghĩa\r\n");
        for(Item f:library(student).formulas())csv.append(csvCell(f.title())).append(',').append(csvCell(f.subtitle())).append("\r\n");
        return new Export("cong-thuc.csv",csv.toString());
    }

    public static String csvCell(String value) {
        String text=value;
        String leading=text.stripLeading();
        if((!leading.isEmpty()&&"=+-@".indexOf(leading.charAt(0))>=0)||(!text.isEmpty()&&Character.isISOControl(text.charAt(0))))text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }

    public Result upload(UUID actor,UUID cl,String title,String license,String filename,byte[] bytes) {
        teacher(actor,cl);
        if(bytes.length==0||bytes.length>5*1024*1024)throw new WorkspaceException(400,"Chọn tệp không rỗng, tối đa 5 MB.");
        if(title.isBlank()||title.length()>300||!Set.of("tu_soan","duoc_phep","cong_khai","chua_ro").contains(license))throw new WorkspaceException(400,"Kiểm tra tên và quyền dùng tài liệu.");
        String lower=filename.toLowerCase(Locale.ROOT);
        String text;
        if(lower.endsWith(".pdf"))text=content.extractPdf(bytes);
        else if(lower.endsWith(".txt")||lower.endsWith(".md"))text=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
        else throw new WorkspaceException(400,"Chỉ nhận PDF, TXT hoặc MD.");
        if(text.isBlank()||text.indexOf('\0')>=0)throw new WorkspaceException(400,"Tệp chưa có văn bản đọc được.");
        store.document(actor,cl,title,license,text,clock.instant());
        return ok("Đã thêm tài liệu vào kho lớp.",1);
    }

    public Result formula(UUID actor,Formula r) {
        teacher(actor,r.lopId());
        store.formula(actor,r,clock.instant());
        return ok("Đã thêm công thức nháp; cần kiểm trước khi khóa.",1);
    }

    public Result checkFormulas(UUID actor,UUID cl) {
        teacher(actor,cl);
        return ok("Đã kiểm bảng công thức.",content.checkFormulas(actor,cl));
    }

    public List<Item> reviews(UUID actor,UUID cl) {
        teacher(actor,cl);
        var result=new ArrayList<>(store.reviews(cl));
        result.addAll(drafts.pending(cl));
        return result;
    }

    public Result review(UUID actor,UUID id,Review r) {
        teacher(actor,r.lopId());
        var draft=drafts.lock(r.lopId(),id);
        if(draft.isPresent()) {
            if(r.approve())content.createApprovedDraft(actor,draft.get().problem(),draft.get().solution());
            drafts.decide(actor,id,r,clock.instant());
        } else content.review(actor,id,r);
        store.log(actor,r.approve()?"DUYET":"CHAN",id.toString(),r.reason(),clock.instant());
        return ok("Đã lưu quyết định.",1);
    }

    public Settings settings(UUID actor,UUID cl) {
        teacher(actor,cl);
        return store.settings(cl);
    }

    public Result settings(UUID actor,Settings r) {
        teacher(actor,r.lopId());
        store.settings(actor,r,clock.instant());
        return ok("Đã lưu cài đặt.",1);
    }

    public List<Item> stuckAlerts(UUID actor, UUID cl) {
        teacher(actor, cl);
        return store.stuckAlerts(cl);
    }

    public WeeklyPlan weeklyPlan(UUID student, int weekOffset) {
        if (weekOffset < -52 || weekOffset > 52) throw new WorkspaceException(400, "Chọn tuần trong phạm vi một năm.");
        return WeeklyStudyRules.build(store.learningSkills(student, studentClass(student)), clock.instant(), weekOffset);
    }

    public List<Item> schedule(UUID student) {
        return store.schedule(student);
    }

    public Result schedule(UUID student,Schedule r) {
        if(Duration.between(r.startsAt(),r.endsAt()).compareTo(Duration.ofMinutes(1))<0||Duration.between(r.startsAt(),r.endsAt()).compareTo(Duration.ofHours(8))>0)throw new WorkspaceException(400,"Chọn khung học từ 1 phút đến 8 giờ.");
        store.schedule(student,r);
        return ok("Đã thêm buổi học.",1);
    }

    public Result complete(UUID student,UUID id) {
        if(!store.complete(student,id))throw new WorkspaceException(404,"Không tìm thấy buổi học.");
        return ok("Đã đánh dấu hoàn thành.",1);
    }

    public List<Item> reminders(UUID student) {
        var result = new ArrayList<>(store.reminders(student,clock.instant()));
        var today = clock.instant().atZone(WeeklyStudyRules.SCHOOL_ZONE).toLocalDate();
        weeklyPlan(student, 0).sessions().stream().filter(b ->
            Instant.parse((String)b.details().get("startsAt")).atZone(WeeklyStudyRules.SCHOOL_ZONE).toLocalDate().equals(today)
            && !Instant.parse((String)b.details().get("endsAt")).isBefore(clock.instant())).forEach(b -> result.addFirst(b));
        return result;
    }

    private boolean globalAdmin(UUID actor) {
        String role=store.accountRole(actor);
        if("ADMIN".equals(role))return true;
        if("SCHOOL_ADMIN".equals(role))return false;
        throw new WorkspaceException(403,"Tài khoản không có quyền quản trị.");
    }

    public List<Item> users(UUID actor) {
        boolean global=globalAdmin(actor);
        return store.users().stream().filter(u->global||store.managesUser(actor,UUID.fromString(u.id()))).toList();
    }

    public Result createUser(UUID actor,NewUser r) {
        if(!r.synthetic())throw new WorkspaceException(400,"Bản thử nghiệm chỉ nhận tài khoản tổng hợp.");
        if(!globalAdmin(actor))throw new WorkspaceException(403,"Quản trị lớp chỉ quản lý tài khoản đã ghi danh; nhờ quản trị hệ thống tạo tài khoản mới.");
        store.createUser(actor,r,passwords.hash(r.password()),clock.instant());
        return ok("Đã tạo tài khoản.",1);
    }

    public Result bulk(UUID actor,Bulk r) {
        if(!r.enabled()&&r.ids().contains(actor))throw new WorkspaceException(400,"Không thể tự khóa tài khoản đang dùng.");
        boolean global=globalAdmin(actor);
        if(!global&&!r.ids().stream().allMatch(id->store.managesUser(actor,id)))throw new WorkspaceException(403,"Chỉ cập nhật tài khoản học sinh và giáo viên trong lớp được phân công.");
        store.bulk(actor,r,clock.instant());
        return ok("Đã cập nhật tài khoản.",r.ids().size());
    }

    public List<Item> allClasses(UUID actor) {
        return store.classes(actor,globalAdmin(actor));
    }

    public Result createClass(UUID actor,NewClass r) {
        if(!globalAdmin(actor))throw new WorkspaceException(403,"Nhờ quản trị hệ thống tạo và phân công lớp mới.");
        store.createClass(actor,r,clock.instant());
        return ok("Đã tạo lớp.",1);
    }

    public Result archiveAssignments(UUID actor,ArchiveSet r) {
        teacher(actor,r.lopId());
        if(!store.archiveAssignments(actor,r.lopId(),r.ids(),clock.instant()))
            throw new WorkspaceException(404,"Không tìm thấy lượt giao trong lớp.");
        return ok("Đã lưu trữ bộ bài được giao.",r.ids().size());
    }

    public Result archiveDocument(UUID actor,UUID cl,UUID id) {
        teacher(actor,cl);
        if(!store.archiveDocument(actor,cl,id,clock.instant())) throw new WorkspaceException(404,"Không tìm thấy tài liệu trong lớp.");
        return ok("Đã ẩn tài liệu khỏi kho lớp.",1);
    }

    private void manageAccount(UUID actor,UUID id) {
        boolean global=globalAdmin(actor);
        if(actor.equals(id))throw new WorkspaceException(400,"Không thể thực hiện với tài khoản đang dùng.");
        if(!global&&!store.managesUser(actor,id))throw new WorkspaceException(403,"Tài khoản ngoài phạm vi quản lý.");
    }

    public Result archiveUser(UUID actor,UUID id) {
        manageAccount(actor,id);
        if(!store.archiveUser(actor,id,clock.instant()))throw new WorkspaceException(404,"Không tìm thấy tài khoản.");
        return ok("Đã vô hiệu hóa và ẩn tài khoản.",1);
    }

    public Result resetPassword(UUID actor,UUID id,ResetPassword r) {
        manageAccount(actor,id);
        if(r.password()==null||r.password().length()<12||r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new WorkspaceException(400,"Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte.");
        if(!store.resetPassword(actor,id,passwords.hash(r.password()),clock.instant()))throw new WorkspaceException(404,"Không tìm thấy tài khoản.");
        return ok("Đã đặt mật khẩu mới và đăng xuất các phiên cũ.",1);
    }

    public Result lockUser(UUID actor,UUID id,Lock r) {
        return bulk(actor,new Bulk(List.of(id),!r.locked()));
    }

    public Result lockClass(UUID actor,UUID id,Lock r) {
        boolean global=globalAdmin(actor);
        if(store.classes(actor,global).stream().noneMatch(i->i.id().equals(id.toString())))
            throw new WorkspaceException(global?404:403,"Không tìm thấy lớp trong phạm vi quản lý.");
        if(!store.lockClass(actor,id,r.locked(),clock.instant()))throw new WorkspaceException(404,"Không tìm thấy lớp.");
        return ok(r.locked()?"Đã khóa lớp.":"Đã mở lớp.",1);
    }

    public Result enroll(UUID actor,Enrollment r) {
        if(!globalAdmin(actor)) {
            teacher(actor,r.lopId());
            if(!store.managesUser(actor,r.userId()))throw new WorkspaceException(403,"Chỉ phân công người dùng thuộc phạm vi lớp quản lý.");
        }
        store.enroll(actor,r,clock.instant());
        return ok("Đã ghi danh vào lớp.",1);
    }

    public List<Item> audit(UUID actor) {
        boolean global=globalAdmin(actor);
        return store.audit().stream().filter(i->global||actor.toString().equals(i.details().get("actorId"))).toList();
    }

    private static Result ok(String text,int n) {
        return new Result(text,n);
    }
}
