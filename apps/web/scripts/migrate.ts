import { readFileSync } from "fs";
import path from "path";
import { sql } from "../lib/db";

async function main() {
  const found = await sql<{ name: string | null }[]>`select to_regclass('public.users') as name`;
  if (found[0]?.name) {
    console.log("Đã có bảng users — bỏ qua 0001_init.sql");
    await sql.end();
    return;
  }
  const file = path.join(process.cwd(), "drizzle", "0001_init.sql");
  const raw = readFileSync(file, "utf8");
  const stripped = raw
    .split("\n")
    .filter((line) => !line.trim().startsWith("--"))
    .join("\n");
  const statements = stripped
    .split(";")
    .map((s) => s.trim())
    .filter(Boolean);
  for (const statement of statements) {
    await sql.unsafe(statement);
  }
  console.log(`Đã chạy ${statements.length} câu trong 0001_init.sql`);
  await sql.end();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
