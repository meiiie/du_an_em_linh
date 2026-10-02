#!/usr/bin/env node
/** Một sản phẩm = một SemVer. Lệch file → exit 1. */
import { readFileSync } from "node:fs";

function doc(path) {
  return readFileSync(path, "utf8");
}

const root = JSON.parse(doc("package.json")).version;
const web = JSON.parse(doc("apps/web/package.json")).version;
const frontend = JSON.parse(doc("apps/frontend/package.json")).version;
const py = /(?<=^version = ")[^"]+/m.exec(doc("services/math/pyproject.toml"))?.[0];
const core = /<version>([^<]+)<\/version>\s*<!-- x-release-please-version -->/.exec(doc("services/core/pom.xml"))?.[1];
const cit = /(?<=^version: ")[^"]+/m.exec(doc("CITATION.cff"))?.[0];
const man = JSON.parse(doc(".release-please-manifest.json"))["."];

const bang = { "package.json": root, "apps/web/package.json": web, "apps/frontend/package.json": frontend, "pyproject.toml": py, "services/core/pom.xml": core, "CITATION.cff": cit, "release-please": man };
const lech = Object.entries(bang).filter(([, v]) => v !== root);

if (!root || !/^\d+\.\d+\.\d+$/.test(root) || lech.length) {
  console.error("Phiên bản lệch hoặc không SemVer:", bang);
  process.exit(1);
}

console.log(`phiên bản ${root}`);
