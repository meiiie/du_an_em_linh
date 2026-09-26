import { createHash, randomBytes, scryptSync, timingSafeEqual } from "crypto";
import { eq } from "drizzle-orm";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { db } from "./db";
import { sessions, userRoles, users } from "./db/schema";

const COOKIE = "hoc_session";

export function hashPassword(password: string) {
  const salt = randomBytes(16).toString("hex");
  const hash = scryptSync(password, salt, 32).toString("hex");
  return `scrypt$${salt}$${hash}`;
}

export function verifyPassword(password: string, stored: string) {
  const [kind, salt, hash] = stored.split("$");
  if (kind !== "scrypt" || !salt || !hash) return false;
  const got = scryptSync(password, salt, 32);
  const exp = Buffer.from(hash, "hex");
  if (got.length !== exp.length) return false;
  return timingSafeEqual(got, exp);
}

function tokenHash(token: string) {
  return createHash("sha256").update(token).digest("hex");
}

export async function createSession(userId: string) {
  const token = randomBytes(32).toString("hex");
  const expires = new Date(Date.now() + 1000 * 60 * 60 * 24 * 14);
  await db.insert(sessions).values({ tokenHash: tokenHash(token), userId, expiresAt: expires });
  const jar = await cookies();
  jar.set(COOKIE, token, { httpOnly: true, sameSite: "lax", path: "/", expires });
}

export async function clearSession() {
  const jar = await cookies();
  const token = jar.get(COOKIE)?.value;
  if (token) await db.delete(sessions).where(eq(sessions.tokenHash, tokenHash(token)));
  jar.delete(COOKIE);
}

export type SessionUser = {
  id: string;
  email: string;
  displayName: string;
  isSynthetic: boolean;
  pseudonymId: string;
  roles: string[];
};

export async function getSession(): Promise<SessionUser | null> {
  const jar = await cookies();
  const token = jar.get(COOKIE)?.value;
  if (!token) return null;
  const row = await db.select().from(sessions).where(eq(sessions.tokenHash, tokenHash(token))).limit(1);
  const s = row[0];
  if (!s || s.expiresAt.getTime() < Date.now()) return null;
  const u = await db.select().from(users).where(eq(users.id, s.userId)).limit(1);
  if (!u[0]) return null;
  const roles = await db.select().from(userRoles).where(eq(userRoles.userId, u[0].id));
  return {
    id: u[0].id,
    email: u[0].email,
    displayName: u[0].displayName,
    isSynthetic: u[0].isSynthetic,
    pseudonymId: u[0].pseudonymId,
    roles: roles.map((r) => r.roleCode),
  };
}

export async function requireUser() {
  const u = await getSession();
  if (!u) redirect("/dang-nhap");
  return u;
}

export async function requireRole(role: "GV" | "HS") {
  const u = await requireUser();
  if (!u.roles.includes(role)) redirect(u.roles.includes("GV") ? "/gv" : "/hs");
  return u;
}
