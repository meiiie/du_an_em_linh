import type { MetadataRoute } from "next";
import { siteUrl } from "@/lib/site";

export default function robots(): MetadataRoute.Robots {
  const goc = siteUrl().origin;
  return {
    rules: [
      {
        userAgent: "*",
        allow: "/",
        disallow: ["/gv", "/hs", "/api"],
      },
    ],
    sitemap: goc + "/sitemap.xml",
    host: goc,
  };
}
