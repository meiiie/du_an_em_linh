package vn.hoctapcanman.core.workspace.application.exception;

public class WorkspaceException extends RuntimeException {
    private final int status;
    public WorkspaceException(int status,String message) {
        super(message);
        this.status=status;
    }

    public int status() {
        return status;
    }
}
