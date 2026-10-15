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

/** Ngày giờ nhập theo Việt Nam, đóng về UTC; không để Date tự cuộn ngày sai sang tháng sau. */
export function docHanViet(value:string):string|null {
  const m=/^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/.exec(value.trim());
  if(!m)return null;
  const [,d,mo,y,h,mi]=m.map(Number);
  if(y<2000||y>2100||mo<1||mo>12||d<1||h>23||mi>59)return null;
  const civil=new Date(Date.UTC(y,mo-1,d,h,mi));
  if(civil.getUTCDate()!==d||civil.getUTCMonth()!==mo-1)return null;
  return new Date(civil.getTime()-7*3600_000).toISOString();
}
