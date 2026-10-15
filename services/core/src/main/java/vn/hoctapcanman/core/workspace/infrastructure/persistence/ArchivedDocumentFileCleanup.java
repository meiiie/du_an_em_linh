package vn.hoctapcanman.core.workspace.infrastructure.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Thử lại việc xóa tệp tài liệu đã ẩn sau commit; chỉ xóa trong thư mục tải lên được cấu hình. */
@Component
public class ArchivedDocumentFileCleanup {
    private final JdbcClient db;
    private final Path root;
    public ArchivedDocumentFileCleanup(JdbcClient db,@Value("${app.content.upload-dir:data/uploads}") String directory) {
        this.db=db;this.root=Path.of(directory).toAbsolutePath().normalize();
    }
    @Scheduled(fixedDelay=60000,initialDelay=1000)
    public void cleanup() {
        var rows=db.sql("SELECT id,file_ref FROM documents WHERE archived_at IS NOT NULL AND file_ref IS NOT NULL ORDER BY archived_at LIMIT 20")
            .query((r,n)->new FileRow(r.getObject(1,UUID.class),r.getString(2))).list();
        for(var row:rows) {
            try {
                deleteStoredFile(root,row.reference());
                db.sql("UPDATE documents SET file_ref=NULL WHERE id=:id AND archived_at IS NOT NULL AND file_ref=:ref")
                    .param("id",row.id()).param("ref",row.reference()).update();
            } catch(IOException | IllegalArgumentException e) {
                // Giữ tham chiếu để lần sau thử lại; không ghi đường dẫn hoặc nội dung tài liệu vào log.
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Chưa xóa được một tệp tài liệu đã ẩn.");
            }
        }
    }
    static void deleteStoredFile(Path root,String reference)throws IOException {
        Path directory=root.toAbsolutePath().normalize();
        Path supplied=Path.of(reference);
        Path target=(supplied.isAbsolute()?supplied:directory.resolve(supplied)).normalize();
        if(target.equals(directory)||!target.startsWith(directory))throw new IllegalArgumentException("Tệp ngoài kho lưu trữ");
        if(!Files.exists(target,LinkOption.NOFOLLOW_LINKS))return;
        Path realRoot=directory.toRealPath();
        if(!target.toRealPath().startsWith(realRoot)||Files.isSymbolicLink(target)||!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Tệp ngoài kho lưu trữ");
        Files.deleteIfExists(target);
    }
    private record FileRow(UUID id,String reference) {}
}
