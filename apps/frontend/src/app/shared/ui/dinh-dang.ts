import { Pipe, PipeTransform } from '@angular/core';

const SO = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });
const TY_LE = new Intl.NumberFormat('vi-VN', { style: 'percent', maximumFractionDigits: 0 });
const NGAY = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' });
const GIO = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' });

@Pipe({ name: 'soViet' })
export class DinhDangSo implements PipeTransform {
  transform(value: number | null | undefined, kieu: 'number' | 'percent' = 'number'): string {
    return value == null || !Number.isFinite(value) ? '—' : (kieu === 'percent' ? TY_LE : SO).format(value);
  }
}

export function ngayViet(value: string | Date | null | undefined, coGio = false): string {
  if (!value) return '—';
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return '—';
  const parts = NGAY.formatToParts(date);
  const part = (name: Intl.DateTimeFormatPartTypes) => parts.find(p => p.type === name)?.value ?? '';
  const weekday = part('weekday').replace(/(^|\s)\S/g, c => c.toLocaleUpperCase('vi-VN'));
  return `${weekday}, ${part('day')}/${part('month')}${coGio ? ' · ' + GIO.format(date) : ''}`;
}
