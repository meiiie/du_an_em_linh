package vn.hoctapcanman.core.reminder.application.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class PushDto {
    private PushDto() {}
    public record Keys(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{87,88}={0,2}") String p256dh,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{22,24}={0,2}") String auth) {
        @Override public String toString(){return "Keys[redacted]";}
    }
    public record Subscription(@NotBlank @Size(max=2000) String endpoint,@NotNull @Valid Keys keys) {
        @Override public String toString(){return "Subscription[redacted]";}
    }
    public record Config(boolean available,String publicKey) {}
    public record Result(String message) {}
    public record Target(UUID id,UUID student,Subscription subscription) {
        @Override public String toString(){return "Target[redacted]";}
    }
}
