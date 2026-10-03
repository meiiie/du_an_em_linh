package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.domain.model.Document;
import vn.hoctapcanman.core.content.domain.model.DocumentKind;
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;
import vn.hoctapcanman.core.content.domain.repository.DocumentRepository;

/** Tài liệu và đoạn trên bảng {@code documents}, {@code document_passages}. */
@Repository
public class DocumentRepositoryAdapter implements DocumentRepository {

    private static final String COT = """
            id, class_id, code, title, kind, source, license_status, file_ref, text_content, version, uploaded_by, created_at""";

    private final JdbcClient jdbc;

    public DocumentRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public List<DocumentPassage> save(Document d, List<DocumentPassage> passages) {
        if (passages.stream().anyMatch(p -> !p.documentId().equals(d.id()))) {
            throw new IllegalArgumentException("Đoạn phải thuộc tài liệu " + d.id());
        }
        jdbc.sql("insert into documents (" + COT + """
                ) values (:id, :lop, :code, :title, :kind, :source, :license, :file, :text, :version, :by, :created)
                on conflict (id) do update set class_id = excluded.class_id, code = excluded.code, title = excluded.title,
                    kind = excluded.kind, source = excluded.source, license_status = excluded.license_status,
                    file_ref = excluded.file_ref, text_content = excluded.text_content, version = excluded.version,
                    uploaded_by = excluded.uploaded_by, created_at = excluded.created_at""")
            .param("id", d.id()).param("lop", d.classId()).param("code", d.code()).param("title", d.title())
            .param("kind", d.kind().code()).param("source", d.source()).param("license", d.licenseStatus())
            .param("file", d.fileRef()).param("text", d.textContent()).param("version", d.version())
            .param("by", d.uploadedBy()).param("created", Cot.luc(d.createdAt()))
            .update();
        for (DocumentPassage p : passages) {
            jdbc.sql("""
                    insert into document_passages (id, document_id, page, char_start, char_end, text, text_folded)
                    values (:id, :doc, :page, :start, :end, :text, :folded)
                    on conflict (document_id, char_start) do update set page = excluded.page, char_end = excluded.char_end,
                        text = excluded.text, text_folded = excluded.text_folded""")
                .param("id", p.id()).param("doc", p.documentId()).param("page", p.page()).param("start", p.charStart())
                .param("end", p.charEnd()).param("text", p.text()).param("folded", p.textFolded())
                .update();
        }
        List<Integer> viTri = passages.stream().map(DocumentPassage::charStart).toList();
        if (viTri.isEmpty()) {
            jdbc.sql("delete from document_passages where document_id = :doc").param("doc", d.id()).update();
        } else {
            jdbc.sql("delete from document_passages where document_id = :doc and char_start not in (:vt)")
                .param("doc", d.id()).param("vt", viTri).update();
        }
        return findPassages(d.id());
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return jdbc.sql("select " + COT + " from documents where id = :id").param("id", id)
            .query(DocumentRepositoryAdapter::taiLieu).optional();
    }

    @Override
    public Optional<Document> findByClassAndCode(UUID classId, String code) {
        return jdbc.sql("select " + COT + " from documents where class_id = :lop and code = :code")
            .param("lop", classId).param("code", code).query(DocumentRepositoryAdapter::taiLieu).optional();
    }

    @Override
    public List<Document> findByClass(UUID classId) {
        return jdbc.sql("select " + COT + " from documents where class_id = :lop order by created_at, id")
            .param("lop", classId).query(DocumentRepositoryAdapter::taiLieu).list();
    }

    @Override
    public List<DocumentPassage> findPassages(UUID documentId) {
        return jdbc.sql("""
                select id, document_id, page, char_start, char_end, text, text_folded from document_passages
                where document_id = :doc order by char_start""")
            .param("doc", documentId)
            .query((rs, n) -> new DocumentPassage(Cot.uuid(rs, "id"), Cot.uuid(rs, "document_id"), Cot.soNeuCo(rs, "page"),
                rs.getInt("char_start"), rs.getInt("char_end"), Cot.chu(rs, "text"), Cot.chu(rs, "text_folded")))
            .list();
    }

    private static Document taiLieu(ResultSet rs, int n) throws SQLException {
        return new Document(Cot.uuid(rs, "id"), Cot.uuid(rs, "class_id"), rs.getString("code"), Cot.chu(rs, "title"),
            DocumentKind.parse(Cot.chu(rs, "kind")), rs.getString("source"), Cot.chu(rs, "license_status"),
            rs.getString("file_ref"), Cot.chu(rs, "text_content"), rs.getInt("version"), Cot.uuidNeuCo(rs, "uploaded_by"),
            Cot.thoiDiem(rs, "created_at"));
    }
}
