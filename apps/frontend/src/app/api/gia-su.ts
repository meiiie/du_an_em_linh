/** DTO của services/core/tutor; mô hình hiển thị được ánh xạ riêng bên dưới. */
export const API_GIA_SU = '/api/hs/gia-su/v2/phien';
export type TrangThaiLuot = 'generating' | 'completed' | 'cancelled' | 'error';
export interface PhienGiaSuV2 {
  sessionId: string;
  lastSeq: number;
  turnId: string | null;
  status: TrangThaiLuot | 'idle';
}
export interface SessionGiaSuV2 {
  id: string;
  studentId: string;
  assignmentId: string;
  classId: string;
  tomTat: string;
  summaryThrough: number;
  contextVersion: string;
  lastSeq: number;
  revision: number;
  tokenCount: number;
}
export interface TurnGiaSuV2 {
  id: string;
  sessionId: string;
  requestId: string;
  ordinal: number;
  step: string | null;
  level: number;
  question: string;
  answer: string;
  status: TrangThaiLuot;
  createdAt: string;
  references?: readonly NguonGiaSu[];
}
/** Các lượt mới nhất trước; phân trang bằng offset/limit. */
export interface LichSuGiaSuV2 { session: SessionGiaSuV2; turns: readonly TurnGiaSuV2[] }
export interface HoiGiaSuV2 { requestId: string; maBuoc: string | null; cap: number; cauHoi: string }
export interface HuyGiaSuV2 { turnId: string }
export interface XacNhanHuyGiaSuV2 { sessionId: string; turnId: string; status: Exclude<TrangThaiLuot, 'generating'>; lastSeq: number }
interface EventData { v: 2; sessionId: string; turnId: string; seq: number }
export type SuKienSse =
  | { event: 'start'; id?: string; data: EventData & { requestId: string; maBuoc: string | null; cap: number; contextVersion: string; references?: readonly NguonGiaSu[] } }
  | { event: 'delta'; id?: string; data: EventData & { text: string } }
  | { event: 'done'; id?: string; data: EventData & { status: 'completed' | 'cancelled'; tokens?: number; durationMs?: number } }
  | { event: 'error'; id?: string; data: EventData & { code: string; message: string; retryable: boolean } }
  | { event: 'heartbeat'; id?: string; data: { sessionId: string; turnId?: string; lastSeq: number } };

/** Snapshot bảng đã khóa, lấy từ start SSE hoặc lịch sử của chính lượt này. */
export interface NguonGiaSu { index: number; text: string; latex?: string }
/** Trạng thái UI, không gửi các trường riêng (text, retryOfTurnId, error) lên API. */
export interface LuotGiaSuV2 {
  requestId: string;
  turnId: string;
  maBuoc: string | null;
  cap: number;
  cauHoi: string;
  retryOfTurnId?: string;
  text: string;
  references?: readonly NguonGiaSu[];
  status: TrangThaiLuot;
  error?: { message: string; retryable: boolean };
}
