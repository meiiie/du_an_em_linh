import { readdirSync, readFileSync } from "fs";
import path from "path";
import { sql } from "../lib/db";

async function runSql(raw: string) {
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
  return statements.length;
}

async function main() {
  await sql.unsafe(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      id text PRIMARY KEY,
      applied_at timestamptz NOT NULL DEFAULT now()
    )
  `);

  const dir = path.join(process.cwd(), "drizzle");
  const files = readdirSync(dir)
    .filter((f) => f.endsWith(".sql"))
    .sort();

  for (const file of files) {
    const already = await sql<{ id: string }[]>`select id from schema_migrations where id = ${file}`;
    if (already[0]) {
      console.log(`Đã có ${file} — bỏ qua`);
      continue;
    }

    if (file === "0001_init.sql") {
      const found = await sql<{ name: string | null }[]>`select to_regclass('public.users') as name`;
      if (found[0]?.name) {
        await sql`insert into schema_migrations (id) values (${file}) on conflict do nothing`;
        console.log("Đã có bảng users — đánh dấu 0001_init.sql");
        continue;
      }
    }

    const n = await runSql(readFileSync(path.join(dir, file), "utf8"));
    await sql`insert into schema_migrations (id) values (${file}) on conflict do nothing`;
    console.log(`Đã chạy ${file} (${n} câu)`);
  }

  await sql.end();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
