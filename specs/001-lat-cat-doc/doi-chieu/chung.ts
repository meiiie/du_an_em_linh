// Phần dùng chung của các script đối chiếu v0 (xuat-v0.ts, cham-v0.ts): dựng dịch vụ toán từ chính checkout, cắt mã v0
// theo mốc, ghi nguồn git của tệp vàng.
import { execFileSync, execSync } from 'node:child_process';

export type DichVuToan = {
  url: string;
  anh: string;
  dung: () => void;
  /** Ghi nguyên vào tệp vàng. */
  nguon: { cay_git: string; anh_goc: { ten: string; digest: string }; python: string; goi_python: string[]; suc_khoe: unknown };
};

const TOAN = 'services/math';

/** Build và chạy services/math của checkout ở `goc`; trả URL, nguồn (cây git, ảnh gốc, runtime, gói) và hàm dừng. */
export async function chayDichVuToan(goc: string): Promise<DichVuToan> {
  const git = (lenh: string) => execSync('git ' + lenh, { cwd: goc }).toString().trim();
  const docker = (lenh: string) => execSync('docker ' + lenh, { cwd: goc, env: { ...process.env, MSYS_NO_PATHCONV: '1' } }).toString().trim();
  if (git('status --porcelain -- ' + TOAN)) throw new Error(TOAN + ' có thay đổi chưa commit: tệp vàng phải ứng với mã đã commit');
  const cay = git('rev-parse HEAD:' + TOAN);
  // Ảnh gốc trong FROM là tag có thể đổi (python:3.12-slim): kéo về trước khi build để bản build dùng đúng bản cục bộ này,
  // rồi ghi digest bất biến của nó; cùng cây git thì cùng runtime.
  const tu = git('show HEAD:' + TOAN + '/Dockerfile').split('\n').find((d) => /^FROM\s/i.test(d));
  const anhGoc = (tu ?? '').trim().split(/\s+/)[1];
  if (!anhGoc) throw new Error('Không đọc được ảnh gốc trong FROM của ' + TOAN + '/Dockerfile');
  execFileSync('docker', ['pull', '-q', anhGoc]);
  const digest = execFileSync('docker', ['image', 'inspect', '--format', '{{index .RepoDigests 0}}', anhGoc]).toString().trim();
  // Ngữ cảnh build chỉ gồm tệp đã commit của services/math (git archive HEAD), không phải thư mục làm việc: tệp bị
  // .gitignore (khóa *.pem, *apikey*, .env…) không bao giờ vào ảnh, và ảnh đúng cây git ghi ở dưới.
  const nguCanh = execSync('git archive --format=tar HEAD ' + TOAN, { cwd: goc, maxBuffer: 512 * 1024 * 1024 });
  const anh = execSync('docker build -q -f ' + TOAN + '/Dockerfile -', {
    cwd: goc,
    input: nguCanh,
    env: { ...process.env, MSYS_NO_PATHCONV: '1' },
  }).toString().trim();
  // Gói Python đã cài (phiên bản SymPy…) và phiên bản Python của ảnh: cùng với cây git và digest, cố định thư viện, runtime.
  const goi = execFileSync('docker', ['run', '--rm', '-e', 'PIP_NO_CACHE_DIR=1', '--entrypoint', 'pip', anh, 'freeze']).toString()
    .split('\n').map((d) => d.trim()).filter(Boolean).sort();
  const python = execFileSync('docker', ['run', '--rm', '--entrypoint', 'python', anh, '--version']).toString().trim();
  const hop = docker('run -d --rm --read-only --tmpfs /tmp -p 127.0.0.1::8000 ' + anh);
  const dung = () => {
    try {
      docker('rm -f ' + hop);
    } catch {
      // đã dừng
    }
  };
  try {
    const url = 'http://' + docker('port ' + hop + ' 8000/tcp').split('\n')[0];
    for (let i = 0; ; i++) {
      let r: Response | null = null;
      try {
        r = await fetch(url + '/health');
      } catch {
        // chưa sẵn sàng
      }
      if (r?.ok) {
        return {
          url, anh, dung,
          nguon: { cay_git: cay, anh_goc: { ten: anhGoc, digest }, python, goi_python: goi, suc_khoe: await r.json() },
        };
      }
      if (i >= 60) throw new Error('dịch vụ toán không lên sau 60 s');
      await new Promise((xong) => setTimeout(xong, 1000));
    }
  } catch (e) {
    dung();
    throw e;
  }
}

function viTri(src: string, ten: string, moc: string, tu = 0): number {
  const i = src.indexOf(moc, tu);
  if (i < 0) throw new Error(ten + ' thiếu mốc ' + JSON.stringify(moc));
  return i;
}

/** Đoạn của `src` từ mốc `dau` tới hết mốc `cuoi` đầu tiên sau đó. Thiếu mốc thì ném lỗi nêu `ten`. */
export function doan(src: string, ten: string, dau: string, cuoi: string): string {
  const a = viTri(src, ten, dau);
  return src.slice(a, viTri(src, ten, cuoi, a + dau.length) + cuoi.length);
}

/** Đoạn của `src` từ mốc `dau` tới ngay trước mốc `truoc` đầu tiên sau đó (không lấy mốc `truoc`). */
export function giua(src: string, ten: string, dau: string, truoc: string): string {
  const a = viTri(src, ten, dau);
  return src.slice(a, viTri(src, ten, truoc, a + dau.length));
}

/**
 * Blob hay cây HEAD của từng đường dẫn ở `goc`. Dừng nếu đường dẫn nào có thay đổi chưa commit. Đường dẫn so nguyên văn
 * (`--literal-pathspecs`): `[id]` của Next.js là glob với git, khớp nhầm thư mục `i`, `d`.
 */
export function nguonGit(goc: string, duongDan: readonly string[]): Record<string, string> {
  const git = (...lenh: string[]) => execFileSync('git', ['--literal-pathspecs', ...lenh], { cwd: goc }).toString().trim();
  const ban = git('status', '--porcelain', '--', ...duongDan);
  if (ban) throw new Error('Nguồn có thay đổi chưa commit:\n' + ban);
  return Object.fromEntries(duongDan.map((d) => [d, git('rev-parse', 'HEAD:' + d)]));
}
