import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
@Injectable({providedIn:'root'})
export class NhacHoc {
 private readonly http=inject(HttpClient);
 readonly bat=signal(false);readonly ban=signal(false);readonly thongBao=signal('Nhắc trong ứng dụng luôn có trong lịch học.');
 async khoiPhuc():Promise<void>{
  if(!('serviceWorker' in navigator))return;
  try{const registration=await navigator.serviceWorker.getRegistration('/');this.bat.set(!!(await registration?.pushManager.getSubscription()));}catch{this.bat.set(false);}
 }
 async doi():Promise<void>{
  if(this.ban())return;this.ban.set(true);
  try {
   if(!('serviceWorker' in navigator))throw new Error();
   if(this.bat()){
    const registration=await navigator.serviceWorker.getRegistration('/');
    const subscription=await registration?.pushManager.getSubscription();
    if(subscription){
     await firstValueFrom(this.http.delete('/api/hs/nhac/push',{body:subscription.toJSON()}));
     await subscription.unsubscribe();
    }
    this.bat.set(false);this.thongBao.set('Đã tắt nhắc trên thiết bị này. Nhắc trong ứng dụng vẫn hoạt động.');return;
   }
   if(!('PushManager' in window)||!('Notification' in window))throw new Error();
   const config=await firstValueFrom(this.http.get<{available:boolean;publicKey:string}>('/api/hs/nhac/push'));
   if(!config.available)throw new Error();
   const registration=await navigator.serviceWorker.register('/nhac-hoc-sw.js');
   await navigator.serviceWorker.ready;
   let subscription=await registration.pushManager.getSubscription();
   if(await Notification.requestPermission()!=='granted')throw new Error();
   const bytes=Uint8Array.from(atob(config.publicKey.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));
   subscription??=await registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:bytes});
   try {await firstValueFrom(this.http.post('/api/hs/nhac/push',subscription.toJSON()));}
   catch(error){await subscription.unsubscribe();throw error;}
   this.bat.set(true);this.thongBao.set('Đã bật thông báo trên thiết bị này.');
  } catch {this.thongBao.set('Chưa bật được thông báo. Em vẫn xem nhắc học trong ứng dụng.');}
  finally {this.ban.set(false);}
 }
}
