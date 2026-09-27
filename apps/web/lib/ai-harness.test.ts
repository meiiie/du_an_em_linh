import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import {
  AI_MAX_TOKENS_CHAT,
  OPENROUTER_MAC_DINH,
  ZAI_MAC_DINH,
  laDiaChiLoopback,
  laNhaKhoa,
  laUrlCloudHopLe,
  luaChonNhaHocSinh,
  parseProvider,
  resolveProvider,
  thongBaoLoiNha,
} from "./ai-catalog";
import { cauHinhCongKhai, completeChat, docBaseNhaKhoa, docKhoaNha, docModel, probeProvider } from "./ai-harness";

const envKeys = [
  "LLM_API_KEY",
  "LLM_BASE_URL",
  "LLM_MODEL",
  "OLLAMA_BASE_URL",
  "LMSTUDIO_BASE_URL",
  "OPENROUTER_API_KEY",
  "OPENROUTER_MODEL",
  "ZAI_API_KEY",
  "ZAI_MODEL",
] as const;
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
  assert.equal(parseProvider("openrouter"), "openrouter");
  assert.equal(parseProvider("zai"), "zai");
  assert.equal(laNhaKhoa("openrouter"), true);
  assert.equal(laNhaKhoa("zai"), true);
  assert.equal(laNhaKhoa("offline"), false);
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
  assert.equal(laUrlCloudHopLe("https://openrouter.ai/api/v1"), true);
  assert.equal(laUrlCloudHopLe("https://api.z.ai/api/coding/paas/v4"), true);
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
  assert.equal(resolveProvider({ classProvider: "openrouter", sessionProvider: "openrouter" }), "openrouter");
  assert.equal(resolveProvider({ classProvider: "offline", sessionProvider: "openrouter" }), "offline");
  assert.equal(resolveProvider({ classProvider: "openrouter", sessionProvider: "zai" }), "openrouter");
  assert.equal(resolveProvider({ classProvider: "zai", sessionProvider: "zai" }), "zai");
});

test("học sinh thấy ChatGPT của lớp khi lớp bật cloud", () => {
  const rows = luaChonNhaHocSinh({
    classProvider: "cloud",
    classModel: null,
    allowLocal: false,
    cloudReady: true,
    openrouterReady: false,
    zaiReady: false,
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
    openrouterReady: true,
    zaiReady: true,
  });
  assert.equal(rows.some((r) => r.id === "cloud"), false);
  assert.equal(rows.some((r) => r.id === "openrouter"), false);
  assert.equal(rows.some((r) => r.id === "zai"), false);
  assert.equal(rows.some((r) => r.id === "ollama"), true);
});

test("học sinh thấy OpenRouter / Z.AI của lớp khi lớp bật đúng nhà", () => {
  const or = luaChonNhaHocSinh({
    classProvider: "openrouter",
    classModel: null,
    allowLocal: false,
    cloudReady: true,
    openrouterReady: true,
    zaiReady: false,
  });
  assert.equal(or.find((r) => r.id === "openrouter")?.ten, "OpenRouter của lớp");
  assert.equal(or.some((r) => r.id === "cloud"), false);
  const z = luaChonNhaHocSinh({
    classProvider: "zai",
    classModel: null,
    allowLocal: false,
    cloudReady: false,
    openrouterReady: false,
    zaiReady: false,
  });
  assert.equal(z.find((r) => r.id === "zai")?.ten, "Z.AI của lớp — chưa có khóa");
  assert.equal(z.find((r) => r.id === "zai")?.disabled, true);
});

test("thông báo lỗi luôn nói không chuyển nhà", () => {
  for (const kind of ["no_key", "timeout", "http", "network"] as const) {
    assert.match(thongBaoLoiNha(kind, "ollama"), /[Kk]hông (chuyển|gửi lại)/);
  }
});

