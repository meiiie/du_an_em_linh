import { Component, inject, linkedSignal } from '@angular/core';
import { ApiLopHoc } from '../../api/lop-hoc';
import { SuDungGiaSu } from '../../shared/ui/su-dung-gia-su';
import { KhungCho, LoiTai } from '../../shared/ui/trang-thai';
@Component({selector:'app-su-dung-ai-quan-tri',imports:[SuDungGiaSu,KhungCho,LoiTai],template:`
 <div class="page"><header><h1>Sử dụng AI</h1><p>Theo dõi lượt gọi và token gia sư theo lớp.</p></header>
 @if(lop.isLoading()){<app-khung-cho />}@else if(lop.error()){<app-loi-tai [loi]="lop.error()" (thuLai)="lop.reload()" />}@else{
 <label>Lớp<select [value]="lopId()" (change)="lopId.set($any($event.target).value)">@for(l of lop.value();track l.id){<option [value]="l.id">{{l.title}}</option>}</select></label>
 @if(lopId()){<app-su-dung-gia-su [lopId]="lopId()" vai="qt" [hocSinh]="nguoi.value() ?? []" />}
 @if(nguoi.error()){<app-loi-tai [loi]="nguoi.error()" moTa="Chưa tải được tên học sinh." (thuLai)="nguoi.reload()" />}}
 </div>`})
export class SuDungAiQuanTri {
 private readonly api=inject(ApiLopHoc);
 protected readonly lop=this.api.quanTri(()=> 'lop');
 protected readonly nguoi=this.api.nguoiDung(()=>true);
 protected readonly lopId=linkedSignal(()=>this.lop.value()?.[0]?.id ?? '');
}
