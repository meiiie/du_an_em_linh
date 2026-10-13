import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, input, linkedSignal, signal } from '@angular/core';
import { form, FormField, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Muc4, TEN_MUC } from '../../api/hoc-sinh';
import { BankDetails, Dashboard, FormulaDetails, Item, Result, Settings, StudentDetails, StudentProfile } from '../../api/ux';
import { LopDangChon } from '../../core/lop-dang-chon';
import { Katex } from '../../shared/toan/katex';
import { OCongThuc } from '../../shared/toan/o-cong-thuc';
import { Button } from '../../shared/ui/button';
import { HopThoai } from '../../shared/ui/hop-thoai';
import { ThongBaoNhanh } from '../../shared/ui/thong-bao';
import { KhungCho, LoiTai, SoDo, TrangRong } from '../../shared/ui/trang-thai';
export type TrangGv='tong-quan'|'tien-do'|'hoc-sinh'|'ngan-hang'|'tai-lieu'|'cong-thuc'|'duyet'|'giao'|'cai-dat'|'ket-noi-ai';
const TIEU_DE:Record<TrangGv,string>={'tong-quan':'Tổng quan','tien-do':'Mức lớp','hoc-sinh':'Học sinh','ngan-hang':'Đề bài','tai-lieu':'Tài liệu','cong-thuc':'Công thức','duyet':'Duyệt','giao':'Giao bài','cai-dat':'Cài đặt lớp','ket-noi-ai':'Gia sư'};
@Component({selector:'app-phong-giao-vien',imports:[RouterLink,Button,HopThoai,Katex,OCongThuc,FormField,KhungCho,LoiTai,SoDo,TrangRong],templateUrl:'./phong-giao-vien.html'})
export class PhongGiaoVien{
 readonly trang=input<TrangGv>('tong-quan');readonly id=input('');protected readonly lop=inject(LopDangChon);protected readonly tieuDe=computed(()=>TIEU_DE[this.trang()]);protected readonly tenMuc=TEN_MUC;protected readonly cacMuc=Object.keys(TEN_MUC) as Muc4[];protected readonly chuKy=signal(30);protected readonly tim=signal('');protected readonly mucLoc=signal('');protected readonly congLoc=signal('');protected readonly ban=signal(false);protected readonly loi=signal('');protected readonly hop=signal<'duyet'|'muc'|'cong-thuc'|null>(null);protected readonly mucDangChon=signal<Item|null>(null);protected readonly chapThuan=signal(true);protected readonly lyDo=signal('');protected readonly kyNang=signal('');protected readonly mucMoi=signal<Muc4>('NHAN_BIET');protected readonly chonBai=signal<readonly string[]>([]);protected readonly chonHs=signal<readonly string[]>([]);protected readonly file=signal<File|null>(null);private readonly http=inject(HttpClient);private readonly toast=inject(ThongBaoNhanh);
 private url(path:string){return '/api/gv/'+path+'?lopId='+encodeURIComponent(this.lop.id());}
 protected readonly tongQuan=httpResource<Dashboard>(()=>this.lop.id()&&this.trang()==='tong-quan'?this.url('tong-quan')+'&days='+this.chuKy():undefined);
 protected readonly tienDo=httpResource<Item<StudentDetails>[]>(()=>this.lop.id()&&['tien-do','giao'].includes(this.trang())?this.url('tien-do'):undefined);
 protected readonly hocSinh=httpResource<StudentProfile>(()=>this.lop.id()&&this.trang()==='hoc-sinh'&&this.id()?this.url('hoc-sinh/'+encodeURIComponent(this.id())):undefined);
 protected readonly nganHang=httpResource<Item<BankDetails>[]>(()=>this.lop.id()&&['ngan-hang','giao'].includes(this.trang())?this.url('ngan-hang'):undefined);
 protected readonly ds=httpResource<Item[]>(()=>{if(!this.lop.id())return undefined;const t=this.trang();return ['duyet','giao','cong-thuc'].includes(t)?this.url(t):undefined;});
 protected readonly kho=httpResource<{documents:Item[]}>(()=>this.lop.id()&&this.trang()==='tai-lieu'?this.url('kho'):undefined);
 protected readonly caiDat=httpResource<Settings>(()=>this.lop.id()&&['cai-dat','ket-noi-ai'].includes(this.trang())?this.url('cai-dat'):undefined);
 protected readonly caiModel=linkedSignal(()=>({revealSolutionAfterSubmit:this.caiDat.hasValue()?this.caiDat.value().revealSolutionAfterSubmit:false,aiProvider:'offline'}));protected readonly caiForm=form(this.caiModel);
 protected readonly giaoModel=signal({setName:'',mode:'ON_TAP',dueAt:''});protected readonly giaoForm=form(this.giaoModel,p=>required(p.setName));
 protected readonly congModel=signal({title:'',latex:'',statement:''});protected readonly congForm=form(this.congModel,p=>{required(p.title);required(p.latex);required(p.statement);});
 protected readonly docModel=signal({title:'',licenseStatus:'tu_soan'});protected readonly docForm=form(this.docModel,p=>required(p.title));
 protected readonly baiLoc=computed(()=>{const ds=this.nganHang.hasValue()?this.nganHang.value():[];const q=this.tim().toLocaleLowerCase('vi');return ds.filter(b=>(!q||(b.title+' '+b.subtitle+' '+b.details.skillCode).toLocaleLowerCase('vi').includes(q))&&(!this.mucLoc()||b.details.level4===this.mucLoc())&&(!this.congLoc()||b.details.gate===this.congLoc()));});
 protected readonly hsLoc=computed(()=>(this.tienDo.hasValue()?this.tienDo.value():[]).filter(h=>h.title.toLocaleLowerCase('vi').includes(this.tim().toLocaleLowerCase('vi'))));
 constructor(){void this.lop.tai();effect(()=>{if(this.tongQuan.hasValue())this.lop.soChoDuyet.set(this.tongQuan.value().pending);if(this.trang()==='duyet'&&this.ds.hasValue())this.lop.soChoDuyet.set(this.ds.value().length);});}
 protected suaLatex(latex:string){return (m:{title:string;latex:string;statement:string})=>({...m,latex});}
 protected thayLop(e:Event){this.lop.id.set((e.target as HTMLSelectElement).value);this.chonBai.set([]);this.chonHs.set([]);this.loi.set('');}
 protected congTen(s:string){return ({DAT:'✓ Đạt',SAI:'! Sai',KHONG_KIEM_DUOC:'Chờ giáo viên',GV_DUYET:'✓ Giáo viên duyệt',DA_PHAT_HANH:'Đã phát hành',NHAP:'Bản nháp',KHOA:'Đã khóa',BI_CHAN:'Bị chặn'} as Record<string,string>)[s]??s;}
 protected chiTiet(c:Item):string {const d=c.details;return [d['statement'],d['tier1'],d['tier2']].filter(x=>typeof x==='string').join(' · ');}
 protected doiChon(id:string,loai:'bai'|'hs'){const s=loai==='bai'?this.chonBai:this.chonHs;s.update(ds=>ds.includes(id)?ds.filter(x=>x!==id):[...ds,id]);}
 protected duyet(m:Item,approve:boolean){this.mucDangChon.set(m);this.chapThuan.set(approve);this.lyDo.set('');this.loi.set('');this.hop.set('duyet');}
 protected suaMuc(code:string,level:Muc4){this.kyNang.set(code);this.mucMoi.set(level);this.lyDo.set('');this.loi.set('');this.hop.set('muc');}
 protected async ghi(path:string,body:unknown,method:'post'|'put'='post'):Promise<boolean>{if(this.ban())return false;this.ban.set(true);this.loi.set('');try{const k=await firstValueFrom(method==='put'?this.http.put<Result>(path,body):this.http.post<Result>(path,body));this.toast.hien(k.message||'Đã lưu.');return true;}catch(e){const d=e instanceof HttpErrorResponse?e.error?.detail:null;this.loi.set(typeof d==='string'?d:'Chưa lưu được. Kiểm tra kết nối rồi thử lại.');return false;}finally{this.ban.set(false);}}
 protected async luuDuyet(e:Event){e.preventDefault();if(!this.lyDo().trim()||!this.mucDangChon())return;if(await this.ghi('/api/gv/duyet/'+encodeURIComponent(this.mucDangChon()!.id),{lopId:this.lop.id(),approve:this.chapThuan(),reason:this.lyDo()})){this.hop.set(null);this.ds.reload();}}
 protected async ghiMuc(e:Event){e.preventDefault();if(!this.lyDo().trim())return;if(await this.ghi('/api/gv/muc-hieu',{lopId:this.lop.id(),studentId:this.id(),skillCode:this.kyNang(),level4:this.mucMoi(),reason:this.lyDo()})){this.hop.set(null);this.hocSinh.reload();}}
 protected async giao(e:Event){e.preventDefault();if(this.giaoForm().invalid()||!this.chonBai().length)return;const m=this.giaoModel();if(await this.ghi('/api/gv/giao',{lopId:this.lop.id(),studentIds:this.chonHs(),problemIds:this.chonBai(),mode:m.mode,dueAt:m.dueAt?new Date(m.dueAt).toISOString():null,setName:m.setName})){this.chonBai.set([]);this.ds.reload();}}
 protected async themCong(e:Event){e.preventDefault();if(this.congForm().invalid())return;if(await this.ghi('/api/gv/cong-thuc',{lopId:this.lop.id(),...this.congModel()})){this.hop.set(null);this.congModel.set({title:'',latex:'',statement:''});this.ds.reload();}}
 protected async kiemCong(){if(await this.ghi(this.url('cong-thuc/kiem'),{}))this.ds.reload();}
 protected chonFile(e:Event){this.file.set((e.target as HTMLInputElement).files?.[0]??null);}
 protected async taiTaiLieu(e:Event){e.preventDefault();const f=this.file();if(!f||this.docForm().invalid())return;if(f.size>5*1024*1024){this.loi.set('Chọn tệp không quá 5 MB.');return;}const body=new FormData();body.set('lopId',this.lop.id());body.set('title',this.docModel().title);body.set('licenseStatus',this.docModel().licenseStatus);body.set('file',f);if(await this.ghi('/api/gv/tai-lieu',body)){this.file.set(null);this.kho.reload();}}
 protected async luuCai(e:Event){e.preventDefault();await this.ghi('/api/gv/cai-dat',{lopId:this.lop.id(),...this.caiModel()},'put');}
}