test("Dừng giữa chừng: aborted, không gọi xong", async () => {
  process.env.OLLAMA_BASE_URL = "http://127.0.0.1:11434/v1";
  const ac = new AbortController();
  ac.abort();
  let calls = 0;
  const r = await completeChat({
    provider: "ollama",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    signal: ac.signal,
    fetchFn: async () => {
      calls += 1;
      throw new Error("không được gọi");
    },
  });
  assert.equal(calls, 0);
  assert.equal(r.errorKind, "aborted");
  assert.equal(r.text.includes("GỢI Ý"), false);
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

test("content mảng vẫn lấy được chữ", async () => {
  process.env.OLLAMA_BASE_URL = "http://127.0.0.1:11434/v1";
  const r = await completeChat({
    provider: "ollama",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    fetchFn: async () =>
      new Response(JSON.stringify({ choices: [{ message: { content: [{ type: "text", text: "Em viết y′." }] } }] }), {
        status: 200,
        headers: { "content-type": "application/json" },
      }),
  });
  assert.equal(r.text, "Em viết y′.");
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

test("OpenRouter / Z.AI thiếu khóa: lỗi rõ, không lấy offlineText", async () => {
  delete process.env.OPENROUTER_API_KEY;
  delete process.env.ZAI_API_KEY;
  for (const nha of ["openrouter", "zai"] as const) {
    const r = await completeChat({
      provider: nha,
      messages: [{ role: "user", content: "hi" }],
      offlineText: "BÍ MẬT GỢI Ý",
      fetchFn: async () => {
        throw new Error("không được gọi");
      },
    });
    assert.equal(r.errorKind, "no_key");
    assert.equal(r.text.includes("BÍ MẬT GỢI Ý"), false);
    assert.match(r.text, /Không chuyển/);
  }
});

test("OpenRouter gọi đúng địa chỉ cứng, không lấy LLM_BASE_URL", async () => {
  process.env.OPENROUTER_API_KEY = "sk-or-test";
  process.env.LLM_BASE_URL = "https://evil.example/v1";
  let url = "";
  let model = "";
  const r = await completeChat({
    provider: "openrouter",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    fetchFn: async (input, init) => {
      url = String(input);
      const body = JSON.parse(String(init?.body || "{}")) as { model?: string };
      model = body.model || "";
      return new Response(JSON.stringify({ choices: [{ message: { content: "Em viết lại y′." } }] }), {
        status: 200,
        headers: { "content-type": "application/json" },
      });
    },
  });
  assert.equal(url, `${OPENROUTER_MAC_DINH}/chat/completions`);
  assert.equal(model, "qwen/qwen3-coder");
  assert.equal(r.text, "Em viết lại y′.");
  assert.equal(r.offline, false);
  assert.equal(r.provider, "openrouter");
});

test("Z.AI dùng endpoint coding, không API chat tiêu dùng", async () => {
  process.env.ZAI_API_KEY = "zai-test";
  process.env.LLM_BASE_URL = "https://api.z.ai/api/paas/v4";
  let url = "";
  let model = "";
  const r = await completeChat({
    provider: "zai",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    fetchFn: async (input, init) => {
      url = String(input);
      const body = JSON.parse(String(init?.body || "{}")) as { model?: string };
      model = body.model || "";
      return new Response(JSON.stringify({ choices: [{ message: { content: "Em xét dấu y′." } }] }), {
        status: 200,
        headers: { "content-type": "application/json" },
      });
    },
  });
  assert.equal(url, `${ZAI_MAC_DINH}/chat/completions`);
  assert.equal(url.includes("/api/coding/paas/v4/"), true);
  assert.equal(url.includes("/api/paas/v4/chat"), false);
  assert.equal(model, "glm-5.3-flashx");
  assert.equal(r.provider, "zai");
  assert.equal(docBaseNhaKhoa("zai"), ZAI_MAC_DINH);
  assert.equal(docModel("zai"), "glm-5.3-flashx");
  assert.equal(docModel("openrouter"), "qwen/qwen3-coder");
});

test("khóa lớp Z.AI không làm sẵn cloud/OpenRouter", () => {
  delete process.env.LLM_API_KEY;
  delete process.env.OPENROUTER_API_KEY;
  delete process.env.ZAI_API_KEY;
  assert.equal(docKhoaNha("zai", "khoa-zai", "zai"), "khoa-zai");
  assert.equal(docKhoaNha("cloud", "khoa-zai", "zai"), null);
  assert.equal(docKhoaNha("openrouter", "khoa-zai", "zai"), null);
  assert.equal(docKhoaNha("zai", "khoa-zai"), "khoa-zai");
  const cfg = cauHinhCongKhai({ classProvider: "zai", classApiKey: "khoa-zai" });
  assert.equal(cfg.classProvider, "zai");
  assert.equal(cfg.zaiReady, true);
  assert.equal(cfg.cloudReady, false);
  assert.equal(cfg.openrouterReady, false);
  const khongNha = cauHinhCongKhai({ classApiKey: "khoa-zai" });
  assert.equal(khongNha.classProvider, "offline");
  assert.equal(khongNha.zaiReady, false);
  assert.equal(khongNha.cloudReady, false);
});

test("completeChat cloud không gửi khóa Z.AI của lớp", async () => {
  delete process.env.LLM_API_KEY;
  let calls = 0;
  const r = await completeChat({
    provider: "cloud",
    classApiKey: "khoa-zai",
    classProvider: "zai",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "BÍ MẬT GỢI Ý",
    fetchFn: async () => {
      calls += 1;
      throw new Error("không được gọi");
    },
  });
  assert.equal(calls, 0);
  assert.equal(r.errorKind, "no_key");
  assert.equal(r.text.includes("BÍ MẬT GỢI Ý"), false);
});

test("probe cloud không lấy khóa Z.AI của lớp", async () => {
  delete process.env.LLM_API_KEY;
  let calls = 0;
  const r = await probeProvider({
    provider: "cloud",
    classApiKey: "khoa-zai",
    classProvider: "zai",
    fetchFn: async () => {
      calls += 1;
      throw new Error("không được gọi");
    },
  });
  assert.equal(calls, 0);
  assert.equal(r.ok, false);
  assert.match(r.message, /Chưa có khóa/);
});

test("Z.AI gửi thinking + 1600 token, không đọc reasoning_content", async () => {
  process.env.ZAI_API_KEY = "zai-test";
  let body: { model?: string; max_tokens?: number; thinking?: { type?: string }; stream?: boolean } = {};
  const r = await completeChat({
    provider: "zai",
    messages: [{ role: "user", content: "hi" }],
    offlineText: "GỢI Ý",
    fetchFn: async (_input, init) => {
      body = JSON.parse(String(init?.body || "{}")) as typeof body;
      return new Response(
        JSON.stringify({ choices: [{ message: { content: "", reasoning_content: "BÍ MẬT SUY LUẬN y′=0" } }] }),
        { status: 200, headers: { "content-type": "application/json" } },
      );
    },
  });
  assert.equal(body.model, "glm-5.3-flashx");
  assert.equal(body.max_tokens, AI_MAX_TOKENS_CHAT);
  assert.equal(body.max_tokens, 1600);
  assert.equal(body.stream, false);
  assert.deepEqual(body.thinking, { type: "enabled" });
  assert.equal(r.errorKind, "empty");
  assert.equal(r.text.includes("BÍ MẬT"), false);
  assert.equal(r.text.includes("y′=0"), false);
});

test("thông báo hết giờ nói 30 giây", () => {
  assert.match(thongBaoLoiNha("timeout", "zai"), /30 giây/);
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
