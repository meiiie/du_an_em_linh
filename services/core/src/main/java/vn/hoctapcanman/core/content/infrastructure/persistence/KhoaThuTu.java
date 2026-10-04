package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Thứ tự khóa chung của mọi đường ghi lượt kiểm, duyệt và phát hành (V6): dòng lớp {@code FOR SHARE}, rồi dòng bài
 * {@code FOR NO KEY UPDATE}, rồi mới tới dòng lượt kiểm hay dòng phát hành. Lần sửa nội dung bài khóa dòng bài rồi mới đánh
 * dấu cũ các lượt và rút phát hành ({@code vo_hieu_ket_qua_bai}); đường ghi nào khóa dòng lượt hay phát hành trước dòng bài
 * sẽ khóa chéo với nó (deadlock). Trigger của V6 cũng khóa lớp rồi bài, nhưng chỉ sau khi câu lệnh đã khóa dòng của chính nó,
 * nên nơi gọi phải khóa trước.
 */
final class KhoaThuTu {

    private KhoaThuTu() {}

    static void lopRoiBai(JdbcClient jdbc, UUID lop, UUID bai) {
        jdbc.sql("select 1 from classes where id = :lop for share").param("lop", lop).query(Integer.class).optional();
        jdbc.sql("select 1 from problems where id = :bai for no key update").param("bai", bai).query(Integer.class).optional();
    }
}
