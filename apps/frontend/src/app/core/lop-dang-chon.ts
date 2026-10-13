import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { HangChoDuyet } from './hang-cho';
import { ApiLopHoc } from '../api/lop-hoc';
import { Item } from '../api/ux';
@Injectable({providedIn:'root'})
export class LopDangChon{
 readonly ds=signal<readonly Item[]>([]);readonly id=signal('');readonly loi=signal(false);readonly dangTai=signal(false);readonly soChoDuyet=inject(HangChoDuyet).so;private readonly api=inject(ApiLopHoc);private daTai=false;
 async tai(){if(this.daTai||this.dangTai())return;this.dangTai.set(true);this.loi.set(false);try{const ds=await firstValueFrom(this.api.cacLop());this.ds.set(ds);this.id.set(ds[0]?.id??'');this.daTai=true;if(ds[0]){try{const q=await firstValueFrom(this.api.tongQuan(ds[0].id));this.soChoDuyet.set(q.pending);}catch{/* Badge is optional; the page has its own retry state. */}}}catch{this.loi.set(true);}finally{this.dangTai.set(false);}}
}
