// Xuất tệp vàng lời giải cho học sinh của v0 (T020, #87): chạy nguyên loiGiaiHocSinh của apps/web/lib/loi-giai.ts trên các
// ca cố định, ghi đầu vào và đầu ra để VietLoiGiaiTest của core so khớp từng chữ.
// Chạy từ gốc repo (Node ≥ 23.6): node specs/001-lat-cat-doc/doi-chieu/loi-giai-v0.ts
//   → services/core/src/test/resources/content/loi-giai-v0.json
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import path from 'node:path';
import { loiGiaiHocSinh } from '../../../apps/web/lib/loi-giai.ts';

const GOC = path.resolve(import.meta.dirname, '..', '..', '..');
const TEP = 'apps/web/lib/loi-giai.ts';
const SCRIPT = 'specs/001-lat-cat-doc/doi-chieu/loi-giai-v0.ts';
const blobCua = (tep: string) => execFileSync('git', ['hash-object', tep], { cwd: GOC }).toString().trim();
// TepVangDoiChieuTest của core so hai blob này với checkout: đổi nguồn hay script mà không sinh lại tệp vàng thì đỏ.
for (const tep of [TEP, SCRIPT]) {
  if (execFileSync('git', ['status', '--porcelain', '--', tep], { cwd: GOC }).toString().trim()) {
    throw new Error(`Dừng: ${tep} có thay đổi chưa commit`);
  }
}

// Ca đầu có dạng máy giải dựng (services/math/app/machine.py, «bai_lam»); các ca sau là các nhánh của loiGiaiHocSinh.
const ca: { ten: string; baiLam: unknown; finalAnswer: string | null }[] = [
  { ten: 'đủ các câu, dạng của máy giải', finalAnswer: null, baiLam: {
    ham: 'x**3 - 3*x**2 + 2', TXD: 'D = \\mathbb{R}', dao_ham: '3*x**2 - 6*x', y_phay_bang_0: ['0', '2'], y_phay_khong_xd: [],
    bang: { moc: ['-oo', '0', '2', '+oo'], dau: ['+', '-', '+'], dau_tai_diem: ['0', '0'], chieu: ['tang', 'giam', 'tang'] },
    ket_luan: { dong_bien: ['(-oo; 0)', '(2; +oo)'], nghich_bien: ['(0; 2)'], cuc_dai_x: ['0'], cuc_tieu_x: ['2'],
      gia_tri_cuc_dai: ['2'], gia_tri_cuc_tieu: ['-2'] } } },
  { ten: 'có đáp án cuối thì dùng đáp án cuối', finalAnswer: 'Hàm số đồng biến trên \\mathbb{R}.', baiLam: { TXD: 'R' } },
  { ten: 'đáp án cuối rỗng thì viết từ lời giải', finalAnswer: '', baiLam: { TXD: 'R' } },
  { ten: 'khóa kết luận có mà rỗng hay null', finalAnswer: null,
    baiLam: { ket_luan: { dong_bien: [], nghich_bien: null, cuc_dai_x: [], cuc_tieu_x: [] } } },
  { ten: 'y phẩy không xác định', finalAnswer: null, baiLam: { TXD: 'D = \\mathbb{R} \\setminus \\{1\\}', y_phay_khong_xd: ['1'],
    ket_luan: { nghich_bien: ['(-oo; 1)', '(1; +oo)'] } } },
  { ten: 'thiếu hay rỗng giá trị cực trị', finalAnswer: null,
    baiLam: { ket_luan: { cuc_dai_x: ['-1', '1'], gia_tri_cuc_dai: ['3'], cuc_tieu_x: ['0'], gia_tri_cuc_tieu: [''] } } },
  { ten: 'chuỗi và mảng rỗng', finalAnswer: null, baiLam: { TXD: '', dao_ham: '', y_phay_bang_0: [] } },
  { ten: 'kết luận null', finalAnswer: null, baiLam: { TXD: 'R', ket_luan: null } },
  { ten: 'đối tượng rỗng', finalAnswer: null, baiLam: {} },
  { ten: 'mảng', finalAnswer: null, baiLam: [] },
  { ten: 'null', finalAnswer: null, baiLam: null },
];

const ra = path.join(GOC, 'services/core/src/test/resources/content/loi-giai-v0.json');
const tep = {
  nguon: { tep: TEP, blob: blobCua(TEP), script: blobCua(SCRIPT) },
  ca: ca.map((c) => ({ ...c, loiGiai: loiGiaiHocSinh(c.baiLam, c.finalAnswer) })),
};
writeFileSync(ra, JSON.stringify(tep, null, 2) + '\n', 'utf8');
console.log(`đã ghi ${ca.length} ca vào ${path.relative(GOC, ra)}`);
