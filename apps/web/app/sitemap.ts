import type { MetadataRoute } from "next";
import { siteUrl } from "@/lib/site";

export default function sitemap(): MetadataRoute.Sitemap {
  const goc = siteUrl().origin;
  const home = goc + "/";
  return [
    {
      url: home,
      lastModified: new Date("2026-09-27"),
      changeFrequency: "weekly",
      priority: 1,
      alternates: {
        languages: {
          "vi-VN": home,
          "x-default": home,
        },
      },
    },
  ];
}
