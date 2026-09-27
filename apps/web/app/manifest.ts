import type { MetadataRoute } from "next";
import { SITE_DESC, SITE_NAME } from "@/lib/site";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: SITE_NAME,
    short_name: "Học toán AI",
    description: SITE_DESC,
    start_url: "/",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#17181C",
    lang: "vi",
    icons: [
      { src: "/icon", sizes: "32x32", type: "image/png" },
      { src: "/apple-icon", sizes: "180x180", type: "image/png" },
    ],
  };
}
