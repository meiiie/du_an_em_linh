import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { test, expect, navigate, noOverflow, flat, csv, detail, formula, bank, problem, item } from './fixtures';
import type { Page } from '@playwright/test';
const hs = [['nav-hs-bai', '/hs/bai', 'Đề bài'], ['nav-hs-lich', '/hs/lich', 'Lịch học'], ['nav-hs-kho', '/hs/kho', 'Công thức và tài liệu'], ['nav-hs-lo-trinh', '/hs', 'Chào An']];
const gv = [['nav-gv-giao', '/gv/giao', 'Giao bài'], ['nav-gv-duyet', '/gv/duyet', 'Duyệt'], ['nav-gv-ngan-hang', '/gv/ngan-hang', 'Đề bài'], ['nav-gv-tai-lieu', '/gv/tai-lieu', 'Tài liệu'], ['nav-gv-cong-thuc', '/gv/cong-thuc', 'Công thức'], ['nav-gv-tien-do', '/gv/tien-do', 'Mức lớp'], ['nav-gv-ket-noi-ai', '/gv/ket-noi-ai', 'Gia sư'], ['nav-gv-cai-dat', '/gv/cai-dat', 'Cài đặt lớp'], ['nav-gv-tong-quan', '/gv', 'Lớp 12A1 thử']];
for (const [role, home, routes] of [['STUDENT', '/hs', hs], ['TEACHER', '/gv', gv], ['ADMIN', '/qt', [['nav-qt-lop', '/qt/lop', 'Lớp học'], ['nav-qt-nhat-ky', '/qt/nhat-ky', 'Nhật ký'], ['nav-qt-nguoi-dung', '/qt', 'Người dùng']]]] as const) {
    test(`U0/U1/U3/U14 ${role}: role nav, bottom nav và flat`, async ({ page, api }) => {
        api.role = role;
        await page.goto(home);
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        for (const [id, path, heading] of routes) {
            await navigate(page, id);
            await expect(page).toHaveURL(path);
            await expect(page.getByRole('heading', { level: 1, name: heading, exact: true })).toBeVisible();
            await expect(page.locator('app-khung-cho')).toHaveCount(0);
            await noOverflow(page);
            await flat(page);
        }
        if (role !== 'ADMIN') {
            await page.goto('/qt');
            await expect(page).toHaveURL(home);
            await page.goto(role === 'STUDENT' ? '/gv' : '/hs');
            await expect(page).toHaveURL(home);
        }
        const quick = page.getByRole('navigation', { name: 'Điều hướng nhanh' });
        if (page.viewportSize()!.width === 390) {
            await expect(quick).toBeVisible();
            const box = await quick.boundingBox();
            expect(box!.y + box!.height).toBeCloseTo(844, 0);
            await quick.getByRole('link').first().click();
            await expect(quick.getByRole('link').first()).toHaveAttribute('aria-current', 'page');
        }
        else
            await expect(quick).toHaveCount(0);
    });
}
test('U4 trang Học gọi mastery và hiện bốn mức', async ({ page, api }) => {
    await page.goto('/hs');
    await expect(page.getByRole('heading', { name: 'Chào An' })).toBeVisible();
    await expect(page.getByTestId('so-ky-nang')).toContainText('Đơn điệu');
    await expect(page.getByTestId('so-ky-nang').locator('.bac i')).toHaveCount(4);
    await expect(page.getByTestId('so-ky-nang')).toContainText('Nhận biết');
    await expect(page.getByTestId('lam-buoc-tiep')).toHaveAttribute('href', '/hs/luyen/E2E-01');
    expect(api.calls.some(c => c.path === '/api/hs/trang-hoc')).toBe(true);
});
test('U5/U6 Kho: KaTeX, keyboard flashcard, focus trap, CSV UTF-8', async ({ page, api }) => {
    await page.goto('/hs/kho');
    await expect(page.locator('#ct-ct1 .katex')).toBeVisible();
    await page.getByText('Công cụ ôn thêm', {exact:true}).click();
    await page.getByRole('button', { name: 'Ôn bằng thẻ' }).click();
    const dialog = page.getByRole('dialog', { name: 'Ôn công thức' });
    await expect(dialog).toBeVisible();
    await dialog.getByRole('button', { name: 'Mặt trước. Lật xem công thức' }).focus();
    await page.keyboard.press('Enter');
    await expect(dialog.getByRole('button', { name: 'Mặt sau. Lật về câu hỏi' })).toHaveAttribute('aria-pressed', 'true');
    await expect(dialog).toContainText(formula.details.statement);
    for (let i = 0; i < 8; i++) {
        await page.keyboard.press('Tab');
        expect(await dialog.evaluate(el => el.contains(document.activeElement))).toBe(true);
    }
    await flat(page);
    await page.keyboard.press('Escape');
    await expect(dialog).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Ôn bằng thẻ' })).toBeFocused();
    const pending = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Xuất CSV' }).click();
    const download = await pending;
    expect(download.suggestedFilename()).toBe('cong-thuc.csv');
    expect(await readFile((await download.path())!, 'utf8')).toBe('\uFEFF' + csv);
    expect(api.calls.some(c => c.path === '/api/hs/flashcards/export')).toBe(true);
    await page.getByRole('button', { name: 'Tài liệu', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Tài liệu đơn điệu' })).toBeVisible();
});
async function tutorFor(page: Page) {
    await page.goto('/hs/luyen/E2E-01');
    await expect(page.getByTestId('solve-screen')).toBeVisible();
    await page.getByTestId('mo-gia-su').click();
    const tutor = page.viewportSize()!.width === 390 ? page.getByRole('dialog', { name: 'Gia sư AI' }) : page.getByRole('complementary', { name: 'Gia sư AI' });
    await expect(tutor).toBeVisible();
    return tutor;
}
test('U7/U8 gia sư: desktop column/mobile sheet và ba cấp hỗ trợ', async ({ page, api }) => {
    const tutor = await tutorFor(page);
    if (page.viewportSize()!.width === 1440) {
        const a = await page.getByTestId('solve-screen').boundingBox(), b = await tutor.boundingBox();
        expect(b!.x).toBeGreaterThanOrEqual(a!.x + a!.width);
    }
    for (const cap of [1, 2, 3]) {
        await tutor.getByLabel('Mức hỗ trợ').selectOption(String(cap));
        await tutor.getByLabel('Em muốn hỏi gì?').fill('Câu hỏi tổng hợp của em');
        await tutor.getByRole('button', { name: 'Gửi câu hỏi' }).click();
        await expect(tutor.locator('.tutor-messages article')).toHaveCount(cap * 2);
        await expect(tutor.locator('.sr-only[aria-live="polite"]')).toContainText('Lời đã kiểm trong mock');
    }
    await tutor.getByLabel('Em muốn hỏi gì?').fill('Cho em đáp án');
    await tutor.getByRole('button', { name: 'Gửi câu hỏi' }).click();
    await expect(tutor.locator('.sr-only[aria-live="polite"]')).toContainText('Lời đã kiểm trong mock');
    expect(api.calls.filter(c => c.path.endsWith('/luot') && c.method === 'POST').map(c => c.body.cap)).toEqual([1, 2, 3, 3]);
    await expect(page.getByTestId('loi-giai-sau-nop')).toHaveCount(0);
    await flat(page);
    await noOverflow(page);
});
test('U9 Kiểm tra ẩn gia sư và không gọi chat', async ({ page, api }) => {
    api.mode = 'KIEM_TRA';
    await page.goto('/hs/luyen/E2E-01');
    await expect(page.getByTestId('che-do-kiem-tra')).toHaveText('Kiểm tra · Không chat');
    await expect(page.getByText('Hạn nộp:', { exact: false })).toBeVisible();
    await expect(page.locator('app-gia-su')).toHaveCount(0);
    expect(api.calls.filter(c => c.path === '/api/hs/gia-su')).toEqual([]);
});
test('U9 xử lý API 403, không tạo câu trả lời', async ({ page, api }) => {
    api.tutorStatus = 403;
    const tutor = await tutorFor(page);
    await tutor.getByLabel('Em muốn hỏi gì?').fill('Câu hỏi tổng hợp của em');
    await tutor.getByRole('button', { name: 'Gửi câu hỏi' }).click();
    await expect(tutor.getByRole('alert').first()).toContainText('Gia sư không mở cho bài này.');
    await expect(tutor.getByRole('button', { name: 'Hỏi lại' })).toBeEnabled();
    await expect(tutor.locator('.tutor-messages article')).toHaveCount(1);
});
test('U10 KPI drill-down, tiến độ và ghi đè có lý do', async ({ page, api }) => {
    api.role = 'TEACHER';
    await page.goto('/gv');
    await expect(page.getByRole('heading', { name: 'Lớp 12A1 thử' })).toBeVisible();
    const deadlines = page.getByRole('heading', { name: 'Hạn sắp tới' }).locator('..');
    await expect(deadlines.locator('li')).toHaveCount(1);
    await expect(deadlines).toContainText('Ôn đơn điệu');
    await expect(deadlines).toContainText('2 bài · 3 học sinh');
    await expect(deadlines).toContainText('1/6 đã nộp');
    await expect(page.getByRole('heading', { name: 'Phân bố mức hiểu' })).toBeVisible();
    await page.getByRole('link', { name: /Học sinh đang kẹt/ }).click();
    await expect(page).toHaveURL('/gv/tien-do');
    await expect(page.getByText(/3 mức khi xem/)).toBeVisible();
    await page.getByRole('link', { name: 'An', exact: true }).click();
    await expect(page).toHaveURL('/gv/hoc-sinh/hs1');
    await page.getByRole('button', { name: 'Ghi đè mức', exact: true }).click();
    const dialog = page.getByRole('dialog', { name: 'Ghi đè mức hiểu' });
    await expect(dialog.getByRole('button', { name: 'Lưu mức mới' })).toBeDisabled();
    await dialog.getByLabel('Mức mới').selectOption('THONG_HIEU');
    await dialog.getByLabel('Lý do').fill('Đã kiểm bài tổng hợp.');
    await dialog.getByRole('button', { name: 'Lưu mức mới' }).click();
    await expect(dialog).not.toBeVisible();
    expect(api.calls.find(c => c.path === '/api/gv/muc-hieu')?.body).toMatchObject({ studentId: 'hs1', level4: 'THONG_HIEU', reason: 'Đã kiểm bài tổng hợp.' });
});
test('U11 ngân hàng lọc mức/cổng; tài liệu và công thức', async ({ page, api }) => {
    api.role = 'TEACHER';
    await page.goto('/gv/ngan-hang');
    await expect(page.getByRole('table')).toContainText('Bài cần sửa');
    await page.getByLabel('Mức', { exact: true }).selectOption('THONG_HIEU');
    await page.getByLabel('Kết quả kiểm').selectOption('KHONG_KIEM_DUOC');
    await expect(page.getByRole('table').locator('tbody tr')).toHaveCount(1);
    await expect(page.getByRole('table')).toContainText('Xét cực trị');
    await expect(page.getByRole('table')).toContainText('E2E-02');
    await page.getByLabel('Kết quả kiểm').selectOption('DAT');
    await expect(page.getByRole('heading', { name: 'Không có bài phù hợp' })).toBeVisible();
    await navigate(page, 'nav-gv-tai-lieu');
    await expect(page.getByRole('heading', { name: 'Tài liệu đơn điệu' })).toBeVisible();
    await navigate(page, 'nav-gv-cong-thuc');
    await expect(page.locator('main .katex')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Kiểm và khóa bảng' })).toBeEnabled();
});
test('U11 phân trang ngân hàng và tìm kiếm đặt lại trang', async ({ page, api }) => {
    api.role = 'TEACHER';
    api.bankItems = Array.from({ length: 21 }, (_, i) => ({
        ...bank[0], id: `PAGE-${i}`, title: `PAGE-${i}`, subtitle: `Đề trang ${i}`,
        details: { ...bank[0].details, code: `PAGE-${i}` },
    }));
    await page.goto('/gv/ngan-hang');
    const rows = page.getByRole('table').locator('tbody tr');
    const size = page.viewportSize()!.width === 390 ? 10 : 20;
    await expect(rows).toHaveCount(size);
    const pagination = page.getByRole('navigation', { name: 'Phân trang danh sách' });
    await pagination.getByRole('button', { name: 'Trang sau' }).click();
    await expect(rows.first()).toContainText(`PAGE-${size}`);
    await expect(rows).toHaveCount(21 - size > size ? size : 21 - size);
    await page.getByLabel('Tìm bài', { exact: true }).fill('PAGE-20');
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText('Đề trang 20');
    await expect(pagination).toHaveCount(0);
    expect(api.calls.filter(c => c.path === '/api/gv/ngan-hang').map(c => c.query)).toEqual([
        expect.objectContaining({ page: '1', size: String(size), q: '' }),
        expect.objectContaining({ page: '2', size: String(size), q: '' }),
        expect.objectContaining({ page: '1', size: String(size), q: 'PAGE-20' }),
    ]);
});

test('U4 đề bài nhóm theo tên bộ và đếm bài đã nộp', async ({ page, api }) => {
    await page.goto('/hs/bai');
    await expect(page.locator('details.nhom-chu-de')).toHaveCount(1);
    await expect(page.locator('details.nhom-chu-de summary')).toContainText('Ôn đơn điệu');
    await expect(page.locator('details.nhom-chu-de summary')).toContainText('2 bài · 0/2 đã nộp · Hạn:');
    await expect(page.locator('details.nhom-chu-de li')).toHaveCount(2);
});
for (const approve of [true, false])
    test(`U12 ${approve ? 'duyệt' : 'chặn'} cần lý do`, async ({ page, api }) => {
        api.role = 'TEACHER';
        await page.goto('/gv/duyet');
        await page.getByRole('button', { name: approve ? 'Duyệt' : 'Chặn', exact: true }).click();
        const dialog = page.getByRole('dialog');
        const submit = dialog.getByRole('button', { name: approve ? 'Xác nhận duyệt' : 'Xác nhận chặn' });
        await expect(submit).toBeDisabled();
        await dialog.getByLabel('Lý do').fill('Đã xem điều kiện.');
        await submit.click();
        await expect(dialog).not.toBeVisible();
        expect(api.calls.find(c => c.path === '/api/gv/duyet/duyet1')?.body).toMatchObject({ approve, reason: 'Đã xem điều kiện.' });
    });
test('U13 lịch tuần, thêm buổi học bằng bàn phím', async ({ page, api }) => {
    await page.goto('/hs/lich');
    await expect(page.getByRole('heading', { name: 'Thứ hai' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Chủ nhật' })).toBeVisible();
    await page.getByRole('button', { name: 'Tuần sau', exact: true }).focus();
    await page.keyboard.press('Enter');
    await page.getByRole('button', { name: 'Thêm buổi học' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('Việc học').fill('Ôn xét dấu');
    await dialog.getByLabel('Bắt đầu').fill('2026-10-14T17:00');
    await dialog.getByLabel('Kết thúc').fill('2026-10-14T18:00');
    await dialog.getByRole('button', { name: 'Lưu buổi học' }).focus();
    await page.keyboard.press('Enter');
    await expect(dialog).not.toBeVisible();
    expect(api.calls.find(c => c.method === 'POST' && c.path === '/api/hs/lich')?.body.title).toBe('Ôn xét dấu');
});
test('U14 quản trị bulk khóa và phân công lớp', async ({ page, api }) => {
    api.role = 'ADMIN';
    await page.goto('/qt');
    await page.getByLabel('Tìm người dùng').fill('An');
    await expect(page.getByRole('table').locator('tbody tr')).toHaveCount(1);
    await page.getByRole('checkbox', { name: 'Chọn An' }).check();
    await page.getByRole('button', { name: 'Khóa tài khoản' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Xác nhận', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Bỏ chọn' })).toHaveCount(0);
    expect(api.calls.find(c => c.path.endsWith('/bulk'))?.body).toEqual({ ids: ['hs1'], enabled: false });
    await navigate(page, 'nav-qt-lop');
    await page.getByRole('button', { name: 'Phân công', exact: true }).click();
    const dialog = page.getByRole('dialog', { name: 'Phân công vào lớp' });
    await dialog.getByLabel('Người dùng', { exact: true }).selectOption('gv1');
    await dialog.getByRole('button', { name: 'Lưu phân công' }).click();
    await expect(dialog).not.toBeVisible();
    expect(api.calls.find(c => c.path.endsWith('/phan-cong'))?.body).toMatchObject({ userId: 'gv1', role: 'TEACHER' });
});
for (const scenario of ['empty', 'error', 'unavailable'] as const)
    test(`U2/U4 ${scenario} và thử lại`, async ({ page, api }) => {
        api.overrides.set('GET /api/hs/bai', { status: scenario === 'empty' ? 200 : scenario === 'error' ? 500 : 501, body: scenario === 'empty' ? [] : {} });
        await page.goto('/hs');
        await expect(page.getByRole('heading', { name: scenario === 'empty' ? 'Chưa có bài' : scenario === 'error' ? 'Chưa tải được' : 'Tính năng chưa sẵn sàng', exact: true })).toBeVisible();
        if (scenario !== 'empty') {
            api.overrides.delete('GET /api/hs/bai');
            await page.getByRole('button', { name: 'Thử lại' }).click();
            await expect(page.getByTestId('lam-buoc-tiep')).toBeVisible();
        }
    });
test('U2 skeleton khi chờ rồi trạng thái rỗng', async ({ page, api }) => {
    let release!: () => void;
    const barrier = new Promise<void>(resolve => {
        release = resolve;
    });
    await page.route('**/api/hs/kho', async (route) => {
        await barrier;
        await route.fulfill({ json: { formulas: [], documents: [] } });
    });
    await page.goto('/hs/kho');
    try {
        await expect(page.getByRole('status').filter({ hasText: 'Đang tải' })).toBeVisible();
    }
    finally {
        release();
    }
    await expect(page.getByRole('heading', { name: 'Chưa có công thức' })).toBeVisible();
});
test.describe('giảm chuyển động', () => {
    test.use({ reducedMotion: 'reduce' });
    test('U1/U2 prefers-reduced-motion tắt animation', async ({ page, api }) => {
        await page.goto('/');
        await expect(page.locator('h1')).toBeVisible();
        expect(await page.evaluate(() => matchMedia('(prefers-reduced-motion: reduce)').matches)).toBe(true);
        const moving = await page.evaluate(() => [...document.querySelectorAll('*')].filter(el => el.getClientRects().length).flatMap(el => ['', '::before', '::after'].map(p => getComputedStyle(el, p || null)).filter(s => [s.animationDuration, s.transitionDuration].some(v => v.split(',').some(d => parseFloat(d) > 0.001))).map(s => `${el.tagName}: ${s.animationDuration}/${s.transitionDuration}`)));
        expect(moving).toEqual([]);
        await flat(page);
    });
});
const loadDependency = createRequire(__filename);
let AxeBuilder: any;
try {
    AxeBuilder = loadDependency('@axe-core/playwright').default;
}
catch (error) {
    if ((error as NodeJS.ErrnoException).code !== 'MODULE_NOT_FOUND')
        throw error;
}
for (const [role, path] of [['STUDENT', '/hs'], ['STUDENT', '/hs/kho'], ['TEACHER', '/gv'], ['ADMIN', '/qt']] as const)
    test(`U2 axe ${path}: no serious/critical`, async ({ page, api }) => {
        test.skip(!AxeBuilder, '@axe-core/playwright is not in existing dependencies; no package changes in this lane');
        api.role = role;
        await page.goto(path);
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await expect(page.locator('app-khung-cho')).toHaveCount(0);
        const results = await new AxeBuilder({ page }).analyze();
        expect(results.violations.filter((v: {
            impact: string;
        }) => ['serious', 'critical'].includes(v.impact))).toEqual([]);
    });
for (const theme of ['light', 'dark'] as const)
    test(`U15 ${theme}: giao → gia sư → mức tăng → bài kế → lịch`, async ({ page, api }, info) => {
        await page.emulateMedia({ colorScheme: theme });
        api.role = 'TEACHER';
        await page.goto('/gv/giao');
        await page.getByLabel('Tên bộ bài').fill('Ôn đơn điệu');
        await page.getByRole('checkbox', { name: /Bài nhận biết/ }).check();
        await page.getByRole('button', { name: 'Giao bài', exact: true }).click();
        await expect(page.locator('.toast').filter({ hasText: 'Đã giao bộ bài.' })).toBeVisible();
        expect(api.calls.find(c => c.method === 'POST' && c.path === '/api/gv/giao')?.body).toMatchObject({ problemIds: ['E2E-01'], mode: 'ON_TAP' });
        api.role = 'STUDENT';
        const tutor = await tutorFor(page);
        await tutor.getByLabel('Em muốn hỏi gì?').fill('Câu hỏi tổng hợp của em');
        await tutor.getByRole('button', { name: 'Gửi câu hỏi' }).click();
        await expect(tutor.locator('.tutor-messages article')).toHaveCount(2);
        await page.screenshot({ path: info.outputPath(`luyen-${theme}.png`), fullPage: true });
        if (page.viewportSize()!.width === 390)
            await page.keyboard.press('Escape');
        else
            await tutor.getByRole('button', { name: 'Đóng gia sư' }).click();
        // Persisted verified attempt supplied by mock; this does not test CAS or server authorization.
        api.overrides.set('GET /api/hs/bai/E2E-01', { body: { ...detail, buocBatDau: 'B.DH.KETLUAN', baiLam: { trangThai: 'DANG_LAM', cacBuoc: detail.cacBuoc.map(s => ({ maBuoc: s.maBuoc, dong: [], bang: [], ketQua: 'DAT', thongBao: 'Bước đạt.', oSai: [] })) } } });
        await page.reload();
        await page.getByTestId('latex-db').fill('(0;1)');
        await page.getByTestId('latex-nb').fill('(1;2)');
        await page.getByTestId('nop-buoc').click();
        await expect(page.getByTestId('cham-thong-bao')).toContainText('Bước đạt.');
        await page.getByTestId('nop-bai').click();
        await expect(page.getByTestId('da-nop')).toContainText('Nhận biết → Thông hiểu');
        await expect(page.getByTestId('bai-ke-ket-qua')).toHaveAttribute('href', '/hs/luyen/E2E-NEXT');
        await flat(page);
        await page.screenshot({ path: info.outputPath(`ket-qua-${theme}.png`), fullPage: true });
        await page.goto('/hs');
        await expect(page.getByTestId('so-ky-nang')).toContainText('Thông hiểu');
        await navigate(page, 'nav-hs-lich');
        await expect(page.getByRole('heading', { name: 'Lịch học' })).toBeVisible();
        await flat(page);
        await noOverflow(page);
        await page.screenshot({ path: info.outputPath(`lich-${theme}.png`), fullPage: true });
    });

test('U14 quản trị trường có quyền vào cả giáo viên và quản trị', async ({ page, api }) => {
    api.role = 'SCHOOL_ADMIN';
    await page.goto('/qt');
    await expect(page.getByRole('heading', { level: 1, name: 'Người dùng' })).toBeVisible();
    await page.goto('/gv');
    await expect(page.getByRole('heading', { level: 1, name: 'Lớp 12A1 thử' })).toBeVisible();
});

test('U14 tạo lớp gửi tên, khối và năm học', async ({ page, api }) => {
    api.role = 'ADMIN';
    api.overrides.set('POST /api/qt/lop', { body: { message: 'Đã tạo lớp.', count: 1 } });
    await page.goto('/qt/lop');
    await page.getByRole('button', { name: 'Tạo lớp', exact: true }).click();
    const dialog = page.getByRole('dialog', { name: 'Tạo lớp' });
    await expect(dialog.getByRole('button', { name: 'Tạo lớp', exact: true })).toBeDisabled();
    await dialog.getByLabel('Tên lớp').fill('12A2 thử');
    await dialog.getByLabel('Khối', { exact: true }).selectOption('12');
    await dialog.getByLabel('Năm học').fill('2026-2027');
    await dialog.getByRole('button', { name: 'Tạo lớp', exact: true }).click();
    await expect(dialog).not.toBeVisible();
    expect(api.calls.find(c => c.method === 'POST' && c.path === '/api/qt/lop')?.body).toMatchObject({ name: '12A2 thử', grade: 12, schoolYear: '2026-2027' });
});

test('U5 Kho lỗi có Thử lại; dữ liệu rỗng có hướng dẫn', async ({ page, api }) => {
    api.overrides.set('GET /api/hs/kho', { status: 500, body: {} });
    await page.goto('/hs/kho');
    await expect(page.getByRole('alert')).toContainText('Chưa tải được');
    api.overrides.set('GET /api/hs/kho', { body: { formulas: [], documents: [] } });
    await page.getByRole('button', { name: 'Thử lại' }).click();
    await expect(page.getByRole('heading', { name: 'Chưa có công thức' })).toBeVisible();
    await page.getByRole('button', { name: 'Tài liệu', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Chưa có tài liệu' })).toBeVisible();
});

test('U9 giáo viên giao chế độ Kiểm tra và hạn nộp', async ({ page, api }) => {
    api.role = 'TEACHER';
    await page.goto('/gv/giao');
    await page.getByLabel('Tên bộ bài').fill('Kiểm tra đơn điệu');
    await page.getByLabel('Chế độ', { exact: true }).selectOption('KIEM_TRA');
    await page.getByLabel('Hạn nộp', { exact: true }).fill('20/10/2026 19:00');
    await page.getByRole('checkbox', { name: /Bài nhận biết/ }).check();
    await page.getByRole('button', { name: 'Giao bài', exact: true }).click();
    await expect(page.locator('.toast')).toContainText('Đã giao bộ bài.');
    const body = api.calls.find(c => c.method === 'POST' && c.path === '/api/gv/giao')?.body;
    expect(body).toMatchObject({ mode: 'KIEM_TRA', problemIds: ['E2E-01'], studentIds: [] });
    expect(body.dueAt).toBe('2026-10-20T12:00:00.000Z');
});

test('Học ghi rõ bài kiểm tra tiếp theo và Luyện hiện banner cùng hạn', async ({ page, api }) => {
    api.mode = 'KIEM_TRA';
    api.overrides.set('GET /api/hs/bai', { body: [
        {...problem, tenBo: 'Kiểm tra hàm số — 45 phút'},
        {...problem, maBai: 'E2E-NEXT', tenBo: 'Kiểm tra hàm số — 45 phút'},
    ] });
    api.overrides.set('GET /api/hs/che-do', { body: ['E2E-01', 'E2E-NEXT'].map(id => item(id, id, '', 'KIEM_TRA', {dueAt: problem.han})) });
    await page.goto('/hs');
    await expect(page.locator('.ke-tiep')).toContainText('Bài kiểm tra · 45 phút');
    await expect(page.locator('.ke-tiep')).toContainText('2 câu');
    await expect(page.locator('.ke-tiep')).toContainText('20/10');
    await expect(page.locator('.ke-tiep')).toContainText('Gia sư tạm tắt');
    await expect(page.getByTestId('bai-E2E-NEXT')).toContainText('Kiểm tra');
    await page.getByRole('link', {name: 'Bắt đầu kiểm tra', exact: true}).click();
    await expect(page.getByTestId('bang-kiem-tra')).toContainText('Đang kiểm tra — gia sư tạm tắt');
    await expect(page.getByTestId('bang-kiem-tra')).toContainText('20/10');
    await expect(page.getByTestId('mo-gia-su')).toBeDisabled();
    await flat(page);
    await noOverflow(page);
});

test('Học ưu tiên bài ôn dù có bài kiểm tra đang làm và được gợi trước', async ({ page, api }) => {
    api.overrides.set('GET /api/hs/bai', {body: [
        {...problem, trangThai: 'DANG_LAM', tenBo: 'Kiểm tra hàm số — 45 phút'},
        {...problem, maBai: 'E2E-NEXT'},
    ]});
    api.overrides.set('GET /api/hs/che-do', {body: [
        item('E2E-01', '', '', 'KIEM_TRA', {dueAt: problem.han}),
        item('E2E-NEXT', '', '', 'ON_TAP', {dueAt: problem.han}),
    ]});
    // Máy chủ gợi bài ôn; bài kiểm tra đang làm không được chen lên trước.
    api.overrides.set('GET /api/hs/goi-bai', {body: [item('E2E-NEXT', 'Bài kế', '', 'CUNG_CO', {})]});
    await page.goto('/hs');
    await expect(page.getByTestId('lam-buoc-tiep')).toHaveAttribute('href', '/hs/luyen/E2E-NEXT');
    await page.getByTestId('lam-buoc-tiep').click();
    await expect(page.getByTestId('mo-gia-su')).toBeVisible();
});
