import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Item } from '../api/ux';
@Injectable({providedIn:'root'})
export class LopDangChon{
 readonly ds=signal<readonly Item[]>([]);readonly id=signal('');readonly loi=signal(false);readonly dangTai=signal(false);readonly soChoDuyet=signal(0);private readonly http=inject(HttpClient);private daTai=false;
 async tai(){if(this.daTai||this.dangTai())return;this.dangTai.set(true);this.loi.set(false);try{const ds=await firstValueFrom(this.http.get<Item[]>('/api/gv/lop'));this.ds.set(ds);this.id.set(ds[0]?.id??'');this.daTai=true;}catch{this.loi.set(true);}finally{this.dangTai.set(false);}}
}
