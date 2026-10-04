package vn.hoctapcanman.core.content.domain.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Một lượt kiểm 3 tầng, gắn lớp và đúng bảng công thức đã dùng (tầng 2, 3 dùng tài liệu và bảng của lớp, nên một lượt
 * của lớp A không phát hành bài cho lớp B). Đổi bảng công thức thì lượt cũ thành {@code stale}.
 *
 * <p>Với lượt kiểm bài, mọi trạng thái suy từ các tầng: trạng thái tổng bằng {@link #overallOf} của các tầng (hoặc
 * {@code GV_DUYET} khi các tầng đủ 1–3 và tổng là {@code KHONG_KIEM_DUOC}), trạng thái phát hành bằng
 * {@link #publishOf} của trạng thái tổng. Constructor từ chối mọi tổ hợp lệch, kể cả lượt nạp lại từ CSDL, nên một tầng
 * {@code SAI} không bao giờ được duyệt hay phát hành.
 *
 * <p>{@code contentVersion} là phiên bản nội dung của bài lúc kiểm ({@code problems.content_version}, V5): bắt buộc với
 * lượt kiểm bài, trống với lượt công thức gia sư. CSDL chỉ nhận lượt kiểm bài đúng phiên bản hiện tại, nên lượt kiểm trên
 * nội dung cũ không ghi được. {@code citationPassageIds} là các đoạn tài liệu lượt này trích dẫn ở tầng 2; adapter ghi
 * vào {@code verification_run_citations} để đoạn và tài liệu căn cứ không đổi dưới chân kết quả kiểm.
 */
public record VerificationRun(
        UUID id,
        UUID classId,
        SubjectKind subjectKind,
        UUID subjectId,
        String contentHash,
        @Nullable Integer contentVersion,
        @Nullable UUID formulaSheetId,
        CheckStatus overallStatus,
        @Nullable ReleaseStatus publishStatus,
        boolean stale,
        Instant createdAt,
        List<TierResult> tiers,
        List<UUID> citationPassageIds) {

    private static final Set<Integer> BA_TANG = Set.of(1, 2, 3);

    public VerificationRun {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(subjectKind, "subjectKind");
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(contentHash, "contentHash");
        Objects.requireNonNull(overallStatus, "overallStatus");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(tiers, "tiers");
        Objects.requireNonNull(citationPassageIds, "citationPassageIds");
        Kiem.sha256(contentHash, "Dấu vân tay nội dung");
        citationPassageIds = List.copyOf(citationPassageIds);
        if (new HashSet<>(citationPassageIds).size() != citationPassageIds.size()) {
            throw new IllegalArgumentException("Đoạn trích dẫn ghi hai lần");
        }
        if (contentVersion != null && contentVersion < 1) {
            throw new IllegalArgumentException("Phiên bản nội dung phải từ 1");
        }
        if (subjectKind == SubjectKind.PROBLEM && contentVersion == null) {
            throw new IllegalArgumentException("Lượt kiểm bài phải ghi phiên bản nội dung đã kiểm");
        }
        tiers = tiers.stream().sorted(Comparator.comparingInt(TierResult::tier)).toList();
        Set<Integer> daCo = new HashSet<>();
        for (TierResult t : tiers) {
            if (!daCo.add(t.tier())) {
                throw new IllegalArgumentException("Tầng " + t.tier() + " ghi hai lần");
            }
        }
        if (subjectKind == SubjectKind.PROBLEM) {
            CheckStatus tuTang = overallOf(tiers);
            boolean khop = overallStatus == tuTang
                || (overallStatus == CheckStatus.GV_DUYET && tuTang == CheckStatus.KHONG_KIEM_DUOC && duBaTang(tiers));
            if (!khop) {
                throw new IllegalArgumentException("Trạng thái tổng " + overallStatus + " không khớp các tầng (" + tuTang + ")");
            }
            if (publishStatus != publishOf(overallStatus)) {
                throw new IllegalArgumentException("Trạng thái phát hành không khớp trạng thái tổng của lượt kiểm bài");
            }
        } else {
            if (publishStatus != null || overallStatus == CheckStatus.GV_DUYET) {
                throw new IllegalArgumentException("Công thức trong lời gia sư không phát hành và không duyệt riêng (ADR 013)");
            }
            if (overallStatus != overallOf(tiers)) {
                throw new IllegalArgumentException("Trạng thái tổng " + overallStatus + " không khớp các tầng (" + overallOf(tiers) + ")");
            }
        }
    }

    /**
     * Lượt kiểm bài mới cho một lớp, trên phiên bản nội dung {@code contentVersion}; trạng thái tổng và phát hành tính từ
     * các tầng. Chưa có đoạn trích dẫn: gắn bằng {@link #withCitations}.
     */
    public static VerificationRun forProblem(UUID classId, UUID problemId, String contentHash, int contentVersion,
            @Nullable UUID formulaSheetId, List<TierResult> tiers, Instant now) {
        CheckStatus tong = overallOf(tiers);
        return new VerificationRun(UUID.randomUUID(), classId, SubjectKind.PROBLEM, problemId, contentHash, contentVersion,
            formulaSheetId, tong, publishOf(tong), false, now, tiers, List.of());
    }

    /** Lượt này với các đoạn tài liệu tầng 2 đã trích dẫn. */
    public VerificationRun withCitations(List<UUID> passageIds) {
        return new VerificationRun(id, classId, subjectKind, subjectId, contentHash, contentVersion, formulaSheetId,
            overallStatus, publishStatus, stale, createdAt, tiers, passageIds);
    }

    /**
     * Trạng thái tổng từ các tầng, như {@code cong_phat_hanh} của {@code services/math/app/verify.py}: có tầng {@code SAI}
     * thì {@code SAI}; còn tầng {@code KHONG_KIEM_DUOC} thì {@code KHONG_KIEM_DUOC}; mọi tầng {@code DAT} thì {@code DAT}.
     * Chặt hơn v0 một chỗ: thiếu tầng nào trong 1–3 thì coi tầng đó {@code KHONG_KIEM_DUOC}, nên phản hồi thiếu tầng không
     * bao giờ tự phát hành. Lượt thiếu tầng cũng không duyệt được ({@link ApprovalRefusal#INCOMPLETE}), như v0.
     */
    public static CheckStatus overallOf(List<TierResult> tiers) {
        if (tiers.stream().anyMatch(t -> t.status() == CheckStatus.SAI)) {
            return CheckStatus.SAI;
        }
        if (!duBaTang(tiers) || tiers.stream().anyMatch(t -> t.status() != CheckStatus.DAT)) {
            return CheckStatus.KHONG_KIEM_DUOC;
        }
        return CheckStatus.DAT;
    }

    /** Trạng thái phát hành của bài theo trạng thái tổng của lượt kiểm. */
    public static ReleaseStatus publishOf(CheckStatus overall) {
        return switch (overall) {
            case DAT, GV_DUYET -> ReleaseStatus.DA_PHAT_HANH;
            case SAI -> ReleaseStatus.BI_CHAN;
            case KHONG_KIEM_DUOC -> ReleaseStatus.CHO_GIAO_VIEN_DUYET;
        };
    }

    /** Bảng công thức của lớp vừa đổi: lượt này không còn là căn cứ phát hành. */
    public VerificationRun markStale() {
        return stale ? this : new VerificationRun(id, classId, subjectKind, subjectId, contentHash, contentVersion,
            formulaSheetId, overallStatus, publishStatus, true, createdAt, tiers, citationPassageIds);
    }

    /**
     * Lượt này còn là căn cứ cho trạng thái hiện tại của (lớp, bài) không: không {@code stale}, là lượt mới nhất, đúng nội
     * dung hiện tại của bài, đúng bảng công thức hiện tại của lớp (cả hai cùng trống khi lớp chưa có bảng khóa). Dùng cả khi
     * duyệt lẫn khi áp lượt vào trạng thái phát hành ({@link ProblemRelease#apply}).
     *
     * @param latest đây là lượt mới nhất của (lớp, bài)
     */
    public Optional<ApprovalRefusal> freshnessRefusal(boolean latest, String currentContentHash, @Nullable UUID currentSheetId) {
        if (stale) {
            return Optional.of(ApprovalRefusal.STALE);
        } else if (!latest) {
            return Optional.of(ApprovalRefusal.NOT_LATEST);
        } else if (!contentHash.equals(currentContentHash)) {
            return Optional.of(ApprovalRefusal.CONTENT_CHANGED);
        } else if (!Objects.equals(formulaSheetId, currentSheetId)) {
            return Optional.of(ApprovalRefusal.SHEET_CHANGED);
        }
        return Optional.empty();
    }

    /**
     * Vì sao giáo viên không duyệt được lượt này, hoặc rỗng nếu duyệt được (FR-005): chỉ lượt kiểm bài; tầng {@code SAI}
     * (tính lại từ các tầng) không bao giờ duyệt được; phải đủ ba tầng; rồi lượt phải còn mới ({@link #freshnessRefusal}).
     */
    public Optional<ApprovalRefusal> approvalRefusal(boolean latest, String currentContentHash, @Nullable UUID currentSheetId) {
        if (subjectKind != SubjectKind.PROBLEM) {
            return Optional.of(ApprovalRefusal.NOT_A_PROBLEM);
        }
        if (overallOf(tiers) == CheckStatus.SAI) {
            return Optional.of(ApprovalRefusal.BLOCKED);
        }
        if (overallStatus == CheckStatus.GV_DUYET) {
            return Optional.of(ApprovalRefusal.ALREADY_APPROVED);
        }
        if (overallStatus == CheckStatus.DAT) {
            return Optional.of(ApprovalRefusal.NOTHING_TO_APPROVE);
        }
        if (!duBaTang(tiers)) {
            return Optional.of(ApprovalRefusal.INCOMPLETE);
        }
        return freshnessRefusal(latest, currentContentHash, currentSheetId);
    }

    /**
     * Giáo viên duyệt: lượt thành {@code GV_DUYET}, bài {@code DA_PHAT_HANH} cho lớp này. Không duyệt được thì
     * {@link IllegalStateException}; nơi gọi hỏi {@link #approvalRefusal} trước để trả lý do.
     */
    public Approval approve(
            UUID reviewerId, String note, boolean latest, String currentContentHash, @Nullable UUID currentSheetId, Instant now) {
        approvalRefusal(latest, currentContentHash, currentSheetId).ifPresent(lyDo -> {
            throw new IllegalStateException("Không duyệt được lượt kiểm: " + lyDo);
        });
        ContentReview duyet = new ContentReview(UUID.randomUUID(), id, contentHash, reviewerId, note, now);
        VerificationRun daDuyet = new VerificationRun(id, classId, subjectKind, subjectId, contentHash, contentVersion,
            formulaSheetId, CheckStatus.GV_DUYET, ReleaseStatus.DA_PHAT_HANH, false, createdAt, tiers, citationPassageIds);
        return new Approval(daDuyet, duyet);
    }

    /** Không in căn cứ của từng tầng vào log: tầng 1 là kết quả chấm lời giải chuẩn (FR-006). */
    @Override
    public String toString() {
        return "VerificationRun[id=" + id + ", subjectKind=" + subjectKind + ", overall=" + overallStatus
            + ", publish=" + publishStatus + ", stale=" + stale + "]";
    }

    private static boolean duBaTang(List<TierResult> tiers) {
        return tiers.stream().map(TierResult::tier).toList().containsAll(BA_TANG);
    }

    /** Kết quả duyệt: lượt kiểm sau duyệt và bản ghi duyệt, ghi cùng một giao dịch. */
    public record Approval(VerificationRun run, ContentReview review) {}
}
