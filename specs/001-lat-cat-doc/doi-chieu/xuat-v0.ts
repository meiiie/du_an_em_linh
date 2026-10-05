// Đối chiếu v0 (T013, #85): chạy NGUYÊN mã nạp bài của apps/web/scripts/seed.ts (dựng bài, gọi dịch vụ toán, cổng 3 tầng)
// với dịch vụ toán thật và kho của lớp v2 — 5 tài liệu (3 của v0, sp-tai-lieu-0001, sp-tai-lieu-0002) và 6 dòng bảng
// công thức của v0, đọc từ đúng các tệp importer v2 đọc. Xuất:
//   - v0-bai.json: mỗi bài (mã, dấu vân tay kiểu v0, nguồn, trạng thái tổng, trạng thái phát hành, trạng thái từng tầng);
//   - phan-hoi-toan.json: mọi cặp yêu cầu → phản hồi của dịch vụ toán theo thứ tự gọi (yêu cầu verify bỏ kho lớp, vì
//     kho ghi một lần ở v0-bai.json), để test T014 phát lại mà không cần dịch vụ toán thật.
// Không gõ lại logic của v0: đoạn mã được cắt theo mốc từ seed.ts, bỏ kiểu bằng stripTypeScriptTypes của Node, rồi chạy
// trong vm với một `db` giả chỉ ghi lại `db.insert(<bảng>).values(...)`. Thay duy nhất một chỗ: hằng `corpus` (kho của
// lớp v0, một tài liệu) thành kho của lớp v2.
// Dịch vụ toán không lấy từ một URL có sẵn (không biết nó build từ mã nào: /health luôn báo 0.1.0): script tự build ảnh
// services/math từ chính checkout này (dừng nếu services/math có thay đổi chưa commit), chạy ở một cổng ngẫu nhiên của
// 127.0.0.1, rồi ghi cây git của services/math và id ảnh vào nguồn của tệp vàng.
// Chạy từ gốc repo (Node ≥ 23.6, Docker):
//   node specs/001-lat-cat-doc/doi-chieu/xuat-v0.ts
// KHO_LOP=v0 giữ nguyên kho của seed.ts (một tài liệu) và chỉ in trạng thái từng bài, không ghi tệp: để tách ảnh hưởng
// của kho tài liệu khỏi ảnh hưởng của mã.
import { createHash, randomUUID } from 'node:crypto';
import { execSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { stripTypeScriptTypes } from 'node:module';
import path from 'node:path';
import vm from 'node:vm';
import { chayDichVuToan, doan, giua, nguonGit, type DichVuToan } from './chung.ts';

const GOC = path.resolve(import.meta.dirname, '..', '..', '..');
const TEP = 'apps/web/scripts/seed.ts';

const src = readFileSync(path.join(GOC, TEP), 'utf8');

function nguonDaCommit() {
  const blob = nguonGit(GOC, [TEP])[TEP];
  return { tep: TEP, commit: execSync('git log -1 --format=%H -- ' + TEP, { cwd: GOC }).toString().trim(), blob };
}

/**
 * Đầu vào khác của tệp vàng: dữ liệu script đọc trực tiếp (bài ví dụ, bài khung ngắn, tài liệu của lab và của v0, bảng công
 * thức) dưới hai thư mục data, và chính script này (thay kho, lọc yêu cầu, ánh xạ kết quả). Dừng nếu có thay đổi chưa commit
 * (dữ liệu lab chỉ đổi qua bản vá nguyên văn có mã), và ghi cây hay blob git của từng thứ để tệp vàng tái lập được từ đúng
 * các phiên bản đã ghi.
 */
const DU_LIEU = ['data/supham', 'data/v0', 'specs/001-lat-cat-doc/doi-chieu/xuat-v0.ts', 'specs/001-lat-cat-doc/doi-chieu/chung.ts'];

const docJson = (tep: string) => JSON.parse(readFileSync(path.join(GOC, tep), 'utf8'));

/** Kho của lớp v2, đúng dạng `corpus` của seed.ts; id là mã ổn định (v0 dùng UUID ngẫu nhiên). */
function khoLop() {
  const taiLieu = [
    ...docJson('data/v0/tai-lieu.json'),
    docJson('data/supham/tai-lieu/sp-tai-lieu-0001.json'),
    docJson('data/supham/tai-lieu/sp-tai-lieu-0002.json'),
  ];
  const bang = docJson('data/v0/bang-cong-thuc.json');
  return {
    tai_lieu: taiLieu.map((d) => ({ id: d.ma, ten: d.title, text: d.textContent, license_status: d.licenseStatus, phien_ban: d.version })),
    cong_thuc: bang.formulas.map((f: { ma: string; latex: string; noiDung: string; title: string }) => ({
      id: f.ma,
      latex: f.latex,
      noi_dung: f.noiDung,
      ten: f.title,
    })),
  };
}

async function main() {
  const seed = nguonDaCommit();
  nguonGit(GOC, DU_LIEU);
  const kho = khoLop();
  const toan = await chayDichVuToan(GOC);
  try {
    await xuat(seed, kho, toan);
  } finally {
    toan.dung();
  }
}

async function xuat(seed: ReturnType<typeof nguonDaCommit>, kho: ReturnType<typeof khoLop>, toan: DichVuToan) {

  // Cắt mã của v0 theo mốc. Đoạn nạp bài: từ khung bước tới trước phần dữ liệu học sinh mẫu (`const levels`).
  const CORPUS = doan(src, TEP, 'const corpus = {', '\n  };\n');
  let doanNap = giua(src, TEP, 'const steps = [', 'const levels');
  if (doanNap.split(CORPUS).length !== 2) throw new Error('cần đúng một hằng corpus trong đoạn nạp bài');
  const khoGoc = process.env.KHO_LOP === 'v0';
  if (!khoGoc) doanNap = doanNap.replace(CORPUS, 'const corpus = __khoLop;\n');
  const ma = [
    doan(src, TEP, 'const MUC4: Record<string, string> = {', '\n};\n'),
    doan(src, TEP, 'const MUC3: Record<string, string> = {', '\n};\n'),
    doan(src, TEP, 'function contentHash(', '\n}\n'),
    doan(src, TEP, 'async function math<T>(', '\n}\n'),
    doan(src, TEP, 'function hintText(', '\n}\n'),
    doan(src, TEP, 'async function napBai(', '\n}\n'),
    doanNap,
  ].join('\n');
  const js = stripTypeScriptTypes('(async () => {\n' + ma + '\n})()', { mode: 'strip' });

  const daGhi: { bang: string; o: Record<string, unknown> }[] = [];
  const goiToan: { job: string; yeu_cau: Record<string, unknown>; phan_hoi: unknown }[] = [];
  const BANG = ['problems', 'solutions', 'hintLevels', 'verificationRuns', 'verificationTierResults', 'stepTemplates',
    'masteryConfig', 'documents', 'formulaSheets', 'formulas'];
  const ngu = {
    db: { insert: (bang: string) => ({ values: async (o: Record<string, unknown>) => void daGhi.push({ bang, o }) }) },
    ...Object.fromEntries(BANG.map((b) => [b, b])),
    createHash,
    crypto: { randomUUID },
    GV: '<giao-vien>',
    LOP: '<lop>',
    now: new Date(0),
    process: { env: { MATH_SERVICE_URL: toan.url } },
    repoRoot: () => GOC,
    existsSync: () => false,
    spawnSync: () => {
      throw new Error('xuat-v0 chỉ gọi dịch vụ toán qua HTTP');
    },
    path,
    readFileSync,
    root: GOC,
    examples: docJson('data/supham/03-vi-du-bai-tap.json'),
    __khoLop: kho,
    console: { log: () => {}, error: console.error },
    fetch: async (url: string, init: { body: string }) => {
      const r = await fetch(url, init as RequestInit);
      const chu = await r.text();
      const yeuCau = JSON.parse(init.body);
      const job = new URL(url).pathname.replace('/v1/', '');
      if (job === 'verify') {
        delete yeuCau.tai_lieu;
        delete yeuCau.cong_thuc;
      }
      goiToan.push({ job, yeu_cau: yeuCau, phan_hoi: r.ok ? JSON.parse(chu) : { http: r.status, loi: chu.slice(0, 500) } });
      return new Response(chu, { status: r.status, headers: r.headers });
    },
  };
  await vm.runInNewContext(js, ngu, { filename: 'seed.ts (đoạn nạp bài)' });

  const cua = (bang: string) => daGhi.filter((g) => g.bang === bang).map((g) => g.o);
  const luot = new Map(cua('verificationRuns').map((r) => [r.problemId, r]));
  const loiGiai = new Map(cua('solutions').map((s) => [s.problemId, s]));
  const goiY = cua('hintLevels');
  const tang = cua('verificationTierResults');
  const bai = cua('problems')
    .map((p) => {
      const r = luot.get(p.id) as Record<string, unknown>;
      return {
        ma: p.code,
        dau_van_tay_v0: p.contentHash,
        nguon_bai: p.origin,
        dang_tra_loi: p.dangTraLoi,
        trang_thai_tong: r.overallStatus,
        trang_thai_phat_hanh: r.publishStatus,
        // Dữ kiện bảo vệ v0 ghi cho bài (solutions.protectedFacts): bộ lọc lộ đáp án của gia sư dùng chúng, mà dấu vân
        // tay {de, bl, hints} không gồm chúng.
        su_kien_bao_ve: (loiGiai.get(p.id) as Record<string, unknown>).protectedFacts,
        // Mọi cột nội dung v0 ghi cho bài, nguyên văn: test so từng cột của core với đây, không với chính bài core dựng.
        cot_v0: {
          skillCode: p.skillCode,
          skillCodesPhu: p.skillCodesPhu,
          mucDo4: p.mucDo4,
          mucDoBo3: p.mucDoBo3,
          bloomLevel: p.bloomLevel,
          difficulty: p.difficulty,
          statementText: p.statementText,
          statementLatex: p.statementLatex,
          hamSympy: p.hamSympy,
          buocBatDau: p.buocBatDau,
          baiLam: (loiGiai.get(p.id) as Record<string, unknown>).baiLam,
          finalAnswer: (loiGiai.get(p.id) as Record<string, unknown>).finalAnswer,
          goiY: goiY
            .filter((h) => h.problemId === p.id)
            .map((h) => ({ maBuoc: h.maBuoc, cap: h.cap, noiDung: h.noiDung })),
        },
        tang: tang
          .filter((t) => t.runId === r.id)
          .map((t) => ({ tang: t.tier, trang_thai: t.status }))
          .sort((a, b) => (a.tang as number) - (b.tang as number)),
      };
    })
    .sort((a, b) => String(a.ma).localeCompare(String(b.ma)));

  if (khoGoc) {
    for (const b of bai) console.log(`${b.ma}\t${b.trang_thai_phat_hanh}\t${b.tang.map((t) => t.trang_thai).join('/')}`);
    return;
  }
  const ghi = (ten: string, giaTri: unknown) =>
    writeFileSync(path.join(import.meta.dirname, ten), JSON.stringify(giaTri, null, 2) + '\n', 'utf8');
  ghi('v0-bai.json', {
    nguon: {
      seed,
      du_lieu: nguonGit(GOC, DU_LIEU),
      dich_vu_toan: toan.nguon,
      kho_lop: {
        tai_lieu: kho.tai_lieu.map((d) => d.id),
        cong_thuc: kho.cong_thuc.map((c: { id: string }) => c.id),
        bam: createHash('sha256').update(JSON.stringify(kho)).digest('hex'),
      },
    },
    bai,
  });
  ghi('phan-hoi-toan.json', goiToan);
  const dem = (k: string) => bai.filter((b) => b.trang_thai_phat_hanh === k).length;
  console.log(`v0: ${bai.length} bài (DA_PHAT_HANH ${dem('DA_PHAT_HANH')}, CHO_GIAO_VIEN_DUYET ${dem('CHO_GIAO_VIEN_DUYET')}, ` +
    `BI_CHAN ${dem('BI_CHAN')}); ${goiToan.length} lần gọi dịch vụ toán; services/math ${toan.nguon.cay_git}, ảnh ${toan.anh}`);
}

await main();
