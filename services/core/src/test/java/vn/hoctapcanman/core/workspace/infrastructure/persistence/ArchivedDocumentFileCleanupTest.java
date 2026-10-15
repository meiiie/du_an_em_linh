package vn.hoctapcanman.core.workspace.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchivedDocumentFileCleanupTest {
    @TempDir Path temp;
    @Test void deletesStoredFileAndRetryIsIdempotent()throws Exception {
        Path root=Files.createDirectory(temp.resolve("uploads"));
        Path file=Files.writeString(root.resolve("bai.txt"),"Tài liệu lớp");
        ArchivedDocumentFileCleanup.deleteStoredFile(root,"bai.txt");
        assertThat(file).doesNotExist();
        ArchivedDocumentFileCleanup.deleteStoredFile(root,"bai.txt");
    }
    @Test void rejectsTraversalDirectoryAndSymlinks()throws Exception {
        Path root=Files.createDirectory(temp.resolve("uploads"));
        Path other=Files.writeString(temp.resolve("other.txt"),"Giữ lại");
        assertThatThrownBy(()->ArchivedDocumentFileCleanup.deleteStoredFile(root,"../other.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->ArchivedDocumentFileCleanup.deleteStoredFile(root,root.toString())).isInstanceOf(IllegalArgumentException.class);
        Files.createSymbolicLink(root.resolve("link.txt"),other);
        assertThatThrownBy(()->ArchivedDocumentFileCleanup.deleteStoredFile(root,"link.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThat(other).hasContent("Giữ lại");
    }
}
