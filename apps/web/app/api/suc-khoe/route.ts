export const dynamic = "force-dynamic";

/** Ping giữ thức Render — không đụng DB, không đụng SymPy. */
export async function GET() {
  return Response.json(
    { ok: true, service: "web" },
    { headers: { "cache-control": "no-store" } },
  );
}
