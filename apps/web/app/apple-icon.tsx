import { ImageResponse } from "next/og";

export const size = { width: 180, height: 180 };
export const contentType = "image/png";

export default function AppleIcon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "#17181C",
          color: "#F4F4F5",
          fontSize: 72,
          fontWeight: 700,
          fontFamily: "Georgia, serif",
        }}
      >
        f′
      </div>
    ),
    { ...size },
  );
}
