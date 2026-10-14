import { VanBanToan } from '../../shared/toan/van-ban-toan';
import { DinhDangSo } from '../../shared/ui/dinh-dang';
import { ApiLopHoc } from '../../api/lop-hoc';
import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Katex } from '../../shared/toan/katex';
import { Button } from '../../shared/ui/button';
import { HopThoai } from '../../shared/ui/hop-thoai';
import { ThongBaoNhanh } from '../../shared/ui/thong-bao';
import { KhungCho, LoiTai, TrangRong } from '../../shared/ui/trang-thai';
@Component({selector:'app-kho',imports:[VanBanToan,Katex,Button,HopThoai,KhungCho,LoiTai,TrangRong,DinhDangSo],template:`
<div class="page"><header><h1>Công thức và tài liệu</h1><p>Kho kiến thức của lớp em.</p></header><div class="toolbar"><label>Tìm trong kho<input type="search" [value]="tim()" (input)="tim.set($any($event.target).value)" placeholder="Tên công thức hoặc nội dung tài liệu" /></label></div>
@if(kho.isLoading()){<app-khung-cho />}@else if(kho.error()){<app-loi-tai [loi]="kho.error()" moTa="Chưa tải được công thức và tài liệu của lớp." (thuLai)="kho.reload()" />}@else if(kho.value();as k){
<div class="toolbar" role="group" aria-label="Nội dung lớp"><button appButton variant="secondary" type="button" [attr.aria-pressed]="tab()==='cong-thuc'" (click)="tab.set('cong-thuc')">Công thức</button><button appButton variant="secondary" type="button" [attr.aria-pressed]="tab()==='tai-lieu'" (click)="tab.set('tai-lieu')">Tài liệu</button></div>
@if(tab()==='cong-thuc'){<section class="surface"><div class="toolbar"><h2>Công thức</h2><span class="list-summary">{{congLoc().length | soViet}} công thức</span>@if(k.formulas.length){<details class="document-details"><summary>Công cụ ôn thêm</summary><div class="actions"><button appButton variant="secondary" type="button" (click)="moThe()">Ôn bằng thẻ</button><button appButton variant="secondary" type="button" [disabled]="dangXuat()" (click)="xuat()">Xuất CSV</button></div></details>}</div>
@if(!k.formulas.length){<app-trang-rong tieuDe="Chưa có công thức" moTa="Công thức thầy cô đã kiểm sẽ hiện ở đây." />}@else{<ul class="row-list">@for(c of congLoc();track c.id){<li [id]="'ct-'+c.id"><h3>{{c.title}}</h3><app-katex bang [latex]="c.subtitle" /><p><app-van-ban-toan [text]="c.details.statement" /></p></li>}@empty{<li>Không có kết quả phù hợp. Đổi từ khóa để tìm lại.</li>}</ul>}</section>}
@if(tab()==='tai-lieu'){<section class="surface"><div class="section-heading"><h2>Tài liệu</h2><span class="list-summary">{{taiLieuLoc().length | soViet}} tài liệu</span></div>@if(!k.documents.length){<app-trang-rong tieuDe="Chưa có tài liệu" moTa="Tài liệu của lớp sẽ hiện ở đây." />}@else{<ul class="row-list">@for(d of taiLieuLoc();track d.id){<li [id]="'tl-'+d.id"><h3>{{d.title}}</h3><details class="document-details"><summary>Xem nội dung tài liệu</summary><p class="document-text">{{d.subtitle}}</p></details></li>}@empty{<li>Không có kết quả phù hợp. Đổi từ khóa để tìm lại.</li>}</ul>}</section>}}
</div>
<app-hop-thoai [mo]="theMo()" tieuDe="Ôn công thức" (dong)="theMo.set(false)">@if(the();as c){<p class="muted">Thẻ {{(viTri()+1) | soViet}} / {{kho.value()?.formulas?.length | soViet}}</p><button class="flashcard" type="button" [attr.aria-label]="lat() ? 'Mặt sau. Lật về câu hỏi' : 'Mặt trước. Lật xem công thức'" [attr.aria-pressed]="lat()" (click)="lat.update(doi)">@if(lat()){<app-katex bang [latex]="c.subtitle" /><span><app-van-ban-toan [text]="c.details.statement" /></span>}@else{<strong>{{c.title}}</strong><span class="muted">Nhấn Enter hoặc chạm để lật</span>}</button><div class="actions"><button appButton variant="secondary" type="button" [disabled]="viTri()===0" (click)="chuyen(-1)">Thẻ trước</button><button appButton type="button" [disabled]="viTri()+1 >= (kho.value()?.formulas?.length ?? 0)" (click)="chuyen(1)">Thẻ tiếp</button></div>}</app-hop-thoai>`,styles:`.document-text{white-space:pre-wrap}.flashcard{display:grid;gap:24px;place-items:center;min-height:240px;width:100%;padding:24px;margin-bottom:24px;border:1px solid var(--line-strong);border-radius:12px;background:var(--raise);color:var(--ink);font:inherit}.flashcard strong{font-size:20px}`})
export class Kho{
 private readonly api=inject(ApiLopHoc);

 protected readonly tim=signal('');
 protected readonly congLoc=computed(()=>(this.kho.hasValue()?this.kho.value().formulas:[]).filter(c=>(c.title+' '+c.subtitle+' '+c.details.statement).toLocaleLowerCase('vi').includes(this.tim().trim().toLocaleLowerCase('vi'))));
 protected readonly taiLieuLoc=computed(()=>(this.kho.hasValue()?this.kho.value().documents:[]).filter(d=>(d.title+' '+d.subtitle).toLocaleLowerCase('vi').includes(this.tim().trim().toLocaleLowerCase('vi'))));
 protected readonly tab=signal<'cong-thuc'|'tai-lieu'>('cong-thuc');
 protected readonly kho=this.api.khoHocSinh();protected readonly theMo=signal(false);protected readonly viTri=signal(0);protected readonly lat=signal(false);protected readonly dangXuat=signal(false);protected readonly doi=(v:boolean)=>!v;protected readonly the=computed(()=>this.kho.hasValue() ? this.kho.value().formulas[this.viTri()] : undefined);private readonly thongBao=inject(ThongBaoNhanh);
 protected moThe(){this.viTri.set(0);this.lat.set(false);this.theMo.set(true);}protected chuyen(d:number){this.viTri.update(v=>v+d);this.lat.set(false);}
 protected async xuat(){this.dangXuat.set(true);try{const c=await firstValueFrom(this.api.xuatThe());const url=URL.createObjectURL(new Blob([c.content.startsWith('\uFEFF')?c.content:'\uFEFF'+c.content],{type:'text/csv;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download=c.filename;a.click();URL.revokeObjectURL(url);this.thongBao.hien('Đã xuất bảng công thức.');}catch{this.thongBao.hien('Chưa xuất được. Thử lại sau.','loi');}finally{this.dangXuat.set(false);}}
}
