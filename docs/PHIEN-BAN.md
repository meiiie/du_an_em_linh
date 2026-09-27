# Phiên bản

Một sản phẩm, **một** số SemVer (`MAJOR.MINOR.PATCH`). `0.y.z` = nguyên mẫu; breaking trong `0.x` tăng MINOR (`bump-minor-pre-major`).

| Nguồn | Việc |
| --- | --- |
| `package.json` | Số chuẩn (root) |
| `apps/web/package.json` | Cùng số |
| `services/math/pyproject.toml` | Cùng số |
| `CITATION.cff` | Cùng số khi trích dẫn NCKH |
| `.release-please-manifest.json` | Số đã phát hành gần nhất |
| `CHANGELOG.md` | Ghi chú cho người đọc |
| Tag `vX.Y.Z` + GitHub Release | Bản đóng gói |

Không tự sửa số tay trên nhánh việc. `pnpm test:version` bắt lệch.

## Conventional Commits

Tiêu đề commit / squash PR (như Angular, Next.js, repo Google):

| Tiền tố | SemVer |
| --- | --- |
| `feat:` | MINOR |
| `fix:` | PATCH |
| `feat!:` / `BREAKING CHANGE:` | MINOR khi `0.x`, MAJOR từ `1.0.0` |
| `docs:`, `chore:`, `ci:`, `test:` | không tăng số (vào changelog nếu đáng) |

Ví dụ: `feat: gợi ý lịch theo kỹ năng yếu`.

## Phát hành

[release-please](https://github.com/googleapis/release-please) (Google) chạy trên `main` (`.github/workflows/phat-hanh.yml`):

1. Gom commit Conventional → mở PR «chore: release X.Y.Z» (đổi số + CHANGELOG).
2. Gộp PR đó → tag `vX.Y.Z` + GitHub Release.

**Lần đầu (0.1.0):** sau khi gộp nguyên mẫu vào `main`, tạo Release `v0.1.0` trên GitHub (hoặc `git tag v0.1.0 && git push origin v0.1.0`). Các bản sau để release-please lo.

Cần Settings → Actions → General → **Allow GitHub Actions to create and approve pull requests**.
