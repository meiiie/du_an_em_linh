import { test as base, expect, type Page } from '@playwright/test';
import type { VaiTro } from '../../src/app/api/auth';
import type { BaiCuaHocSinh, ChiTietBai } from '../../src/app/api/hoc-sinh';
import type { SessionGiaSuV2, TurnGiaSuV2 } from '../../src/app/api/gia-su';
import type { DeadlineDetails } from '../../src/app/api/ux';
export const item = <D extends object = Record<string, unknown>>(id: string, title: string, subtitle = '', status = 'DAT', details: D = {} as D) => ({ id, title, subtitle, status, details });
export const levels = ['NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO'];
export const problem: BaiCuaHocSinh = { maBai: 'E2E-01', deBai: 'Xét tính đơn điệu của hàm số', deBaiLatex: 'y=x^3-3x', kyNang: 'T12.DH.03', tenKyNang: 'Đơn điệu', muc4: 'NHAN_BIET', tenBo: 'Ôn đơn điệu', han: '2026-10-20T12:00:00Z', trangThai: 'CHUA_LAM', soBuocDat: 0, soBuoc: 5, ketQua: null };
export const deadline = item<DeadlineDetails>('han1', 'Ôn đơn điệu', 'Hạn tuần này', 'ON_TAP', {
    tenBo: 'Ôn đơn điệu', setKey: 'bo1', classId: 'lop1', mode: 'ON_TAP', dueAt: '2026-10-20T12:00:00Z',
    problemCodes: ['E2E-01', 'E2E-NEXT'], problemCount: 2, studentCount: 3, assignmentCount: 6, submittedCount: 1,
});
const steps = ['TXD', 'DAOHAM', 'NGHIEM', 'XETDAU', 'KETLUAN'].map((s, i) => ({ maBuoc: `B.DH.${s}`, ten: ['Tập xác định', 'Đạo hàm', 'Nghiệm', 'Xét dấu', 'Kết luận'][i], viec: 'Trình bày bước đang làm.', kieu: ['TXD', 'DAO_HAM', 'NGHIEM', 'BANG_XET_DAU', 'KET_LUAN_DH'][i] }));
export const detail: ChiTietBai = { maBai: problem.maBai, giaoBaiId: 'assignment-01', de: { text: problem.deBai, latex: problem.deBaiLatex }, kyNang: problem.kyNang, tenKyNang: problem.tenKyNang, muc4: problem.muc4, dangTraLoi: 'TU_LUAN_5_BUOC', buocBatDau: null, khaiBaoKetLuan: ['dong_bien', 'nghich_bien'], cacBuoc: steps, baiLam: { trangThai: 'DANG_LAM', cacBuoc: [] }, coTheMoLoiGiai: false };
export const formula = item('ct1', 'Đạo hàm, hàm đa thức', 'y=x^n', 'KHOA', { statement: 'Định nghĩa "đạo hàm", có dấu phẩy', tier1: 'DAT', tier2: 'DAT', version: 1 });
export const csv = '"thuật ngữ","định nghĩa"\r\n"Đạo hàm, hàm đa thức","y=x^n — Định nghĩa ""đạo hàm"", có dấu phẩy"\r\n';
export const bank = [item('E2E-01', 'Bài nhận biết', problem.deBai, 'DA_PHAT_HANH', { code: 'E2E-01', skillCode: problem.kyNang, level4: 'NHAN_BIET', gate: 'DAT', latex: problem.deBaiLatex }), item('E2E-02', 'Bài thông hiểu', 'Xét cực trị', 'NHAP', { code: 'E2E-02', skillCode: problem.kyNang, level4: 'THONG_HIEU', gate: 'KHONG_KIEM_DUOC', latex: 'y=x^2' }), item('E2E-03', 'Bài cần sửa', '', 'BI_CHAN', { level4: 'VAN_DUNG', gate: 'SAI', skillCode: problem.kyNang, latex: 'y=x' })];
export const student = item('hs1', 'An', 'hs.an@demo.local', 'STUDENT', { skills: [{ code: problem.kyNang, name: 'Đơn điệu', level4: 'NHAN_BIET', level3: 'Biết', mastery: 0.4, overridden: false }], submitted: 1 });
export const tutorSession = (lastSeq=0): SessionGiaSuV2 => ({
    id:'e2e-session',studentId:'synthetic-user',assignmentId:detail.giaoBaiId!,classId:'lop1',
    tomTat:'',summaryThrough:0,contextVersion:'1:synthetic',lastSeq,revision:1,tokenCount:0,
});
export class MockApi {
    role: VaiTro = 'STUDENT';
    mode = 'ON_TAP';
    tutorStatus = 200;
    mastered = false;
    tutorSeq = 0;
    tutorTurns: TurnGiaSuV2[] = [];
    calls: {
        method: string;
        path: string;
        body: any;
    }[] = [];
    unexpected: string[] = [];
    overrides = new Map<string, {
        status?: number;
        body: unknown;
    }>();
    constructor(private page: Page) {
    }
    async install() {
        // Các ca xác thực mô phỏng một trình duyệt đã đăng nhập trước đó.
        await this.page.addInitScript(() => localStorage.setItem('mathl-co-phien', '1'));
        await this.page.route('**/api/**', async (route) => {
            const req = route.request();
            const path = new URL(req.url()).pathname;
            const method = req.method();
            const body = req.postData() ? req.postDataJSON() : null;
            this.calls.push({ method, path, body });
            const override = this.overrides.get(`${method} ${path}`);
            if (override) {
                await route.fulfill({ status: override.status ?? 200, json: override.body });
                return;
            }
            const tutorBase = '/api/hs/gia-su/v2/phien';
            if (path === tutorBase) {
                await route.fulfill({ json: {sessionId:'e2e-session',lastSeq:this.tutorSeq,turnId:this.tutorTurns[0]?.id ?? null,status:this.tutorTurns[0]?.status ?? 'idle'} });
                return;
            }
            if (path === tutorBase + '/e2e-session/luot' && method === 'GET') {
                await route.fulfill({ json: {session:tutorSession(this.tutorSeq),turns:this.tutorTurns.slice(Number(new URL(req.url()).searchParams.get('offset') ?? 0), Number(new URL(req.url()).searchParams.get('offset') ?? 0) + Number(new URL(req.url()).searchParams.get('limit') ?? 50))} });
                return;
            }
            if (path === tutorBase + '/e2e-session/cancel') {
                await route.fulfill({ json: {sessionId:'e2e-session',turnId:body.turnId,status:'cancelled',lastSeq:++this.tutorSeq} });
                return;
            }
            if (path === tutorBase + '/e2e-session/luot' && method === 'POST') {
                const turnId = 'turn-' + body.requestId;
                const event = (name: string, extra: object) => {
                    const seq = ++this.tutorSeq;
                    return `id: e2e-session:${seq}\nevent: ${name}\ndata: ${JSON.stringify({v:2,sessionId:'e2e-session',turnId,seq,...extra})}\n\n`;
                };
                const text = '**Lời đã kiểm trong mock** $x^2$.';
                await route.fulfill({ status:this.tutorStatus, contentType:'text/event-stream', body:this.tutorStatus === 200
                    ? event('start',{requestId:body.requestId,maBuoc:body.maBuoc,cap:body.cap,contextVersion:'1:synthetic'}) + event('delta',{text}) + event('done',{status:'completed',tokens:10,durationMs:20}) : '' });
                if (this.tutorStatus === 200) this.tutorTurns.unshift({id:turnId,sessionId:'e2e-session',requestId:body.requestId,ordinal:this.tutorTurns.length+1,step:body.maBuoc,level:body.cap,question:body.cauHoi,answer:text,status:'completed',createdAt:'2026-10-14T00:00:00Z'});
                return;
            }
            let response: unknown;
            if (path === '/api/auth/refresh' || path === '/api/auth/login')
                response = { accessToken: 'synthetic-access-token', accessTokenExpiresAt: '2099-01-01T00:00:00Z', user: { id: 'synthetic-user', email: 'demo@demo.local', displayName: 'An', role: this.role } };
            else if (path === '/api/auth/logout')
                response = {};
            else if (path === '/api/hs/trang-hoc')
                response = { ten: 'An', soKyNang: [{ kyNang: problem.kyNang, tenKyNang: 'Đơn điệu', muc4: this.mastered ? 'THONG_HIEU' : 'NHAN_BIET', ket: false }], hoanThanh: { kyNang: [], chuDe: [] } };
            else if (path === '/api/hs/chu-de')
                response = [item('DH12','Đơn điệu và cực trị')];
            else if (path === '/api/hs/tu-hoc')
                response = [item(problem.maBai,'Đơn điệu',problem.deBai)];
            else if (path.startsWith('/api/hs/tu-hoc/') && method === 'POST')
                response = {message:'Đã chọn bài tự học.',count:1};
            else if (path === '/api/hs/nhac/push')
                response = {available:false,publicKey:''};
            else if (path === '/api/hs/bai')
                response = [problem, { ...problem, maBai: 'E2E-NEXT', muc4: 'THONG_HIEU' }];
            else if (/^\/api\/hs\/bai\/[^/]+$/.test(path))
                response = { ...detail, maBai: path.split('/').at(-1) };
            else if (path.endsWith('/buoc'))
                response = { ketQua: 'DAT', thongBao: 'Bước đạt.', oSai: [] };
            else if (path.endsWith('/nop')) {
                this.mastered = true;
                response = { ketQua: 'DAT', mucHieu: [{ kyNang: problem.kyNang, muc4Truoc: 'NHAN_BIET', muc4Sau: 'THONG_HIEU' }] };
            }
            else if (path === '/api/hs/che-do')
                response = [problem.maBai,'E2E-NEXT'].map(id=>item(id, 'Bài thử', '', this.mode, { dueAt: '2026-10-20T12:00:00Z' }));
            else if (path === '/api/hs/goi-bai')
                response = [item(this.mastered ? 'E2E-NEXT' : problem.maBai, 'Bài kế', 'Nâng 1 nấc', 'NANG_1_NAC', { level4: this.mastered ? 'THONG_HIEU' : 'NHAN_BIET', skillCode: problem.kyNang })];
            else if (path === '/api/hs/kho' || path === '/api/gv/kho')
                response = { formulas: [formula], documents: [item('tl1', 'Tài liệu đơn điệu', 'Tài liệu tổng hợp do lớp biên soạn.', 'tu_soan')] };
            else if (path === '/api/hs/flashcards/export')
                response = { filename: 'cong-thuc.csv', content: csv };
            else if (path === '/api/hs/ke-hoach-tuan')
                response = { sessions: [], loiKhuyen: 'Bắt đầu bằng một bài Nhận biết.' };
            else if (path === '/api/gv/canh-bao')
                response = [];
            else if (path === '/api/hs/lich' || path === '/api/hs/nhac')
                response = method === 'GET' ? [] : { message: 'Đã thêm buổi học.', count: 1 };
            else if (path === '/api/gv/lop' || path === '/api/qt/lop')
                response = [item('lop1', '12A1 thử', '2026-2027', 'MO', {enabled:true})];
            else if (/^\/api\/(gv|qt)\/lop\/[^/]+\/gia-su\/su-dung$/.test(path))
                response = {classId:'lop1',requests:0,inputTokens:0,outputTokens:0,estimatedTokens:0,students:[]};
            else if (/^\/api\/qt\/lop\/[^/]+\/khoa$/.test(path) && method === 'PUT')
                response = {message:'Đã lưu.',count:1};
            else if (path === '/api/gv/ky-nang')
                response = [item(problem.kyNang, 'Hàm số', '', 'KY_NANG', {})];
            else if (path === '/api/gv/tong-quan')
                response = { students: 3, stuck: 1, pending: 1, assignments: 6, distribution: Object.fromEntries(levels.map(l => [l, 1])), deadlines: [deadline] };
            else if (path === '/api/gv/tien-do')
                response = [student];
            else if (path === '/api/gv/hoc-sinh/hs1')
                response = { student, history: [item('h1', 'Bài đã làm', '', 'DAT')] };
            else if (path === '/api/gv/ngan-hang')
                response = bank;
            else if (path === '/api/gv/cong-thuc')
                response = [formula];
            else if (path === '/api/gv/duyet')
                response = [item('duyet1', 'Bài chờ kiểm', 'Cần xem lại điều kiện', 'KHONG_KIEM_DUOC')];
            else if (path === '/api/gv/giao')
                response = method === 'GET' ? [] : { message: 'Đã giao bộ bài.', count: 1 };
            else if (path === '/api/gv/cai-dat')
                response = { lopId: 'lop1', revealSolutionAfterSubmit: false, aiProvider: 'offline' };
            else if (path === '/api/qt/nguoi-dung')
                response = [item('hs1', 'An', 'hs.an@demo.local', 'STUDENT', { enabled: true, synthetic: true }), item('gv1', 'Cô Bình', 'gv@demo.local', 'TEACHER', { enabled: true, synthetic: true })];
            else if (path === '/api/qt/nhat-ky')
                response = [];
            else if (['/api/gv/duyet/duyet1', '/api/gv/muc-hieu', '/api/qt/nguoi-dung/bulk', '/api/qt/lop/phan-cong'].includes(path) && method === 'POST')
                response = { message: 'Đã lưu.', count: 1 };
            else {
                this.unexpected.push(`${method} ${path}`);
                await route.fulfill({ status: 501, json: { detail: 'Missing acceptance mock' } });
                return;
            }
            await route.fulfill({ json: response });
        });
    }
}
export const test = base.extend<{
    api: MockApi;
}>({ api: [async ({ page }, use) => {
        const api = new MockApi(page);
        await api.install();
        await use(api);
        expect(api.unexpected, 'Every API request needs an explicit mock').toEqual([]);
    }, { auto: true }] });
export { expect };
export async function navigate(page: Page, id: string) {
    if ((page.viewportSize()?.width ?? 1440) < 1024)
        await page.getByTestId('mo-sidebar').click();
    await page.getByTestId(id).click();
}
export async function noOverflow(page: Page) {
    expect(await page.evaluate(() => document.documentElement.scrollWidth - innerWidth)).toBeLessThanOrEqual(1);
}
export async function flat(page: Page) {
    const shadows = await page.evaluate(() => [...document.querySelectorAll('*')].filter(el => el.getClientRects().length && getComputedStyle(el).visibility !== 'hidden').flatMap(el => ['', '::before', '::after'].map(pseudo => ({ element: el.tagName + '.' + el.className, pseudo, shadow: getComputedStyle(el, pseudo || null).boxShadow })).filter(x => x.shadow !== 'none')));
    expect(shadows, 'Visible elements and pseudo-elements must have no box-shadow').toEqual([]);
}
