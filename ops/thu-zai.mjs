// Gọi thử Z.AI Coding Plan (endpoint OpenAI-compatible của v0) bằng khóa đã nạp từ Docker secret. Không in khóa.
// Chạy trong container web: docker compose exec web docker-entrypoint.sh node /dev/stdin < ops/thu-zai.mjs
const key = process.env.ZAI_API_KEY;
const model = process.env.ZAI_MODEL || "glm-5.3";
if (!key) {
  console.log("Không có ZAI_API_KEY: gia sư chạy nhà offline.");
  process.exit(1);
}
const t0 = Date.now();
const res = await fetch("https://api.z.ai/api/coding/paas/v4/chat/completions", {
  method: "POST",
  headers: { "content-type": "application/json", authorization: `Bearer ${key}` },
  body: JSON.stringify({
    model,
    max_tokens: 1600,
    messages: [{ role: "user", content: "Trả lời đúng một câu ngắn bằng tiếng Việt: đạo hàm dùng để làm gì?" }],
  }),
});
const body = await res.json().catch(() => ({}));
const text = body?.choices?.[0]?.message?.content ?? "";
console.log(JSON.stringify({ http: res.status, model: body?.model ?? model, ms: Date.now() - t0, tra_loi: text.slice(0, 200), loi: body?.error?.message }));
process.exit(res.ok && text ? 0 : 1);
