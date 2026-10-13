#!/usr/bin/env node
/**
 * WCAG 2.x (sRGB): đo token từ styles.css, nguồn duy nhất của bảng màu.
 * DESIGN.md là bản đối chiếu: thiếu/lệch token, tỉ lệ chép tay, cặp dưới
 * ngưỡng AA hoặc token chưa được đo đều làm cổng thất bại.
 */
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";

const CHU = 4.5;
const DIEU_KHIEN = 3;

function docBang(md) {
  const bang = {};
  const chepTay = [];
  for (const dong of md.split("\n")) {
    const o = dong.split(" | ");
    if (o.length < 4 || !dong.startsWith("| `")) continue;
    const ten = [...o[0].matchAll(/`([a-z0-9-]+)`/g)].map((m) => m[1]);
    const sang = o[1].match(/#[0-9A-Fa-f]{6}\b/g);
    if (!sang) continue;
    const toi = o[2].includes("như sáng") ? sang : o[2].match(/#[0-9A-Fa-f]{6}\b/g);
    if (!toi) throw new Error(`thiếu màu tối: ${dong}`);
    const nhieu = ten.length > 1;
    if (nhieu && (sang.length !== ten.length || toi.length !== ten.length)) throw new Error(`số tên ≠ số màu: ${dong}`);
    ten.forEach((t, i) => {
      if (bang[t]) throw new Error(`token lặp: ${t}`);
      bang[t] = { light: sang[nhieu ? i : 0], dark: toi[nhieu ? i : 0] };
    });
    for (const m of dong.matchAll(/(\d+(?:,\d+)?)\s*:\s*1\b/g)) {
      if (!["3", "4,5"].includes(m[1])) chepTay.push({ so: m[1], ten: ten.join(", ") });
    }
  }
  return { bang, chepTay };
}

const kenh = (c) => {
  const v = c / 255;
  return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
};
const doChoi = (hex) => {
  const [r, g, b] = [1, 3, 5].map((i) => kenh(parseInt(hex.slice(i, i + 2), 16)));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};
const tiLe = (a, b) => {
  const [x, y] = [doChoi(a), doChoi(b)].sort((p, q) => q - p);
  return (x + 0.05) / (y + 0.05);
};

export function runCheck(cssSource, md, log = console.log) {
  const css = cssSource.replace(/\/\*[\s\S]*?\*\//g, "");
  const bienCss = (dau) => {
    const phan = css.split(dau);
    if (phan.length !== 2) throw new Error(`styles.css cần đúng một khối ${dau}, có ${phan.length - 1}`);
    const khoi = phan[1].slice(0, phan[1].indexOf("}"));
    return Object.fromEntries([...khoi.matchAll(/--([a-z0-9-]+):\s*([^;]+);/g)].map((m) => [m[1], m[2].trim().toUpperCase()]));
  };
  const cssSang = bienCss(":root {");
  // Khối tối chỉ ghi chỗ khác sáng; biến không ghi lại thì kế thừa giá trị sáng.
  const cssToi = { ...cssSang, ...bienCss(":root[data-theme='dark'] {") };
  const { bang, chepTay } = docBang(md);
  const mau = (ten, cheDo) => {
    const t = typeof ten === "string" ? ten : ten[cheDo];
    if (t.startsWith("#")) return t;
    if (!bang[t]) throw new Error(`DESIGN.md không có token ${t}`);
    const value = (cheDo === "light" ? cssSang : cssToi)[t];
    if (!/^#[0-9A-F]{6}$/.test(value ?? "")) throw new Error(`styles.css thiếu màu hex --${t} (${cheDo})`);
    return value;
  };

  const HAI = ["light", "dark"];
  const chu = (fg, bg) => ({ fg, bg, nguong: CHU });
  const dk = (fg, bg) => ({ fg, bg, nguong: DIEU_KHIEN });
  const BE_MAT = ["canvas", "wash", "raise"];
  const BAT_BUOC = [
    ...["ink", "ink-2", "muted", "accent", "label", "pass", "wait", "mark"].flatMap((fg) => BE_MAT.map((bg) => chu(fg, bg))),
    ...["accent", "ink", "ink-2", "muted"].map((fg) => chu(fg, "selection")),
    chu("action-ink", "action"),
    chu("action-hover-ink", "action-hover"),
    chu("board-ink", "board"),
    ...["m-blue", "m-teal", "m-yellow", "m-red", "m-gold", "m-green"].map((fg) => chu(fg, "board")),
    ...BE_MAT.flatMap((bg) => [dk("line-strong", bg), dk("focus", bg)]),
    dk("focus", "selection"),
    ...["level-0", "level-1", "level-2", "level-3"].flatMap((fg) => BE_MAT.map((bg) => chu(fg, bg))),
    dk("board-rule", "board"),
  ];
  const DOI_CHUNG = [
    { ...dk("line", "canvas"), ly: "kẻ chia trang trí" },
    { ...dk("board-line", "board"), ly: "kẻ trang trí trên bảng" },
  ];

  const so = (x) => x.toFixed(2).replace(".", ",");
  const ten = (x) => (typeof x === "string" ? x : `${x.light} | ${x.dark}`);
  let loi = 0;

  log("cặp bắt buộc (sáng / tối, ngưỡng)");
  for (const c of BAT_BUOC) {
    const r = HAI.map((m) => tiLe(mau(c.fg, m), mau(c.bg, m)));
    const dat = r.every((x) => x >= c.nguong);
    if (!dat) loi++;
    log(`  ${dat ? "đạt " : "TRƯỢT"} ${ten(c.fg)} / ${c.bg}: ${r.map(so).join(" / ")} (≥ ${so(c.nguong)})`);
  }

  log("đối chứng: DESIGN.md ghi dưới ngưỡng");
  for (const c of DOI_CHUNG) {
    const cheDo = c.cheDo ?? HAI;
    const r = cheDo.map((m) => tiLe(mau(c.fg, m), mau(c.bg, m)));
    const van = r.every((x) => x < c.nguong);
    if (!van) loi++;
    log(`  ${van ? "dưới" : "ĐÃ ĐẠT, sửa DESIGN.md"} ${ten(c.fg)} / ${c.bg} (${cheDo.join(", ")}): ${r.map(so).join(" / ")} (< ${so(c.nguong)}; ${c.ly})`);
  }

  const coCap = new Set([...BAT_BUOC, ...DOI_CHUNG].flatMap((c) => [c.fg, c.bg]).filter((t) => typeof t === "string"));
  for (const t of Object.keys(bang).filter((t) => !coCap.has(t))) {
    loi++;
    log(`  CHƯA ĐO token ${t}: thêm cặp vào BAT_BUOC hay DOI_CHUNG`);
  }
  for (const [t, v] of Object.entries(bang)) {
    for (const [m, cssMau] of [["light", cssSang], ["dark", cssToi]]) {
      if (cssMau[t] === v[m].toUpperCase()) continue;
      loi++;
      log(`  LỆCH CSS --${t} (${m}): styles.css ${cssMau[t] ?? "không có"}, DESIGN.md ${v[m]}`);
    }
  }
  for (const t of Object.keys(cssSang).filter((t) => /^#[0-9A-F]{6}$/.test(cssSang[t]) && t !== "mask" && !bang[t])) {
    loi++;
    log(`  THIẾU TÀI LIỆU token --${t}: thêm vào DESIGN.md và cặp đo`);
  }
  for (const g of chepTay) {
    loi++;
    log(`  TỈ LỆ CHÉP TAY ${g.so} ở hàng ${g.ten}: ghi ngưỡng («≥ 3 : 1», «dưới 4,5 : 1»), tỉ lệ để script đo`);
  }

  if (loi) {
    log(`${loi} chỗ sai so với DESIGN.md`);
    return 1;
  }
  log(`${Object.keys(bang).length} token đều được đo và khớp styles.css, ${BAT_BUOC.length} cặp bắt buộc, ${DOI_CHUNG.length} đối chứng`);

  return 0;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  process.exitCode = runCheck(readFileSync("apps/frontend/src/styles.css", "utf8"), readFileSync("docs/DESIGN.md", "utf8"));
}
