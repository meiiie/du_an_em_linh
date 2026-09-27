import { ImageResponse } from "next/og";
import { SITE_NAME } from "@/lib/site";

export const alt = SITE_NAME;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OgImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background: "#FFFFFF",
          color: "#17181C",
          padding: 64,
          border: "16px solid #17181C",
        }}
      >
        <div style={{ display: "flex", fontSize: 22, color: "#5C5F66" }}>Nguyên mẫu · Toán 12</div>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ fontSize: 72, fontWeight: 700, lineHeight: 1.1 }}>{SITE_NAME}</div>
          <div style={{ marginTop: 16, fontSize: 28, color: "#5C5F66", maxWidth: 880 }}>
            Đơn điệu và cực trị · phiếu 5 bước · gia sư không đưa đáp án
          </div>
        </div>
        <div style={{ display: "flex", fontSize: 22, color: "#5C5F66" }}>hoc-toan-ai.onrender.com</div>
      </div>
    ),
    { ...size },
  );
}
