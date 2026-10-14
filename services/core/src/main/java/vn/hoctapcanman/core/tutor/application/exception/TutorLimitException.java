package vn.hoctapcanman.core.tutor.application.exception;

import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

public final class TutorLimitException extends WorkspaceException {
    private final long retryAfterSeconds;
    public TutorLimitException(long retryAfterSeconds) {
        super(429,"Em đã hỏi nhiều lần trong thời gian ngắn. Em chờ một chút rồi thử lại nhé.");
        this.retryAfterSeconds=retryAfterSeconds;
    }
    public long retryAfterSeconds(){return retryAfterSeconds;}
}
