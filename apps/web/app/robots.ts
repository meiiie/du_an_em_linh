import type { MetadataRoute } from "next";
import { siteUrl } from "@/lib/site";

const ICON = [
  "/favicon.ico",
  "/icon.svg",
  "/icon-48.png",
  "/icon-96.png",
  "/icon-192.png",
  "/icon-512.png",
  "/apple-touch-icon.png",
];

export default function robots(): MetadataRoute.Robots {
  const goc = siteUrl().origin;
  return {
    rules: [
      {
        userAgent: "*",
        allow: ["/", "/llms.txt", ...ICON],
        disallow: ["/gv/", "/hs/", "/api/"],
      },
      {
        userAgent: "Googlebot",
        allow: ["/"],
        disallow: ["/gv/", "/hs/", "/api/"],
      },
      {
        userAgent: "Googlebot-Image",
        allow: ["/", ...ICON],
      },
    ],
    sitemap: goc + "/sitemap.xml",
    host: goc,
  };
}
