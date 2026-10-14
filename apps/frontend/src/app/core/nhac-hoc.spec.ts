import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController,provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {NhacHoc} from './nhac-hoc';
import {vi} from 'vitest';
describe('Nhắc học chủ động',()=>{
 afterEach(()=>{vi.unstubAllGlobals();vi.restoreAllMocks();});
 function setup(){TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});return {service:TestBed.inject(NhacHoc),http:TestBed.inject(HttpTestingController)};}
 function browser(permission='granted'){
  const subscription={toJSON:()=>({endpoint:'https://fcm.googleapis.com/send/synthetic',keys:{p256dh:'public',auth:'auth'}}),unsubscribe:vi.fn().mockResolvedValue(true)};
  const manager={getSubscription:vi.fn().mockResolvedValue(subscription),subscribe:vi.fn().mockResolvedValue(subscription)};
  const registration={pushManager:manager};const requestPermission=vi.fn().mockResolvedValue(permission);
  vi.stubGlobal('navigator',{serviceWorker:{register:vi.fn().mockResolvedValue(registration),getRegistration:vi.fn().mockResolvedValue(registration),ready:Promise.resolve(registration)}});
  vi.stubGlobal('PushManager',class{});vi.stubGlobal('Notification',{requestPermission});
  return {subscription,requestPermission};
 }
 it('khôi phục không xin quyền hoặc gửi đăng ký',async()=>{const b=browser();const {service,http}=setup();await service.khoiPhuc();expect(service.bat()).toBe(true);expect(b.requestPermission).not.toHaveBeenCalled();http.verify();});
 it('thiếu khóa dùng nhắc trong ứng dụng, không xin quyền',async()=>{const b=browser();const {service,http}=setup();const pending=service.doi();http.expectOne('/api/hs/nhac/push').flush({available:false,publicKey:''});await pending;expect(b.requestPermission).not.toHaveBeenCalled();expect(service.bat()).toBe(false);expect(service.thongBao()).toContain('trong ứng dụng');http.verify();});
 it('từ chối quyền không lưu subscription',async()=>{const b=browser('denied');const {service,http}=setup();const pending=service.doi();http.expectOne('/api/hs/nhac/push').flush({available:true,publicKey:'BA'});await pending;expect(b.requestPermission).toHaveBeenCalledOnce();expect(service.bat()).toBe(false);http.verify();});
 it('đồng ý mới lưu subscription vào server',async()=>{browser();const {service,http}=setup();const pending=service.doi();http.expectOne('/api/hs/nhac/push').flush({available:true,publicKey:'BA'});await new Promise(r=>setTimeout(r,0));const request=http.expectOne({method:'POST',url:'/api/hs/nhac/push'});expect(request.request.body.endpoint).toContain('fcm.googleapis.com');request.flush({message:'Đã bật'});await pending;expect(service.bat()).toBe(true);http.verify();});
 it('hủy trên thiết bị không phụ thuộc khóa VAPID còn tồn tại',async()=>{const b=browser();const {service,http}=setup();await service.khoiPhuc();const pending=service.doi();await new Promise(r=>setTimeout(r,0));http.expectOne({method:'DELETE',url:'/api/hs/nhac/push'}).flush({message:'Đã tắt'});await pending;expect(b.subscription.unsubscribe).toHaveBeenCalledOnce();expect(service.bat()).toBe(false);expect(b.requestPermission).not.toHaveBeenCalled();http.verify();});
});
