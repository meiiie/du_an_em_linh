/**
 * Adapter lưu trữ của module nội dung, bằng {@code JdbcClient} trên schema {@code V4__content.sql}: ghi đè theo khóa
 * (importer chạy lại không nhân bản), cột {@code jsonb} và mảng, thứ tự ghi mà khóa ngoại và trigger đòi.
 */
@NullMarked
package vn.hoctapcanman.core.content.infrastructure.persistence;

import org.jspecify.annotations.NullMarked;
