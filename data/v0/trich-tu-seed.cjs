// Trích nguyên văn hằng nội dung của v0 từ apps/web/scripts/seed.ts ra data/v0/*.json (#85, T011b).
// Không gõ lại chữ hay tên trường: chạy nguyên đoạn mã nạp nội dung của seed.ts bằng vm, với một `db` giả chỉ ghi lại
// đối tượng truyền vào `db.insert(<bảng>).values(...)`. Giá trị là giá trị lúc chạy (nên `\\ge` của TS thành `\ge`).
// Chạy từ gốc repo: node data/v0/trich-tu-seed.cjs  (ghi đè data/v0/*.json và NGUON.md).
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const { execSync } = require('child_process');

const GOC = path.resolve(__dirname, '..', '..');
const TEP = 'apps/web/scripts/seed.ts';
const src = fs.readFileSync(path.join(GOC, TEP), 'utf8');
const dong = (i) => src.slice(0, i).split('\n').length;

function viTri(moc, tu = 0) {
  const i = src.indexOf(moc, tu);
  if (i < 0) throw new Error('thiếu mốc ' + JSON.stringify(moc));
  return i;
}

/** Dòng đầu và cuối (1-based) của đoạn từ mốc `dau` tới hết mốc `cuoi` đầu tiên sau đó. */
function dongCua(dau, cuoi, tu = 0) {
  const a = viTri(dau, tu);
  const b = viTri(cuoi, a + dau.length) + cuoi.length;
  return { tu: dong(a), den: dong(b), het: b };
}

/** Lấy các khóa `giu` theo thứ tự đó; khóa nào không thuộc `giu` hay `bo` thì dừng, để seed.ts thêm trường là biết. */
function chon(o, giu, bo, ten) {
  const la = Object.keys(o).filter((k) => !giu.includes(k) && !bo.includes(k));
  const thieu = giu.filter((k) => !(k in o));
  if (la.length || thieu.length) throw new Error(`${ten}: khóa lạ ${la.join(', ') || '-'}; thiếu ${thieu.join(', ') || '-'}`);
  return Object.fromEntries(giu.map((k) => [k, o[k]]));
}

// Mã ổn định gắn với tiêu đề của v0, không với vị trí: seed.ts đổi thứ tự thì mã vẫn theo đúng nội dung; tiêu đề lạ hay
// thiếu thì dừng, không đoán. d-1 … d-6 trùng mã của test services/math/tests/test_dong_cong_thuc.py.
const MA_TAI_LIEU = {
  'Ghi chú tự soạn: đơn điệu và cực trị': 'v0-don-dieu',
  'Đề mẫu tự soạn — cùng dạng đa thức bậc ba': 'v0-de-mau',
  'Tham khảo phương pháp — ôn đơn điệu thế nào': 'v0-phuong-phap',
};
const MA_CONG_THUC = {
  'Đạo hàm lũy thừa': 'd-1',
  'Đạo hàm tổng': 'd-2',
  'Đạo hàm thương': 'd-3',
  'Đơn điệu': 'd-4',
  'Cực trị': 'd-5',
  'Điểm tới hạn': 'd-6',
};

function maTheoTieuDe(bang, tieuDe, ten) {
  if (!Object.hasOwn(bang, tieuDe)) throw new Error(`${ten} có tiêu đề lạ ${JSON.stringify(tieuDe)}: thêm mã vào bảng mã trước`);
  return bang[tieuDe];
}

function daDuMa(bang, banGhi, ten) {
  const thay = banGhi.map((b) => b.ma);
  const thieu = Object.values(bang).filter((m) => !thay.includes(m));
  if (thieu.length || new Set(thay).size !== thay.length) throw new Error(`${ten}: thiếu ${thieu.join(', ') || '-'} hoặc trùng mã`);
}

