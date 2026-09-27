"use client";

import { useEffect } from "react";

/** Cuộn tới #ct- / #tl- khi An bấm trích dẫn. */
export function NeoKho() {
  useEffect(() => {
    function toi() {
      const id = decodeURIComponent(location.hash.replace(/^#/, ""));
      if (!id) return;
      document.getElementById(id)?.scrollIntoView({ block: "start" });
    }
    toi();
    window.addEventListener("hashchange", toi);
    return () => window.removeEventListener("hashchange", toi);
  }, []);
  return null;
}
