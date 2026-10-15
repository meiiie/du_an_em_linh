import { describe, expect, it } from 'vitest';
import { DinhDangSo, ngayViet } from './dinh-dang';

describe('Dữ liệu hiển thị tiếng Việt', () => {
  it('nhóm hàng nghìn và dùng dấu phẩy cho phần thập phân', () => {
    expect(new DinhDangSo().transform(12345.5)).toBe('12.345,5');
  });
  it('định dạng tỷ lệ bằng giá trị từ 0 đến 1', () => {
    expect(new DinhDangSo().transform(0.725, 'percent')).toBe('73%');
    expect(new DinhDangSo().transform(0, 'percent')).toBe('0%');
  });
  it('không hiển thị NaN hoặc ngày không hợp lệ', () => {
    expect(new DinhDangSo().transform(undefined)).toBe('—');
    expect(new DinhDangSo().transform(NaN)).toBe('—');
    expect(ngayViet('')).toBe('—');
    expect(ngayViet('không phải ngày')).toBe('—');
  });
  it('giữ ngày 16/10 và hạn 21 giờ theo múi giờ trường', () => {
    expect(ngayViet('2026-10-16T14:00:00Z', true)).toBe('Thứ Sáu, 16/10 · 21:00');
  });
  it('hiển thị thứ đầy đủ, ngày/tháng và giờ Việt Nam', () => {
    expect(ngayViet('2026-10-12T17:30:00Z')).toBe('Thứ Ba, 13/10');
    expect(ngayViet('2026-10-12T17:30:00Z', true)).toBe('Thứ Ba, 13/10 · 00:30');
  });
});
