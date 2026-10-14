import { test as guestTest, expect as guestExpect } from '@playwright/test';
import { test, expect, item, noOverflow, flat } from './fixtures';

for (const width of [390, 768, 1023, 1024]) {
 test(`audit: lịch và điều hướng ở ${width} px`, async ({page,api})=>{
  await page.setViewportSize({width,height:900});
  api.overrides.set('GET /api/hs/nhac',{body:[item('r','Ôn xen kẽ — công thức các chương · ','','CHUA_XONG',{})]});
  await page.goto('/hs/lich');
  const before=page.getByRole('button',{name:'Tuần trước',exact:true});
  const after=page.getByRole('button',{name:'Tuần sau',exact:true});
  await expect(before).toBeVisible();await expect(after).toBeVisible();
  const a=await before.boundingBox(),b=await after.boundingBox();expect(Math.abs(a!.y-b!.y)).toBeLessThan(1);
  const push=page.getByRole('button',{name:'Bật thông báo trình duyệt'});
  await expect(push).toHaveCSS('border-top-width','1px');await expect(push).toHaveCSS('border-left-width','1px');
  expect((await push.boundingBox())!.height).toBeGreaterThanOrEqual(44);
  await expect(page.getByText('Ôn xen kẽ — công thức các chương',{exact:true})).toBeVisible();
  if(width<1024){await expect(page.getByRole('navigation',{name:'Điều hướng nhanh'})).toBeVisible();}
  else {await expect(page.getByRole('navigation',{name:'Điều hướng nhanh'})).toHaveCount(0);}
  await noOverflow(page);await flat(page);
 });
}

test('audit: animation chờ hữu hạn và giảm chuyển động',async({page,api})=>{
 // Giữ yêu cầu chưa trả để quan sát skeleton thật.
 let release!:()=>void;
 const pending=new Promise<void>(resolve=>{release=resolve;});
 await page.route('**/api/hs/bai',async route=>{await pending;await route.fulfill({json:[]});});
 await page.goto('/hs');
 const bar=page.locator('.khung-cho i').first();
 await expect(bar).toHaveCSS('animation-iteration-count','3');
 await expect(bar).toHaveCSS('animation-duration','1.4s');
 await page.emulateMedia({reducedMotion:'reduce'});await expect(bar).toHaveCSS('animation-name','none');
 release();await expect(page.getByRole('heading',{name:'Chưa có bài',exact:true})).toBeVisible();
});

guestTest('audit: khách không gọi refresh ở trang công khai và đăng nhập',async({page})=>{
 const requests:string[]=[];page.on('request',request=>{if(request.url().includes('/api/auth/refresh'))requests.push(request.url());});
 await page.goto('/');await guestExpect(page.getByRole('heading',{level:1})).toBeVisible();
 await page.goto('/dang-nhap');await guestExpect(page.getByRole('heading',{level:1})).toBeVisible();
 guestExpect(requests).toEqual([]);
});
