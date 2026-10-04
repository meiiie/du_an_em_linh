/**
 * Lưu trữ của module practice bằng {@code JdbcClient} như module content (không entity JPA): chỉ mục duy nhất từng phần
 * ({@code ON CONFLICT … WHERE}), {@code jsonb}, khóa dòng bài làm khi thay nội dung một bước.
 */
@NullMarked
package vn.hoctapcanman.core.practice.infrastructure.persistence;

import org.jspecify.annotations.NullMarked;
