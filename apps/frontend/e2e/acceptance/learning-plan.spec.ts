import { test, expect, item, problem, detail, navigate, noOverflow, flat } from './fixtures';

const monday = '2026-10-12';
function weeklyPlan(advice: string, skill: string) {
  return { loiKhuyen: advice, sessions: [12, 14, 17].map((day, i) => item(`goi-y-${monday}-${i}`,
    ['Ôn công thức · ', 'Luyện đúng dạng · ', 'Xem lại bước đã sai · '][i] + skill, 'Buổi học gợi ý', 'GOI_Y',
    {startsAt:`2026-10-${day}T12:30:00Z`,endsAt:`2026-10-${day}T12:50:00Z`})) };
}

test('US6: chưa có lịch sử vẫn có lịch mặc định, lời khuyên bắt đầu và không tràn ngang', async ({ page, api }) => {
  await page.clock.setFixedTime(new Date('2026-10-14T08:00:00Z'));
  api.overrides.set('GET /api/hs/trang-hoc', {body:{ten:'An',soKyNang:[],hoanThanh:{kyNang:[],chuDe:[]}}});
  api.overrides.set('GET /api/hs/ke-hoach-tuan', {body:weeklyPlan('Bắt đầu bằng một bài Nhận biết về tập xác định và đạo hàm; học từng buổi ngắn.', 'Tập xác định và đạo hàm')});
  await page.goto('/hs/lich');
  await expect(page.getByTestId('lich-tuan').locator('section')).toHaveCount(7);
  await expect(page.getByTestId('lich-tuan').locator('article')).toHaveCount(3);
  await expect(page.getByTestId('loi-khuyen')).toContainText('Bắt đầu bằng một bài Nhận biết');
  await expect(page.getByTestId('lich-tuan')).toContainText('19:30–19:50');
  await noOverflow(page);
  await flat(page);
});

