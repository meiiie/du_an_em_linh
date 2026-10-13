import { ApiLopHoc } from '../../api/lop-hoc';
import { Component, computed, inject, signal } from '@angular/core';
import { form, FormField, required, maxLength } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import { Button } from '../../shared/ui/button';
import { HopThoai } from '../../shared/ui/hop-thoai';
import { ThongBaoNhanh } from '../../shared/ui/thong-bao';
import { KhungCho, LoiTai } from '../../shared/ui/trang-thai';
const MUI_GIO_TRUONG = 'Asia/Ho_Chi_Minh';
const NGAY_TRUONG = new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: MUI_GIO_TRUONG });
const GIO_TRUONG = new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', timeZone: MUI_GIO_TRUONG });

function ngayTruong(value: string | Date | null | undefined): string | null {
 if (!value) return null;
 const date = new Date(value);
 if (!Number.isFinite(date.getTime())) return null;
 const parts = NGAY_TRUONG.formatToParts(date);
 const part = (type: Intl.DateTimeFormatPartTypes) => parts.find(p => p.type === type)!.value;
 return `${part('year')}-${part('month')}-${part('day')}`;
}

@Component({selector:'app-lich',imports:[Button,HopThoai,FormField,KhungCho,LoiTai],template:`<div class="page"><header class="toolbar"><div><h1>Lịch học</h1><p>Chọn một khoảng rảnh để học đều mỗi tuần.</p></div><button appButton type="button" (click)="mo.set(true)">Thêm buổi học</button></header>
@if(ds.isLoading()){<app-khung-cho kieu="lich" nhan="Đang tải lịch học" />}@else if(ds.error()){<app-loi-tai [loi]="ds.error()" (thuLai)="ds.reload()" />}@else{
<div class="toolbar"><button appButton variant="secondary" type="button" aria-label="Tuần trước" (click)="tuan.update(giam)">← Tuần trước</button><strong>{{nhanTuan()}}</strong><button appButton variant="secondary" type="button" aria-label="Tuần sau" (click)="tuan.update(tang)">Tuần sau →</button></div>
@if(nhac.error()){<app-loi-tai [loi]="nhac.error()" moTa="Chưa tải được hạn nộp. Lịch học của em vẫn dùng được." (thuLai)="nhac.reload()" />}<div class="week-grid">@for(ngay of cacNgay();track ngay.iso){<section class="surface"><h2>{{ngay.ten}}</h2><p class="muted">{{ngay.nhan}}</p>@for(b of ngay.buoi;track b.id){<article class="schedule-item"><span class="badge">{{gio(b.details.startsAt)}}–{{gio(b.details.endsAt)}}</span><h3>{{b.title}}</h3>@if(b.status==='XONG'){<p>✓ Đã học</p>}@else{<button appButton variant="secondary" type="button" [disabled]="ban()" (click)="xong(b.id)">Đánh dấu đã học</button>}</article>}@empty{<p class="muted">Chưa có buổi học</p>}@for(h of ngay.han;track h.id){<p class="schedule-item"><span class="badge">Hạn nộp</span> {{h.title}} · {{gio(h.details.dueAt!)}}</p>}</section>}</div><p class="muted">Học từng phiên ngắn. Xem lại bước đã sai trước khi làm bài mới.</p>}
</div><app-hop-thoai [mo]="mo()" tieuDe="Thêm buổi học" [ban]="ban()" (dong)="mo.set(false)"><form class="form-grid" (submit)="luu($event)"><label>Việc học<input [formField]="f.title" placeholder="Ôn xét dấu đạo hàm" /></label><label>Bắt đầu<input type="datetime-local" [formField]="f.startsAt" /></label><label>Kết thúc<input type="datetime-local" [formField]="f.endsAt" /></label>@if(loi()){<p class="field-error" role="alert">{{loi()}}</p>}<button appButton type="submit" [disabled]="ban() || f().invalid()">{{ban()?'Đang lưu…':'Lưu buổi học'}}</button></form></app-hop-thoai>`,styles:`.week-grid{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:8px;min-width:0}.week-grid .surface{padding:16px}.week-grid h2{font-size:16px;margin-bottom:8px}.schedule-item{padding-top:16px;margin-top:16px;border-top:1px solid var(--line)}.schedule-item h3{font-size:14px;font-weight:600;line-height:1.6;margin:8px 0}.schedule-item .btn{width:100%;font-size:12px;padding-inline:8px}.week-grid .surface{overflow-wrap:anywhere}.week-grid .badge{font-variant-numeric:tabular-nums;font-size:11px}@media(min-width:641px) and (max-width:1199px){.week-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:640px){.week-grid{grid-template-columns:1fr;overflow:visible}}`})
export class Lich{
 private readonly api=inject(ApiLopHoc);

 protected readonly ds=this.api.lich();protected readonly nhac=this.api.nhac();protected readonly tuan=signal(0);protected readonly mo=signal(false);protected readonly ban=signal(false);protected readonly loi=signal('');protected readonly model=signal({title:'',startsAt:'',endsAt:''});protected readonly f=form(this.model,p=>{required(p.title);maxLength(p.title,120);required(p.startsAt);required(p.endsAt);});private readonly toast=inject(ThongBaoNhanh);protected readonly giam=(n:number)=>n-1;protected readonly tang=(n:number)=>n+1;
 protected readonly cacNgay=computed(()=>{
  // Làm số học trên ngày dân sự ở UTC để không phụ thuộc múi giờ thiết bị.
  const d=new Date(ngayTruong(new Date())+'T00:00:00Z');
  d.setUTCDate(d.getUTCDate()-((d.getUTCDay()+6)%7)+this.tuan()*7);
  return Array.from({length:7},(_,i)=>{
   const n=new Date(d);n.setUTCDate(n.getUTCDate()+i);
   const iso=n.toISOString().slice(0,10);
   return {iso,ten:['Thứ hai','Thứ ba','Thứ tư','Thứ năm','Thứ sáu','Thứ bảy','Chủ nhật'][i],nhan:iso.slice(8,10)+'/'+iso.slice(5,7),
    buoi:(this.ds.value()??[]).filter(b=>ngayTruong(b.details.startsAt)===iso),
    han:(this.nhac.value()??[]).filter(b=>(b.status==='ON_TAP'||b.status==='KIEM_TRA')&&ngayTruong(b.details.dueAt)===iso)};
  });
 });
 protected readonly nhanTuan=computed(()=>this.cacNgay()[0].nhan+' – '+this.cacNgay()[6].nhan);
 protected gio(s:string){return GIO_TRUONG.format(new Date(s));}

 protected async luu(e:Event){e.preventDefault();if(this.f().invalid()||this.ban())return;const m=this.model();const startsAt=new Date(m.startsAt+'+07:00');const endsAt=new Date(m.endsAt+'+07:00');if(endsAt<=startsAt){this.loi.set('Giờ kết thúc phải sau giờ bắt đầu.');return;}this.ban.set(true);this.loi.set('');try{await firstValueFrom(this.api.themBuoi({title:m.title,startsAt:startsAt.toISOString(),endsAt:endsAt.toISOString()}));this.mo.set(false);this.ds.reload();this.toast.hien('Đã thêm buổi học.');}catch{this.loi.set('Chưa lưu được. Kiểm tra kết nối rồi thử lại.');}finally{this.ban.set(false);}}
 protected async xong(id:string){this.ban.set(true);try{await firstValueFrom(this.api.xongBuoi(id));this.ds.reload();this.toast.hien('Đã ghi nhận buổi học.');}catch{this.toast.hien('Chưa cập nhật được buổi học.','loi');}finally{this.ban.set(false);}}
}
