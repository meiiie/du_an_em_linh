import {test,expect,detail,item,noOverflow,flat} from './fixtures';

test.beforeEach(async({page},info)=>{if(info.project.name==='1440')await page.setViewportSize({width:1280,height:1000});});

test('lời giải chỉ mở sau xác nhận, cổng kiểm tra trả lỗi có thể đọc',async({page,api})=>{
 api.overrides.set('GET /api/hs/bai/E2E-01',{body:{...detail,baiLam:{trangThai:'DA_NOP',cacBuoc:[]}}});
 api.overrides.set('POST /api/hs/bai/E2E-01/loi-giai',{status:403,body:{detail:'Lời giải bài kiểm tra chỉ mở sau hạn nộp.'}});
 await page.goto('/hs/luyen/E2E-01');
 await expect(page.getByTestId('xem-toan-bo-loi-giai')).toBeVisible();
 expect(api.calls.some(c=>c.path.endsWith('/loi-giai'))).toBe(false);
 await page.getByTestId('xem-toan-bo-loi-giai').click();
 const dialog=page.getByRole('dialog',{name:'Xem toàn bộ lời giải'});
 await expect(dialog).toBeVisible();await page.getByRole('button',{name:'Xác nhận xem lời giải'}).click();
 await expect(dialog).toContainText('Lời giải bài kiểm tra chỉ mở sau hạn nộp.');
 api.overrides.set('POST /api/hs/bai/E2E-01/loi-giai',{body:{loiGiai:'Lời giải giáo viên đã duyệt.',nguon:'GIAO_VIEN'}});
 await page.getByRole('button',{name:'Xác nhận xem lời giải'}).click();
 await expect(page.getByTestId('loi-giai-sau-nop')).toContainText('Lời giải giáo viên đã duyệt.');
 await expect(dialog).not.toBeVisible();await noOverflow(page);await flat(page);
});
test('AI soạn nháp từ tài liệu vào hàng duyệt, giáo viên đọc cả lời giải',async({page,api})=>{
 api.role='TEACHER';api.overrides.set('POST /api/gv/soan-nhap',{body:{message:'Đã đưa vào hàng duyệt.',count:1}});
 await page.goto('/gv/tai-lieu');await page.getByRole('button',{name:'AI soạn nháp'}).click();
 await page.getByLabel('Tài liệu giáo viên').selectOption('tl1');await page.getByRole('button',{name:'Soạn bản nháp',exact:true}).click();
 await expect(page.getByRole('dialog',{name:'AI soạn nháp'})).not.toBeVisible();
 expect(api.calls.find(c=>c.path==='/api/gv/soan-nhap')?.body).toEqual({lopId:'lop1',documentId:'tl1'});
 api.overrides.set('GET /api/gv/duyet',{body:[item('draft1','AI soạn nháp','Cần duyệt','KHONG_KIEM_DUOC',{kind:'CONTENT_DRAFT',statement:'Đề nháp đơn điệu.',solution:'Lời giải nháp đầy đủ.'})]});
 await page.goto('/gv/duyet');await expect(page.getByText('Lời giải nháp đầy đủ.')).toBeVisible();await noOverflow(page);await flat(page);
});
test('tự học chọn chủ đề, công cụ thẻ và CSV nằm trong phần ôn thêm',async({page,api})=>{
 await page.goto('/hs/bai');await page.getByText('Tự học theo chủ đề',{exact:true}).click();await page.getByRole('combobox',{name:'Chủ đề',exact:true}).selectOption('DH12');
 await page.getByRole('button',{name:'Chọn bài tự học',exact:true}).click();await expect(page).toHaveURL('/hs/luyen/E2E-01');
 expect(api.calls.some(c=>c.method==='POST'&&c.path==='/api/hs/tu-hoc/E2E-01')).toBe(true);
 await page.goto('/hs/kho');await expect(page.getByRole('button',{name:'Xuất CSV'})).not.toBeVisible();await page.getByText('Công cụ ôn thêm',{exact:true}).click();await expect(page.getByRole('button',{name:'Xuất CSV'})).toBeVisible();await noOverflow(page);
});
test('từ chối hoặc thiếu Push API vẫn có nhắc trong ứng dụng',async({page})=>{
 await page.goto('/hs/lich');await page.getByRole('button',{name:'Bật thông báo trình duyệt'}).click();await expect(page.getByRole('status')).toContainText('Em vẫn xem nhắc học trong ứng dụng.');await noOverflow(page);await flat(page);
});
