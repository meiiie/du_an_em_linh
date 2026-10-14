/** Hợp đồng frontend cho docs/design/gia-su-v2.md §6 (máy chủ v2 triển khai riêng). */
export const API_GIA_SU = '/api/hs/gia-su/v2/phien';
export type TrangThaiLuot = 'running' | 'completed' | 'cancelled' | 'error';
export interface CauGiaSu { sentenceIndex: number; text: string }
/** Dòng của bảng đã khóa tại thời điểm lượt; không dùng kho công thức hiện hành thay thế. */
export interface NguonGiaSu { index: number; text: string; latex?: string }
export interface LuotGiaSuV2 {
  requestId: string;
  turnId: string;
  maBuoc: string;
  cap: number;
  cauHoi: string;
  retryOfTurnId?: string;
  sentences: readonly CauGiaSu[];
  references?: readonly NguonGiaSu[];
  status: TrangThaiLuot;
  error?: { message: string; retryable: boolean };
}
export interface PhienGiaSuV2 {
  sessionId: string;
  lastSeq: number;
  currentTurn?: { turnId: string; status: TrangThaiLuot } | null;
}
/** Trang đầu là các lượt gần nhất, theo thứ tự thời gian; cursor lấy các lượt cũ hơn. */
export interface LichSuGiaSuV2 {
  sessionId: string;
  lastSeq: number;
  turns: readonly LuotGiaSuV2[];
  nextCursor?: string | null;
}
export interface DuLieuGiaSu {
  v?: number;
  sessionId: string;
  turnId: string;
  seq?: number;
  lastSeq?: number;
  requestId?: string;
  sentenceIndex?: number;
  text?: string;
  status?: 'completed' | 'cancelled';
  message?: string;
  retryable?: boolean;
  references?: readonly NguonGiaSu[];
}
export interface SuKienSse { event: string; id?: string; data: DuLieuGiaSu }
