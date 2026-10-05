#!/usr/bin/env node
/**
 * Tương phản WCAG 2.x (độ chói sRGB) của các cặp token trong bảng màu `docs/DESIGN.md`. Đọc thẳng bảng, nên đổi màu
 * ở đó là đo lại. Exit 1 khi:
 * - cặp bắt buộc dưới ngưỡng;
 * - cặp đối chứng (chỗ DESIGN.md ghi «dưới ngưỡng, không dùng») lại đạt: lời ghi đã sai;
 * - token trong bảng không nằm trong cặp nào: màu mới chưa được đo;
 * - một tỉ lệ ghi trong bảng (dạng 4,81) không phải kết quả của đúng cặp nó nói tới: số trong bảng đã sai hay cũ.
 */
import { readFileSync } from "node:fs";

const CHU = 4.5;
const DIEU_KHIEN = 3;

function docBang(md) {
  const bang = {};
  const soGhi = [];
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
    // Số thuộc màu gần nhất đứng trước nó trong cùng ô («`#C75B39` với chữ trắng chỉ 4,21»), không có thì thuộc token
    // của hàng. Cột sáng chỉ khớp đo sáng, cột tối chỉ khớp đo tối.
    [["light"], ["dark"], ["light", "dark"]].forEach((cheDo, k) => {
      let chuThe = null;
      for (const m of o[k + 1].matchAll(/#[0-9A-Fa-f]{6}\b|\d+,\d\d/g)) {
        if (m[0].startsWith("#")) chuThe = m[0].toUpperCase();
        else soGhi.push({ so: m[0], ten, chuThe, cheDo });
      }
    });
  }
  return { bang, soGhi };
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

const { bang, soGhi } = docBang(readFileSync("docs/DESIGN.md", "utf8"));
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
const daDo = [];
const ghi = (c, m, x) => daDo.push({ so: so(x), m, mau: [mau(c.fg, m), mau(c.bg, m)].map((h) => h.toUpperCase()) });
let loi = 0;

console.log("cặp bắt buộc (sáng / tối, ngưỡng)");
for (const c of BAT_BUOC) {
  const r = HAI.map((m) => tiLe(mau(c.fg, m), mau(c.bg, m)));
  HAI.forEach((m, i) => ghi(c, m, r[i]));
  const dat = r.every((x) => x >= c.nguong);
  if (!dat) loi++;
  console.log(`  ${dat ? "đạt " : "TRƯỢT"} ${ten(c.fg)} / ${c.bg}: ${r.map(so).join(" / ")} (≥ ${so(c.nguong)})`);
}

console.log("đối chứng: DESIGN.md ghi dưới ngưỡng");
for (const c of DOI_CHUNG) {
  const cheDo = c.cheDo ?? HAI;
  const r = cheDo.map((m) => tiLe(mau(c.fg, m), mau(c.bg, m)));
  cheDo.forEach((m, i) => ghi(c, m, r[i]));
  const van = r.every((x) => x < c.nguong);
  if (!van) loi++;
  console.log(`  ${van ? "dưới" : "ĐÃ ĐẠT, sửa DESIGN.md"} ${ten(c.fg)} / ${c.bg} (${cheDo.join(", ")}): ${r.map(so).join(" / ")} (< ${so(c.nguong)}; ${c.ly})`);
}

const coCap = new Set([...BAT_BUOC, ...DOI_CHUNG].flatMap((c) => [c.fg, c.bg]).filter((t) => typeof t === "string"));
for (const t of Object.keys(bang).filter((t) => !coCap.has(t))) {
  loi++;
  console.log(`  CHƯA ĐO token ${t}: thêm cặp vào BAT_BUOC hay DOI_CHUNG`);
}
const khop = ({ so: s, ten: t, chuThe, cheDo }) =>
  daDo.some((d) => d.so === s && cheDo.includes(d.m) && (chuThe ? [chuThe] : t.map((x) => bang[x][d.m].toUpperCase())).some((h) => d.mau.includes(h)));
for (const g of soGhi.filter((g) => !khop(g))) {
  loi++;
  console.log(`  SỐ SAI ${g.so} ở hàng ${g.ten.join(", ")} (${g.chuThe ?? "màu của hàng"}, ${g.cheDo.join(", ")}): không cặp nào của màu đó ra số này`);
}

if (loi) {
  console.error(`${loi} chỗ sai so với DESIGN.md`);
  process.exit(1);
}
console.log(
  `${Object.keys(bang).length} token, ${BAT_BUOC.length} cặp bắt buộc, ${DOI_CHUNG.length} đối chứng, ${soGhi.length} số ghi trong bảng đều khớp`,
);
