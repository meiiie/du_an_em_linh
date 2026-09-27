import { drizzle } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import * as schema from "./schema";

const url = process.env.DATABASE_URL || "postgres://hoc_toan:hoc_toan@127.0.0.1:5432/hoc_toan";
const canSsl = /render\.com|neon\.tech|sslmode=require/i.test(url);

const globalForDb = globalThis as unknown as { sql?: ReturnType<typeof postgres> };

export const sql = globalForDb.sql ?? postgres(url, { max: 4, ssl: canSsl ? "require" : undefined });
if (process.env.NODE_ENV !== "production") globalForDb.sql = sql;

export const db = drizzle(sql, { schema });
