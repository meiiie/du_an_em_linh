#!/usr/bin/env node
/**
 * Tương phản WCAG 2.x (độ chói sRGB) của các cặp token trong bảng màu `docs/DESIGN.md`. Đọc thẳng bảng, nên đổi màu
 * ở đó là đo lại. Exit 1 khi:
 * - cặp bắt buộc dưới ngưỡng;
 * - cặp đối chứng (chỗ DESIGN.md ghi «dưới ngưỡng, không dùng») lại đạt: lời ghi đã sai;
 * - token trong bảng không nằm trong cặp nào: màu mới chưa được đo;
 * - bảng chép tay một tỉ lệ (dạng «4,81 : 1»): bảng chỉ ghi ngưỡng «3 : 1» / «4,5 : 1», tỉ lệ là kết quả của script này.
 * - `apps/frontend/src/styles.css` lệch bảng: mỗi token phải là biến `--tên` cùng giá trị sáng / tối.
 * Script đọc màu từ bảng, không đọc câu chữ: thêm hay đổi một câu ngưỡng trong DESIGN.md thì thêm cặp tương ứng vào
 * BAT_BUOC (phải đạt) hay DOI_CHUNG (ghi «dưới ngưỡng») ở dưới.
 */
import { readFileSync } from "node:fs";

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

const { bang, chepTay } = docBang(readFileSync("docs/DESIGN.md", "utf8"));
const mau = (ten, cheDo) => {
  const t = typeof ten === "string" ? ten : ten[cheDo];
  if (t.startsWith("#")) return t;
  if (!bang[t]) throw new Error(`DESIGN.md không có token ${t}`);
  return bang[t][cheDo];
};

const HAI = ["light", "dark"];
const chu = (fg, bg) => ({ fg, bg, nguong: CHU });
const dk = (fg, bg) => ({ fg, bg, nguong: DIEU_KHIEN });
const BAT_BUOC = [
  ...["ink", "ink-2", "muted"].flatMap((fg) => ["canvas", "wash", "raise"].map((bg) => chu(fg, bg))),
  chu("label", "canvas"),
  chu("label", "wash"),
  chu("pass", "canvas"),
  chu("wait", "canvas"),
  chu("mark", "canvas"),
  chu("mark", "wash"),
  dk("accent", "canvas"),
  dk("accent", "wash"),
  chu("#FFFFFF", "action"),
  chu({ light: "#FFFFFF", dark: "#141413" }, "action-hover"),
  chu("board-ink", "board"),
  ...["m-blue", "m-teal", "m-yellow", "m-red", "m-gold", "m-green"].map((fg) => chu(fg, "board")),
  ...["canvas", "wash", "raise"].flatMap((bg) => [dk("line-strong", bg), dk("focus", bg)]),
  dk("board-rule", "board"),
];
const DOI_CHUNG = [
  { ...dk("line", "canvas"), ly: "kẻ chia trang trí" },
  { ...dk("board-line", "board"), ly: "kẻ trang trí trên bảng" },
  { ...dk("accent", "raise"), cheDo: ["dark"], ly: "vì thế có token focus" },
  { ...chu("mark", "raise"), cheDo: ["dark"], ly: "chữ mark không trên raise tối" },
  { ...chu("#FFFFFF", "accent"), ly: "vì thế nền nút là action" },
  { ...chu("#FFFFFF", "action-hover"), cheDo: ["dark"], ly: "vì thế chữ nút khi trỏ ở tối là mực" },
];

const so = (x) => x.toFixed(2).replace(".", ",");
const ten = (x) => (typeof x === "string" ? x : `${x.light} | ${x.dark}`);
let loi = 0;

console.log("cặp bắt buộc (sáng / tối, ngưỡng)");
for (const c of BAT_BUOC) {
  const r = HAI.map((m) => tiLe(mau(c.fg, m), mau(c.bg, m)));
  const dat = r.every((x) => x >= c.nguong);
  if (!dat) loi++;
  console.log(`  ${dat ? "đạt " : "TRƯỢT"} ${ten(c.fg)} / ${c.bg}: ${r.map(so).join(" / ")} (≥ ${so(c.nguong)})`);
}

console.log("đối chứng: DESIGN.md ghi dưới ngưỡng");
for (const c of DOI_CHUNG) {
  const cheDo = c.cheDo ?? HAI;
  const r = cheDo.map((m) => tiLe(mau(c.fg, m), mau(c.bg, m)));
  const van = r.every((x) => x < c.nguong);
  if (!van) loi++;
  console.log(`  ${van ? "dưới" : "ĐÃ ĐẠT, sửa DESIGN.md"} ${ten(c.fg)} / ${c.bg} (${cheDo.join(", ")}): ${r.map(so).join(" / ")} (< ${so(c.nguong)}; ${c.ly})`);
}

const coCap = new Set([...BAT_BUOC, ...DOI_CHUNG].flatMap((c) => [c.fg, c.bg]).filter((t) => typeof t === "string"));
for (const t of Object.keys(bang).filter((t) => !coCap.has(t))) {
  loi++;
  console.log(`  CHƯA ĐO token ${t}: thêm cặp vào BAT_BUOC hay DOI_CHUNG`);
}
const css = readFileSync("apps/frontend/src/styles.css", "utf8");
const bienCss = (dau) => {
  const i = css.indexOf(dau);
  if (i < 0) throw new Error(`styles.css không có khối ${dau}`);
  const khoi = css.slice(i, css.indexOf("}", i));
  return Object.fromEntries([...khoi.matchAll(/--([a-z0-9-]+):\s*(#[0-9A-Fa-f]{6})\s*;/g)].map((m) => [m[1], m[2].toUpperCase()]));
};
const cssSang = bienCss(":root {");
// Khối tối chỉ ghi chỗ khác sáng; biến không ghi lại thì kế thừa giá trị sáng.
const cssToi = { ...cssSang, ...bienCss(":root[data-theme='dark'] {") };
for (const [t, v] of Object.entries(bang)) {
  for (const [m, cssMau] of [["light", cssSang], ["dark", cssToi]]) {
    if (cssMau[t] === v[m].toUpperCase()) continue;
    loi++;
    console.log(`  LỆCH CSS --${t} (${m}): styles.css ${cssMau[t] ?? "không có"}, DESIGN.md ${v[m]}`);
  }
}
for (const g of chepTay) {
  loi++;
  console.log(`  TỈ LỆ CHÉP TAY ${g.so} ở hàng ${g.ten}: ghi ngưỡng («≥ 3 : 1», «dưới 4,5 : 1»), tỉ lệ để script đo`);
}

if (loi) {
  console.error(`${loi} chỗ sai so với DESIGN.md`);
  process.exit(1);
}
console.log(`${Object.keys(bang).length} token đều được đo và khớp styles.css, ${BAT_BUOC.length} cặp bắt buộc, ${DOI_CHUNG.length} đối chứng`);
