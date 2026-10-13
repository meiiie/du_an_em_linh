package vn.hoctapcanman.core.workspace.application.dto;

import java.time.Instant;
import java.util.*;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.Nullable;

public final class WorkspaceDto {
    private WorkspaceDto() {
    }

    public record Item(String id,String title,String subtitle,String status,Map<String,Object> details) {
    }

    public record Result(String message,int count) {
    }

    public record Dashboard(int students,int stuck,int pending,int assignments,Map<String,Long> distribution,List<Item> deadlines) {
    }

    public record Library(List<Item> formulas,List<Item> documents) {
    }

    public record Student(Item student,List<Item> history) {
    }

    public record Export(String filename,String content) {
    }

    public record Assign(@NotNull UUID lopId,@NotNull @Size(max=200) List<UUID> studentIds,@NotEmpty @Size(max=100) List<UUID> problemIds,@Pattern(regexp="ON_TAP|KIEM_TRA") @NotNull String mode,@Nullable Instant dueAt,@Size(max=120) String setName) {
    }

    public record MasteryOverride(@NotNull UUID lopId,@NotNull UUID studentId,@NotBlank @Size(max=32) String skillCode,@Pattern(regexp="NHAN_BIET|THONG_HIEU|VAN_DUNG|VAN_DUNG_CAO") @NotNull String level4,@NotBlank @Size(max=500) String reason) {
    }

    public record Review(@NotNull UUID lopId,boolean approve,@NotBlank @Size(max=500) String reason) {
    }

    public record NewProblem(@NotNull UUID lopId,@NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{1,64}") String code,@NotBlank @Size(max=32) String skillCode,@NotNull @Pattern(regexp="NHAN_BIET|THONG_HIEU|VAN_DUNG|VAN_DUNG_CAO") String level4,@NotBlank @Size(max=4000) String statement,@NotBlank @Size(max=4000) String latex,@NotBlank @Size(max=500) String function) {
    }

    public record NextProblem(@NotNull UUID lopId,@NotNull UUID studentId,@NotNull UUID problemId,@NotBlank @Size(max=500) String reason) {
    }

    public record Formula(@NotNull UUID lopId,@NotBlank @Size(max=200) String title,@NotBlank @Size(max=4000) String latex,@NotBlank @Size(max=4000) String statement) {
    }

    public record Settings(@NotNull UUID lopId,boolean revealSolutionAfterSubmit,@NotNull @Pattern(regexp="offline") String aiProvider) {
    }

    public record Schedule(@NotBlank @Size(max=200) String title,@NotNull Instant startsAt,@NotNull Instant endsAt) {
    }

    public record NewUser(@Email @NotBlank @Size(max=254) String email,@NotBlank @Size(max=120) String displayName,@Size(min=12,max=128) @NotNull String password,@NotNull @Pattern(regexp="STUDENT|TEACHER|ADMIN|SCHOOL_ADMIN") String role,boolean synthetic) {
        @Override public String toString() {
            return "NewUser[redacted]";
        }
    }

    public record Bulk(@NotEmpty @Size(max=200) List<UUID> ids,boolean enabled) {
    }

    public record NewClass(@NotBlank @Size(max=60) String name,@Min(1) @Max(12) int grade,@Pattern(regexp="[0-9]{4}-[0-9]{4}") @NotNull String schoolYear) {
    }

    public record Enrollment(@NotNull UUID lopId,@NotNull UUID userId,@Pattern(regexp="STUDENT|TEACHER") @NotNull String role) {
    }

    public record TutorRequest(@NotBlank @Size(max=64) String maBai,@NotBlank @Size(max=32) String maBuoc,@Min(1) @Max(3) int cap,@NotNull @Size(max=2000) String cauHoi) {
    }

    public record TutorEvent(String status,String message) {
    }

    public record TutorContext(UUID problemId,String code,String statement,String function,String step,String hint) {
    }
}