/** Commit cuối đổi tệp và blob ở HEAD; dừng nếu tệp đang đọc khác bản đã commit (NGUON.md phải ứng với đúng nội dung). */
function nguonDaCommit() {
  const git = (lenh) => execSync('git ' + lenh, { cwd: GOC }).toString().trim();
  if (git('status --porcelain -- ' + TEP)) throw new Error(TEP + ' có thay đổi chưa commit: commit hay hoàn tác trước khi trích');
  const blob = git('rev-parse HEAD:' + TEP);
  if (git('hash-object -- ' + TEP) !== blob) throw new Error(TEP + ' khác blob ở HEAD');
  return { commit: git('log -1 --format=%H -- ' + TEP), blob };
}

async function main() {
  const { commit, blob } = nguonDaCommit();
  // Đoạn nạp nội dung: từ khung bước tới hết vòng ghi công thức (trước `const corpus`, là kho gia sư lúc chạy của v0).
  const dau = viTri('const steps = [');
  const cuoi = viTri('const corpus = {', dau);
  const doanMa = src.slice(dau, cuoi);
  if (doanMa.split(' as const').length !== 2) throw new Error('cần đúng một `as const` (cú pháp TS) trong đoạn');

  const daGhi = [];
  const ngu = {
    db: { insert: (bang) => ({ values: async (o) => void daGhi.push({ bang, o }) }) },
    stepTemplates: 'stepTemplates',
    masteryConfig: 'masteryConfig',
    documents: 'documents',
    formulaSheets: 'formulaSheets',
    formulas: 'formulas',
    crypto: { randomUUID: () => '<uuid>' },
    GV: '<giao-vien>',
    LOP: '<lop>',
    now: '<now>',
  };
  await vm.runInNewContext('(async () => {\n' + doanMa.replace(' as const', '') + '\n})()', ngu);

  const cua = (bang, n) => {
    const r = daGhi.filter((g) => g.bang === bang).map((g) => g.o);
    if (r.length !== n) throw new Error(`${bang}: cần ${n} lần ghi, có ${r.length}`);
    return r;
  };
  const buoc = cua('stepTemplates', 5).map((o, i) =>
    chon(o, ['maBuoc', 'topicCode', 'thuTu', 'dangNhap', 'skillCode', 'moTa'], [], 'bước ' + (i + 1)),
  );
  const [bkt] = cua('masteryConfig', 1).map((o) => chon(o, ['key', 'version', 'value'], [], 'BKT'));
  const taiLieu = cua('documents', 3).map((o) => ({
    ma: maTheoTieuDe(MA_TAI_LIEU, o.title, 'tài liệu'),
    // Thứ tự khóa như bản vá sp-tai-lieu-0001 của lab Sư phạm.
    ...chon(o, ['title', 'kind', 'source', 'licenseStatus', 'version', 'textContent'], ['id', 'uploadedBy', 'createdAt'], o.title),
  }));
  daDuMa(MA_TAI_LIEU, taiLieu, 'tài liệu');
  const [bang] = cua('formulaSheets', 1).map((o) =>
    chon(o, ['version', 'note'], ['id', 'classId', 'ownerTeacherId', 'status', 'lockedAt'], 'bảng công thức'),
  );
  const congThuc = cua('formulas', 6).map((o) => ({
    ma: maTheoTieuDe(MA_CONG_THUC, o.title, 'dòng công thức'),
    ...chon(o, ['skillCode', 'title', 'latex', 'noiDung'], ['id', 'formulaSheetId'], o.title),
  }));
  daDuMa(MA_CONG_THUC, congThuc, 'dòng công thức');

  const ghi = (ten, giaTri) => fs.writeFileSync(path.join(__dirname, ten), JSON.stringify(giaTri, null, 2) + '\n', 'utf8');
  ghi('khung-buoc.json', buoc);
  ghi('bkt.json', bkt);
  ghi('tai-lieu.json', taiLieu);
  ghi('bang-cong-thuc.json', { ...bang, formulas: congThuc });

  const dBuoc = dongCua('const steps = [', 'moTa: mota,\n    });\n  }');
  const dBkt = dongCua('await db.insert(masteryConfig).values({', '\n  });');
  // Từ chữ tài liệu 1 (`const docText`) tới hết lần ghi tài liệu thứ ba.
  const GHI_TAI_LIEU = 'await db.insert(documents).values({';
  const thu3 = viTri(GHI_TAI_LIEU, viTri(GHI_TAI_LIEU, viTri(GHI_TAI_LIEU) + 1) + 1);
  const dTaiLieu = { tu: dong(viTri('const docText = [')), den: dongCua(GHI_TAI_LIEU, '\n  });', thu3).den };
  const dBang = dongCua('await db.insert(formulaSheets).values({', 'noiDung: noi,\n    });\n  }');
  const doanTu = dong(dau);
  const doanDen = dong(cuoi) - 1;

  const nguon = `# Nguồn của data/v0

Các tệp JSON ở đây chép **nguyên văn** hằng nội dung của v0 trong \`${TEP}\`, không sửa chữ (#85, T011b). Tệp do
\`data/v0/trich-tu-seed.cjs\` sinh. Script chạy nguyên đoạn mã nạp nội dung của seed.ts (dòng ${doanTu}–${doanDen}) bằng \`vm\`,
với một \`db\` giả chỉ ghi lại đối tượng truyền vào \`db.insert(<bảng>).values(...)\`. Vì vậy tên trường là tên v0 dùng khi
ghi, và giá trị là giá trị lúc chạy (\`\\\\ge\` trong mã TS thành \`\\ge\`). Chạy lại: \`node data/v0/trich-tu-seed.cjs\` từ gốc
repo. Script dừng nếu seed.ts thêm hay bớt trường, đổi số lần ghi, có tiêu đề lạ, hay khác bản đã commit (blob dưới đây
là blob của đúng nội dung đã trích).

Nguồn: \`${TEP}\`, commit cuối đổi tệp \`${commit}\`, blob \`${blob}\`.

| Tệp | Nội dung | Bảng v0 | Dòng trong seed.ts |
| --- | --- | --- | --- |
| \`khung-buoc.json\` | Khung 5 bước \`B.DH.*\` | \`stepTemplates\` | ${dBuoc.tu}–${dBuoc.den} |
| \`bkt.json\` | Cấu hình BKT, khóa \`bkt\` | \`masteryConfig\` | ${dBkt.tu}–${dBkt.den} |
| \`tai-lieu.json\` | Ba tài liệu tự soạn của lớp | \`documents\` | ${dTaiLieu.tu}–${dTaiLieu.den} |
| \`bang-cong-thuc.json\` | Bảng công thức: phiên bản, ghi chú, 6 dòng | \`formulaSheets\`, \`formulas\` | ${dBang.tu}–${dBang.den} |

Khác với v0:
- Bỏ trường lúc chạy: \`id\`, \`uploadedBy\`, \`createdAt\` của tài liệu; \`id\`, \`classId\`, \`ownerTeacherId\`, \`lockedAt\` của
  bảng; \`id\`, \`formulaSheetId\` của dòng công thức.
- Bỏ \`status: "locked"\` của bảng: v2 chỉ khóa bảng khi mọi dòng \`DAT\` qua job \`kiem-dong-cong-thuc\` (ADR 013, T012).
- Thêm mã ổn định để importer nhận lại khi nạp lần hai (v0 dùng UUID ngẫu nhiên). Mã gắn với tiêu đề của v0, không với
  vị trí, nên seed.ts đổi thứ tự thì mã vẫn theo đúng nội dung:
${[...Object.entries(MA_TAI_LIEU), ...Object.entries(MA_CONG_THUC)].map(([t, m]) => `  - \`${m}\`: «${t}»`).join('\n')}
- Mã dòng công thức \`d-1\` … \`d-6\` trùng mã của test \`services/math/tests/test_dong_cong_thuc.py\`.
- Khóa của tài liệu xếp như bản vá \`sp-tai-lieu-0001\` của lab Sư phạm, để importer đọc một định dạng cho cả tài liệu của
  v0 và của lab.
`;
  fs.writeFileSync(path.join(__dirname, 'NGUON.md'), nguon, 'utf8');
  console.log('đã ghi: khung-buoc.json, bkt.json, tai-lieu.json, bang-cong-thuc.json, NGUON.md');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