for (const flow of ['raise', 'repair', 'stuck'] as const) {
  test(`US5/6 sau chấm bài: ${flow}, gợi bài có lý do → Học → Lịch`, async ({ page, api }) => {
    await page.clock.setFixedTime(new Date('2026-10-14T08:00:00Z'));
    const stuck = flow === 'stuck';
    const passed = flow === 'raise';
    const nextCode = 'E2E-NEXT';
    const level = flow === 'repair' ? 'NHAN_BIET' : 'THONG_HIEU';
    const nextLevel = passed ? 'THONG_HIEU' : 'NHAN_BIET';
    const reason = passed ? 'Em đã làm đạt. Nâng đúng một nấc từ Nhận biết lên Thông hiểu ở kỹ năng Đơn điệu.'
      : stuck ? 'Em đang kẹt ở Đơn điệu. Luyện bài dễ hơn ở mức Nhận biết; chưa tăng độ khó.'
      : 'Chữa dạng lỗi vừa gặp: Nhầm dấu đạo hàm. Ôn Đơn điệu ở mức Nhận biết.';
    const advice = stuck ? 'Ôn lại Đơn điệu: Nhầm dấu đạo hàm; em đang kẹt, chưa tăng độ khó.'
      : 'Ưu tiên Đơn điệu, kỹ năng cần củng cố nhất. Xem lại bước đã sai trước khi làm bài mới.';
    api.overrides.set('GET /api/hs/bai/E2E-01', {body:{...detail,buocBatDau:'B.DH.KETLUAN',
      baiLam:{trangThai:'DANG_LAM',cacBuoc:detail.cacBuoc.map(s=>({maBuoc:s.maBuoc,dong:[],bang:[],ketQua:'DAT',thongBao:'Bước đạt.',oSai:[]}))}}});
    api.overrides.set('POST /api/hs/bai/E2E-01/buoc', {body:{ketQua:passed?'DAT':'SAI',thongBao:passed?'Bước đạt.':'Chưa đúng dấu đạo hàm.',oSai:[]}});
    await page.route('**/api/hs/bai/E2E-01/nop', async route => {
      api.overrides.set('GET /api/hs/bai', {body:[{...problem,trangThai:'DA_NOP',ketQua:passed?'DAT':'SAI'},
        {...problem,maBai:nextCode,muc4:nextLevel,han:null}]});
      api.overrides.set('GET /api/hs/che-do', {body:[item(nextCode,'','','ON_TAP',{dueAt:null})]});
      api.overrides.set('GET /api/hs/trang-hoc', {body:{ten:'An',soKyNang:[{kyNang:problem.kyNang,tenKyNang:'Đơn điệu',muc4:level,ket:stuck}],hoanThanh:{kyNang:[],chuDe:[]}}});
      api.overrides.set('GET /api/hs/goi-bai', {body:[item(nextCode,'Bài kế',reason,passed?'NANG_1_NAC':stuck?'DE_HON':'CHUA_LOI',{level4:nextLevel,skillCode:problem.kyNang})]});
      api.overrides.set('GET /api/hs/ke-hoach-tuan', {body:weeklyPlan(advice,'Đơn điệu')});
      api.overrides.set('GET /api/hs/nhac', {body:[weeklyPlan(advice,'Đơn điệu').sessions[1]]});
      await route.fulfill({json:{ketQua:passed?'DAT':'SAI',mucHieu:[{kyNang:problem.kyNang,muc4Truoc:'NHAN_BIET',muc4Sau:level}]}});
    });
    await page.goto('/hs/luyen/E2E-01');
    await page.getByTestId('latex-db').fill('(0;1)');
    await page.getByTestId('latex-nb').fill('(1;2)');
    await page.getByTestId('nop-buoc').click();
    await expect(page.getByTestId('cham-thong-bao')).toContainText(passed?'Bước đạt.':'Chưa đúng dấu đạo hàm.');
    await page.getByTestId('nop-bai').click();
    await expect(page.getByTestId('da-nop')).toContainText(reason);
    await expect(page.getByTestId('bai-ke-ket-qua')).toHaveAttribute('href','/hs/luyen/'+nextCode);
    await page.goto('/hs');
    await expect(page.getByTestId('de-xuat-bai')).toContainText(reason);
    await expect(page.getByTestId('de-xuat-bai').getByRole('link')).toHaveAttribute('href','/hs/luyen/'+nextCode);
    await expect(page.locator('.nhac-hoc')).toContainText('Luyện đúng dạng · Đơn điệu');
    await noOverflow(page);
    await navigate(page,'nav-hs-lich');
    await expect(page.getByTestId('loi-khuyen')).toContainText(advice);
    await expect(page.getByTestId('lich-tuan').locator('article')).toHaveCount(3);
    await noOverflow(page);
    await flat(page);
    if (stuck) {
      api.role='TEACHER';
      api.overrides.set('GET /api/gv/canh-bao',{body:[item('ket1','An','Đang kẹt ở Đơn điệu. Kẹt 3 lượt ở bước xét dấu.','KET',{studentId:'synthetic-user'})]});
      await page.goto('/gv');
      await expect(page.getByTestId('canh-bao-ket')).toContainText('An');
      await expect(page.getByTestId('canh-bao-ket')).toContainText('Đơn điệu');
      await expect(page.getByTestId('canh-bao-ket').getByRole('link')).toHaveAttribute('href','/gv/hoc-sinh/synthetic-user');
      await noOverflow(page);
    }
  });
}

test('US5: kẹt ở Nhận biết, thiếu bài dễ hơn → gửi thầy cô, không tạo liên kết bài rỗng', async ({page,api}) => {
  api.overrides.set('GET /api/hs/goi-bai',{body:[item('','Gửi thầy cô','Em đang kẹt ở Đơn điệu. Hãy ôn lại dạng này và gửi thầy cô; chưa tăng độ khó.','GUI_THAY_CO',{level4:'NHAN_BIET',skillCode:problem.kyNang})]});
  api.overrides.set('GET /api/hs/bai',{body:[]});
  await page.goto('/hs');
  await expect(page.getByTestId('de-xuat-bai')).toContainText('gửi thầy cô');
  await expect(page.getByTestId('de-xuat-bai').getByRole('link')).toHaveCount(0);
  await noOverflow(page);
  await flat(page);
});
