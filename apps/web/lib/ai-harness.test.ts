import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import {
  laDiaChiLoopback,
  laUrlCloudHopLe,
  luaChonNhaHocSinh,
  parseProvider,
  resolveProvider,
  thongBaoLoiNha,
} from "./ai-catalog";
import { completeChat, probeProvider } from "./ai-harness";

const envKeys = ["LLM_API_KEY", "LLM_BASE_URL", "LLM_MODEL", "OLLAMA_BASE_URL", "LMSTUDIO_BASE_URL"] as const;
const saved: Record<string, string | undefined> = {};
for (const k of envKeys) saved[k] = process.env[k];

afterEach(() => {
  for (const k of envKeys) {
    if (saved[k] === undefined) delete process.env[k];
    else process.env[k] = saved[k];
  }
});

test("parseProvider lạ thì về offline", () => {
  assert.equal(parseProvider("chatgpt"), "offline");
  assert.equal(parseProvider("OLLAMA"), "ollama");
});

test("loopback chỉ 127.0.0.1 / localhost / ::1", () => {
  assert.equal(laDiaChiLoopback("http://127.0.0.1:11434/v1"), true);
  assert.equal(laDiaChiLoopback("http://localhost:1234/v1"), true);
  assert.equal(laDiaChiLoopback("http://[::1]:11434/v1"), true);
  assert.equal(laDiaChiLoopback("http://10.0.0.8:11434/v1"), false);
  assert.equal(laDiaChiLoopback("http://192.168.1.10:11434/v1"), false);
  assert.equal(laDiaChiLoopback("http://169.254.169.254/v1"), false);
  assert.equal(laDiaChiLoopback("https://api.openai.com/v1"), false);
  assert.equal(laDiaChiLoopback("not-a-url"), false);
});

test("cloud URL: https công cộng hoặc http loopback", () => {
  assert.equal(laUrlCloudHopLe("https://api.openai.com/v1"), true);
  assert.equal(laUrlCloudHopLe("http://127.0.0.1:8080/v1"), true);
  assert.equal(laUrlCloudHopLe("http://10.0.0.3/v1"), false);
  assert.equal(laUrlCloudHopLe("http://api.openai.com/v1"), false);
});

test("resolveProvider: offline luôn thắng; local cần cửa lớp; cloud không tự bật", () => {
  assert.equal(resolveProvider({ classProvider: "cloud", sessionProvider: "offline" }), "offline");
  assert.equal(resolveProvider({ classProvider: "offline", sessionProvider: "ollama", allowLocal: true }), "ollama");
  assert.equal(resolveProvider({ classProvider: "offline", sessionProvider: "ollama", allowLocal: false }), "offline");
  assert.equal(resolveProvider({ classProvider: "offline", sessionProvider: "cloud" }), "offline");
  assert.equal(resolveProvider({ classProvider: "cloud", sessionProvider: "cloud" }), "cloud");
});

test("học sinh thấy ChatGPT của lớp khi lớp bật cloud", () => {
  const rows = luaChonNhaHocSinh({
    classProvider: "cloud",
    classModel: null,
    allowLocal: false,
    cloudReady: true,
  });
  assert.equal(rows.find((r) => r.id === "cloud")?.ten, "ChatGPT của lớp");
  assert.equal(rows.some((r) => r.id === "ollama"), false);
});

test("học sinh không thấy cloud khi lớp không bật", () => {
  const rows = luaChonNhaHocSinh({
    classProvider: "offline",
    classModel: null,
    allowLocal: true,
    cloudReady: true,
  });
  assert.equal(rows.some((r) => r.id === "cloud"), false);
  assert.equal(rows.some((r) => r.id === "ollama"), true);
});

test("thông báo lỗi luôn nói không chuyển nhà", () => {
  for (const kind of ["no_key", "timeout", "http", "network"] as const) {
    assert.match(thongBaoLoiNha(kind, "ollama"), /[Kk]hông (chuyển|gửi lại)/);
  }
});

test("offline không gọi fetch", async () => {
  let calls = 0;
  const fake: typeof fetch = async () => {
    calls += 1;
    throw new Error("không được gọi");
  };
  const r = await completeChat({
    provider: "offline",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "Gợi ý mức 1",
    fetchFn: fake,
  });
  assert.equal(calls, 0);
  assert.equal(r.text, "Gợi ý mức 1");
  assert.equal(r.error, null);
  assert.equal(r.provider, "offline");
});

test("cloud thiếu khóa: lỗi rõ, không lấy offlineText", async () => {
  delete process.env.LLM_API_KEY;
  const r = await completeChat({
    provider: "cloud",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "BÍ MẬT GỢI Ý",
    fetchFn: async () => {
      throw new Error("không được gọi");
    },
  });
  assert.equal(r.errorKind, "no_key");
  assert.equal(r.text.includes("BÍ MẬT GỢI Ý"), false);
  assert.match(r.text, /Không chuyển/);
});

test("local không loopback: từ chối, không fetch", async () => {
  process.env.OLLAMA_BASE_URL = "http://10.0.0.8:11434/v1";
  let calls = 0;
  const r = await completeChat({
    provider: "ollama",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    fetchFn: async () => {
      calls += 1;
      throw new Error("no");
    },
  });
  assert.equal(calls, 0);
  assert.equal(r.errorKind, "not_loopback");
  assert.equal(r.text.includes("GỢI Ý"), false);
});

test("HTTP lỗi: đúng một lần, không fallback text gợi ý", async () => {
  process.env.OLLAMA_BASE_URL = "http://127.0.0.1:11434/v1";
  let calls = 0;
  const r = await completeChat({
    provider: "ollama",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    fetchFn: async () => {
      calls += 1;
      return new Response("nope", { status: 503 });
    },
  });
  assert.equal(calls, 1);
  assert.equal(r.errorKind, "http");
  assert.match(r.text, /503/);
  assert.equal(r.text.includes("GỢI Ý"), false);
});

test("thành công: lấy content, đánh dấu online", async () => {
  process.env.OLLAMA_BASE_URL = "http://127.0.0.1:11434/v1";
  const r = await completeChat({
    provider: "ollama",
    model: "llama3.2",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    fetchFn: async () =>
      new Response(JSON.stringify({ choices: [{ message: { content: "Em viết lại y′." } }] }), {
        status: 200,
        headers: { "content-type": "application/json" },
      }),
  });
  assert.equal(r.text, "Em viết lại y′.");
  assert.equal(r.offline, false);
  assert.equal(r.error, null);
});

test("probe GET /models một lần, không POST chat", async () => {
  process.env.OLLAMA_BASE_URL = "http://127.0.0.1:11434/v1";
  const methods: string[] = [];
  const r = await probeProvider({
    provider: "ollama",
    fetchFn: async (input, init) => {
      methods.push(String(init?.method || "GET"));
      assert.match(String(input), /\/models$/);
      return new Response(JSON.stringify({ data: [{ id: "llama3.2" }] }), { status: 200 });
    },
  });
  assert.deepEqual(methods, ["GET"]);
  assert.equal(r.ok, true);
  assert.deepEqual(r.models, ["llama3.2"]);
});
