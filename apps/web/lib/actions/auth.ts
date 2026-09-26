"use server";

import { redirect } from "next/navigation";
import { clearSession } from "../auth";

export async function dangXuat() {
  await clearSession();
  redirect("/dang-nhap");
}
