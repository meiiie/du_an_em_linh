import { Component, inject, Injectable, signal } from '@angular/core';
export type LoaiThongBao='dat'|'chu-y'|'loi';
@Injectable({providedIn:'root'})
export class ThongBaoNhanh {
 readonly cacThongBao=signal<readonly {id:number;noiDung:string;loai:LoaiThongBao}[]>([]);
 private stt=0;
 hien(noiDung:string,loai:LoaiThongBao='dat'):void {
  if(this.cacThongBao().some(t=>t.noiDung===noiDung&&t.loai===loai))return;
  const id=++this.stt;this.cacThongBao.update(t=>[...t,{id,noiDung,loai}]);
  setTimeout(()=>this.dong(id),loai==='dat'?3000:loai==='chu-y'?4000:5000);
 }
 dong(id:number):void {this.cacThongBao.update(t=>t.filter(x=>x.id!==id));}
}
@Component({selector:'app-thong-bao',template:`<div class="thong-bao-nhanh" aria-live="polite">@for(t of thongBao.cacThongBao();track t.id){<div class="toast" [attr.data-loai]="t.loai"><span aria-hidden="true">{{t.loai==='dat'?'✓':t.loai==='loi'?'!':'i'}}</span><p>{{t.noiDung}}</p><button class="nut-icon" type="button" aria-label="Đóng thông báo" (click)="thongBao.dong(t.id)">×</button></div>}</div>`})
export class VungThongBao {protected readonly thongBao=inject(ThongBaoNhanh);}
