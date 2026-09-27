/** SSE trạng thái — nhịp Wiii / Open WebUI: event rồi câu đủ. Không xả token. */

export type GiaSuBuocSse = "kho" | "goi" | "loc";

export type SseKhung = { event: string; data: string };

export function vietSse(event: string, data: unknown): string {
  return `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
}

export function gomSse(raw: string): { events: SseKhung[]; leftover: string } {
  const parts = raw.split("\n\n");
  const leftover = parts.pop() ?? "";
  const events: SseKhung[] = [];
  for (const block of parts) {
    let event = "message";
    const data: string[] = [];
    for (const line of block.split("\n")) {
      if (line.startsWith("event:")) event = line.slice(6).trim();
      else if (line.startsWith("data:")) data.push(line.slice(5).replace(/^\s/, ""));
    }
    if (data.length) events.push({ event, data: data.join("\n") });
  }
  return { events, leftover };
}

export function chuTrangThaiGiaSu(buoc: GiaSuBuocSse | null): string {
  if (buoc === "kho") return "Đang mở công thức lớp…";
  if (buoc === "goi") return "Đang hỏi gia sư…";
  if (buoc === "loc") return "Đang lọc khỏi đáp án…";
  return "Đang nghĩ…";
}
