import { gomLuotGiao } from './trinh-bay';
import { SuDungGiaSu } from '../../shared/ui/su-dung-gia-su';
import { KichThuocTrang } from '../../shared/ui/kich-thuoc-trang';
import { PhanTrang } from '../../shared/ui/phan-trang';
import { gomHan, hanTheoBo, tenBai, tenKyNang, tenTrangThai } from './trinh-bay';
import { VanBanToan } from '../../shared/toan/van-ban-toan';
import { DinhDangSo, ngayViet } from '../../shared/ui/dinh-dang';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, linkedSignal, resource, signal } from '@angular/core';
import { form, FormField, required, maxLength } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom, fromEvent, takeUntil } from 'rxjs';
import { ApiLopHoc, CheDoHoc } from '../../api/lop-hoc';
import { Muc4, TEN_MUC } from '../../api/hoc-sinh';
import { AssignmentDetails, BankDetails, Item, Result, StudentDetails, StudentProfile } from '../../api/ux';
import { LopDangChon } from '../../core/lop-dang-chon';
import { Katex } from '../../shared/toan/katex';
import { OCongThuc } from '../../shared/toan/o-cong-thuc';
import { Button } from '../../shared/ui/button';
import { HopThoai } from '../../shared/ui/hop-thoai';
import { ThongBaoNhanh } from '../../shared/ui/thong-bao';
import { KhungCho, LoiTai, SoDo, TrangRong } from '../../shared/ui/trang-thai';
export type TrangGv='tong-quan'|'tien-do'|'hoc-sinh'|'ngan-hang'|'tai-lieu'|'cong-thuc'|'duyet'|'giao'|'cai-dat'|'ket-noi-ai';
const TIEU_DE:Record<TrangGv,string>={'tong-quan':'Tổng quan','tien-do':'Mức lớp','hoc-sinh':'Học sinh','ngan-hang':'Đề bài','tai-lieu':'Tài liệu','cong-thuc':'Công thức','duyet':'Duyệt','giao':'Giao bài','cai-dat':'Cài đặt lớp','ket-noi-ai':'Gia sư'};
@Component({selector:'app-phong-giao-vien',imports:[SuDungGiaSu,PhanTrang,VanBanToan,RouterLink,Button,HopThoai,Katex,OCongThuc,FormField,KhungCho,LoiTai,SoDo,TrangRong,DinhDangSo],templateUrl:'./phong-giao-vien.html'})
export class PhongGiaoVien{
 protected readonly soMoiTrang = inject(KichThuocTrang).soMuc;
 readonly trang=input<TrangGv>('tong-quan');readonly id=input('');protected readonly lop=inject(LopDangChon);protected readonly tieuDe=computed(()=>this.trang()==='tong-quan'&&this.lop.ds().find(l=>l.id===this.lop.id()) ? 'Lớp '+this.lop.ds().find(l=>l.id===this.lop.id())!.title : TIEU_DE[this.trang()]);protected readonly tenMuc=TEN_MUC;protected readonly cacMuc=Object.keys(TEN_MUC) as Muc4[];protected readonly tim=signal('');protected readonly timBaiGiao=signal('');protected readonly timHsGiao=signal('');protected readonly mucLoc=signal('');protected readonly congLoc=signal('');protected readonly ban=signal(false);protected readonly loi=signal('');protected readonly hop=signal<'duyet'|'muc'|'cong-thuc'|'bai'|'bai-ke'|'tai-lieu'|'soan-nhap'|null>(null);protected readonly mucDangChon=signal<Item|null>(null);protected readonly chapThuan=signal(true);protected readonly lyDo=signal('');protected readonly kyNang=signal('');protected readonly mucMoi=signal<Muc4>('NHAN_BIET');protected readonly chonBai=signal<readonly string[]>([]);protected readonly chonHs=signal<readonly string[]>([]);protected readonly file=signal<File|null>(null);private readonly http=inject(HttpClient);private readonly api=inject(ApiLopHoc);private readonly toast=inject(ThongBaoNhanh);
 private url(path:string){return this.api.duongGv(path,this.lop.id());}
 protected readonly tongQuan=this.api.trangTongQuan(()=>this.trang()==='tong-quan'?this.lop.id():undefined);
 protected readonly canhBao=this.api.canhBao(()=>this.trang()==='tong-quan'?this.lop.id():undefined);
 protected readonly tienDo=this.api.tienDo(()=>['tien-do','giao','ngan-hang','ket-noi-ai'].includes(this.trang())?this.lop.id():undefined);
 protected readonly hocSinh=this.api.hocSinh(()=>this.lop.id(),()=>this.trang()==='hoc-sinh'?this.id():undefined);
 protected readonly kyNangLop=this.api.kyNangGv(()=>this.trang()==='ngan-hang'?this.lop.id():undefined);
 protected readonly nganHang=this.api.nganHang(()=>['ngan-hang','giao','hoc-sinh','duyet'].includes(this.trang())?this.lop.id():undefined);
 protected readonly ds=this.api.danhSachGv(()=>{const t=this.trang();return t==='duyet'||t==='giao'||t==='cong-thuc'?t:undefined;},()=>this.lop.id());
 protected readonly kho=this.api.khoGv(()=>this.trang()==='tai-lieu'?this.lop.id():undefined);
 protected readonly caiDat=this.api.caiDat(()=>['cai-dat','ket-noi-ai'].includes(this.trang())?this.lop.id():undefined);
 // Dùng dòng bộ của tổng quan; chỉ tải lịch sử cho hợp đồng cũ.
 protected readonly hanBai = resource({
  params: () => this.trang() === 'tong-quan' && this.lop.id() && this.tongQuan.hasValue()
   ? { lopId: this.lop.id(), deadlines: this.tongQuan.value().deadlines } : undefined,
  loader: async ({ params: { lopId, deadlines }, abortSignal }) => {
   if (deadlines.every(a => typeof a.details.assignmentCount === 'number')) return hanTheoBo(deadlines);
   const get = <T>(path: string) => firstValueFrom(this.http.get<T>(this.api.duongGv(path, lopId)).pipe(takeUntil(fromEvent(abortSignal, 'abort'))));
   const [assignments, bank, students] = await Promise.all([get<Item<AssignmentDetails>[]>('giao'), get<Item<BankDetails>[]>('ngan-hang'), get<Item<StudentDetails>[]>('tien-do')]);
   const histories = new Map<string, StudentProfile>();
   const ids = [...new Set(assignments.filter(a => a.details.dueAt && typeof a.details.submitted !== 'boolean').map(a => String(a.details['studentId'])))].filter(id => students.find(s => s.id === id)?.details.submitted !== 0);
   // Giới hạn bốn yêu cầu, hủy khi chuyển lớp/trang. Chỉ lấy lịch sử, không lấy lời giải.
   for (let i = 0; i < ids.length; i += 4) {
    if (abortSignal.aborted) throw new Error('Đã hủy');
    await Promise.all(ids.slice(i, i + 4).map(async id => histories.set(id, await get<StudentProfile>('hoc-sinh/' + encodeURIComponent(id)))));
   }
   const skillNames = new Map(students.flatMap(s => s.details.skills.map(k => [k.code, k.name] as const)));
   return gomHan(assignments, bank, histories, skillNames);
  },
 });
 protected readonly ngay = ngayViet;
 protected readonly tenBai = tenBai;
 protected tenBaiTheoMa(code:string, macDinh='Bài được giao'){const b=(this.nganHang.value()??[]).find(b=>(b.details.code||b.title)===code);return b?tenBai(b):macDinh;}
 protected readonly tenKyNang = tenKyNang;
 protected readonly phanBo = computed(() => {
  const d = this.tongQuan.hasValue() ? this.tongQuan.value().distribution : {};
  const total = this.cacMuc.reduce((n,m) => n + (d[m] ?? 0), 0);
  return this.cacMuc.map((m,i) => ({m, i, count: d[m] ?? 0, ratio: total ? (d[m] ?? 0) / total : 0}));
 });
 protected readonly tenKyNangTheoMa = computed(() => new Map((this.tienDo.hasValue() ? this.tienDo.value() : []).flatMap(s => s.details.skills.map(k => [k.code, k.name] as const))));
 protected readonly caiModel=linkedSignal(()=>({revealSolutionAfterSubmit:this.caiDat.hasValue()?this.caiDat.value().revealSolutionAfterSubmit:false,aiProvider:this.caiDat.hasValue()&&this.caiDat.value().aiProvider==='glm'?'glm':'offline'}));protected readonly caiForm=form(this.caiModel);
 protected readonly giaoModel=signal<{setName:string;mode:CheDoHoc;dueAt:string}>({setName:'',mode:'ON_TAP',dueAt:''});protected readonly giaoForm=form(this.giaoModel,p=>{required(p.setName);maxLength(p.setName,120);});
 protected readonly congModel=signal({title:'',latex:'',statement:''});protected readonly congForm=form(this.congModel,p=>{required(p.title);maxLength(p.title,120);required(p.latex);maxLength(p.latex,2000);required(p.statement);maxLength(p.statement,2000);});
 protected readonly baiModel=linkedSignal({source:()=>this.lop.id(),computation:()=>({code:'',skillCode:'',level4:'NHAN_BIET',statement:'',latex:'',function:''})});protected readonly baiForm=form(this.baiModel,p=>{required(p.code);required(p.skillCode);required(p.statement);required(p.latex);required(p.function);maxLength(p.code,120);maxLength(p.statement,2000);maxLength(p.latex,2000);maxLength(p.function,2000);});
 protected readonly danhMucKyNang=computed(()=>{
  const skills=new Map<string,string>();
  for(const k of this.kyNangLop.value()??[])skills.set(k.id,tenKyNang(k.id,k.title));
  for(const b of this.nganHang.value()??[])if(b.details.skillCode)skills.set(b.details.skillCode,tenKyNang(b.details.skillCode,b.details.skillName));
  for(const hs of this.tienDo.value()??[])for(const k of hs.details.skills)skills.set(k.code,k.name);
  return [...skills].map(([code,name])=>({code,name})).sort((a,b)=>a.name.localeCompare(b.name,'vi'));
 });
 protected readonly kyNangHopLe=computed(()=>this.danhMucKyNang().some(k=>k.code===this.baiModel().skillCode));
 protected readonly baiKeId=signal('');
 protected readonly docModel=signal({title:'',licenseStatus:'tu_soan'});protected readonly docForm=form(this.docModel,p=>{required(p.title);maxLength(p.title,120);});
 protected readonly baiLoc=computed(()=>{const ds=this.nganHang.hasValue()?this.nganHang.value():[];const q=this.tim().toLocaleLowerCase('vi');return ds.filter(b=>(!q||(b.title+' '+b.subtitle+' '+b.details.skillCode+' '+tenKyNang(b.details.skillCode,b.details.skillName||this.tenKyNangTheoMa().get(b.details.skillCode))).toLocaleLowerCase('vi').includes(q))&&(!this.mucLoc()||b.details.level4===this.mucLoc())&&(!this.congLoc()||b.details.gate===this.congLoc()));});
 protected readonly baiGiao=computed(()=>this.nganHang.hasValue()?this.nganHang.value().filter(b=>b.status==='DA_PHAT_HANH'&&(b.title+' '+b.subtitle+' '+b.details.skillCode+' '+tenKyNang(b.details.skillCode,b.details.skillName||this.tenKyNangTheoMa().get(b.details.skillCode))).toLocaleLowerCase('vi').includes(this.timBaiGiao().trim().toLocaleLowerCase('vi'))):[]);
 protected readonly hsGiao=computed(()=>(this.tienDo.hasValue()?this.tienDo.value():[]).filter(h=>h.title.toLocaleLowerCase('vi').includes(this.timHsGiao().trim().toLocaleLowerCase('vi'))));
 protected readonly luotGiao=computed(()=>gomLuotGiao(this.ds.hasValue()?this.ds.value():[]));
 protected readonly noiDungLoc=computed(()=>(this.trang()==='giao'?this.luotGiao():(this.ds.hasValue()?this.ds.value():[])).filter(d=>(d.title+' '+d.subtitle).toLocaleLowerCase('vi').includes(this.tim().trim().toLocaleLowerCase('vi'))));
 protected readonly taiLieuLoc=computed(()=>(this.kho.hasValue()?this.kho.value().documents:[]).filter(d=>(d.title+' '+d.subtitle).toLocaleLowerCase('vi').includes(this.tim().trim().toLocaleLowerCase('vi'))));
 protected readonly moTa=computed(()=>({
  'tong-quan':'Theo dõi tiến độ, bài cần duyệt và hạn nộp của lớp.',
  'tien-do':'Xem mức từng kỹ năng để hỗ trợ đúng học sinh.',
  'hoc-sinh':'Xem bài làm, xác nhận mức hiểu và chọn bài tiếp theo.',
  'ngan-hang':'Tìm câu hỏi theo kỹ năng, mức và kết quả kiểm định.',
  'giao':'Chọn bài và học sinh, rồi đặt chế độ và hạn nộp.',
  'tai-lieu':'Quản lý tài liệu học sinh và gia sư được phép sử dụng.',
  'cong-thuc':'Kiểm công thức trước khi đưa vào kho kiến thức của lớp.',
  'duyet':'Xem bài cần thầy cô xác nhận và ghi rõ lý do quyết định.',
  'cai-dat':'Quản lý thời điểm mở lời giải và gia sư của lớp.',
  'ket-noi-ai':'Gợi ý theo bước, giúp học sinh tự tìm cách làm.'
 }[this.trang()]));
 protected readonly hsLoc=computed(()=>(this.tienDo.hasValue()?this.tienDo.value():[]).filter(h=>h.title.toLocaleLowerCase('vi').includes(this.tim().toLocaleLowerCase('vi'))));
 protected readonly trangLichSu=linkedSignal({source:()=>({ds:this.hocSinh.value()?.history, size:this.soMoiTrang()}),computation:()=>1});
 protected readonly trangBai=linkedSignal({source:()=>({ds:this.baiLoc(), size:this.soMoiTrang()}),computation:()=>1});
 protected readonly trangHs=linkedSignal({source:()=>({ds:this.hsLoc(), size:this.soMoiTrang()}),computation:()=>1});
 protected readonly trangNoiDung=linkedSignal({source:()=>({ds:this.noiDungLoc(), size:this.soMoiTrang()}),computation:()=>1});
 protected readonly trangTaiLieu=linkedSignal({source:()=>({ds:this.taiLieuLoc(), size:this.soMoiTrang()}),computation:()=>1});
 protected readonly trangBaiGiao=linkedSignal({source:()=>({ds:this.baiGiao(), size:this.soMoiTrang()}),computation:()=>1});
 protected readonly trangHsGiao=linkedSignal({source:()=>({ds:this.hsGiao(), size:this.soMoiTrang()}),computation:()=>1});
 constructor(){effect(()=>{this.trang();this.tim.set('');this.mucLoc.set('');this.congLoc.set('');});void this.lop.tai();effect(()=>{if(this.tongQuan.hasValue())this.lop.soChoDuyet.set(this.tongQuan.value().pending);if(this.trang()==='duyet'&&this.ds.hasValue())this.lop.soChoDuyet.set(this.ds.value().length);});}
 protected suaLatex(latex:string){return (m:{title:string;latex:string;statement:string})=>({...m,latex});}
 protected thayLop(e:Event){this.lop.id.set((e.target as HTMLSelectElement).value);this.chonBai.set([]);this.chonHs.set([]);this.loi.set('');this.tim.set('');this.timBaiGiao.set('');this.timHsGiao.set('');}
 protected readonly congTen=tenTrangThai;
 protected ketQuaBaiNop(b:Item):string {
  const result=typeof b.details['result']==='string'&&b.details['result']?b.details['result']:b.subtitle;
  return ({DAT:'Đạt',SAI:'Sai',KHONG_KIEM_DUOC:'Không kiểm được'} as Record<string,string>)[result]??tenTrangThai(result);
 }
 protected chiTiet(c:Item):string {const d=c.details;return [d['statement'],d['tier1'],d['tier2']].filter(x=>typeof x==='string').join(' · ');}
 protected doiChon(id:string,loai:'bai'|'hs'){const s=loai==='bai'?this.chonBai:this.chonHs;s.update(ds=>ds.includes(id)?ds.filter(x=>x!==id):[...ds,id]);}
 protected duyet(m:Item,approve:boolean){this.mucDangChon.set(m);this.chapThuan.set(approve);this.lyDo.set('');this.loi.set('');this.hop.set('duyet');}
 protected suaMuc(code:string,level:Muc4){this.kyNang.set(code);this.mucMoi.set(level);this.lyDo.set('');this.loi.set('');this.hop.set('muc');}
 protected async ghi(path:string,body:unknown,method:'post'|'put'='post'):Promise<boolean>{if(this.ban())return false;this.ban.set(true);this.loi.set('');try{const k=await firstValueFrom(method==='put'?this.http.put<Result>(path,body):this.http.post<Result>(path,body));this.toast.hien(k.message||'Đã lưu.');return true;}catch(e){const d=e instanceof HttpErrorResponse?e.error?.detail:null;this.loi.set(typeof d==='string'?d:'Chưa lưu được. Kiểm tra kết nối rồi thử lại.');return false;}finally{this.ban.set(false);}}
 protected async luuDuyet(e:Event){e.preventDefault();if(!this.lyDo().trim()||!this.mucDangChon())return;if(await this.ghi('/api/gv/duyet/'+encodeURIComponent(this.mucDangChon()!.id),{lopId:this.lop.id(),approve:this.chapThuan(),reason:this.lyDo()})){this.hop.set(null);this.ds.reload();}}
 protected async ghiMuc(e:Event){e.preventDefault();if(!this.lyDo().trim())return;if(await this.ghi('/api/gv/muc-hieu',{lopId:this.lop.id(),studentId:this.id(),skillCode:this.kyNang(),level4:this.mucMoi(),reason:this.lyDo()})){this.hop.set(null);this.hocSinh.reload();}}
 protected async giao(e:Event){e.preventDefault();if(this.giaoForm().invalid()||!this.chonBai().length)return;const m=this.giaoModel();if(await this.ghi('/api/gv/giao',{lopId:this.lop.id(),studentIds:this.chonHs(),problemIds:this.chonBai(),mode:m.mode,dueAt:m.dueAt?new Date(m.dueAt+'+07:00').toISOString():null,setName:m.setName})){this.chonBai.set([]);this.ds.reload();}}
 protected readonly taiLieuSoan=signal('');
 protected async soanNhap(e:Event){e.preventDefault();if(!this.taiLieuSoan()||this.ban())return;
  if(await this.ghi('/api/gv/soan-nhap',{lopId:this.lop.id(),documentId:this.taiLieuSoan()})){
   this.hop.set(null);this.ds.reload();this.toast.hien('Đã đưa bản nháp vào hàng chờ duyệt.');
  }
 }
 protected async themBai(e:Event){e.preventDefault();if(this.baiForm().invalid()||!this.kyNangHopLe())return;if(await this.ghi('/api/gv/ngan-hang',{lopId:this.lop.id(),...this.baiModel()})){this.hop.set(null);this.nganHang.reload();}}
 protected async ghiBaiKe(e:Event){e.preventDefault();if(!this.lyDo().trim()||!this.baiKeId())return;if(await this.ghi('/api/gv/goi-bai',{lopId:this.lop.id(),studentId:this.id(),problemId:this.baiKeId(),reason:this.lyDo()}))this.hop.set(null);}
 protected async themCong(e:Event){e.preventDefault();if(this.congForm().invalid())return;if(await this.ghi('/api/gv/cong-thuc',{lopId:this.lop.id(),...this.congModel()})){this.hop.set(null);this.congModel.set({title:'',latex:'',statement:''});this.ds.reload();}}
 protected async kiemCong(){if(await this.ghi(this.url('cong-thuc/kiem'),{}))this.ds.reload();}
 protected chonFile(e:Event){this.file.set((e.target as HTMLInputElement).files?.[0]??null);}
 protected async taiTaiLieu(e:Event){e.preventDefault();const f=this.file();if(!f||this.docForm().invalid())return;if(f.size>5*1024*1024){this.loi.set('Chọn tệp không quá 5 MB.');return;}const body=new FormData();body.set('lopId',this.lop.id());body.set('title',this.docModel().title);body.set('licenseStatus',this.docModel().licenseStatus);body.set('file',f);if(await this.ghi('/api/gv/tai-lieu',body)){this.file.set(null);this.hop.set(null);this.kho.reload();}}
 protected async luuCai(e:Event){e.preventDefault();await this.ghi('/api/gv/cai-dat',{lopId:this.lop.id(),...this.caiModel()},'put');}
}
