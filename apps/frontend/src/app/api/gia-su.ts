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
  step: string;
  level: number;
  question: string;
  answer: string;
  status: TrangThaiLuot;
  createdAt: string;
}
/** Các lượt mới nhất trước; phân trang bằng offset/limit. */
export interface LichSuGiaSuV2 { session: SessionGiaSuV2; turns: readonly TurnGiaSuV2[] }
export interface HoiGiaSuV2 { requestId: string; maBuoc: string; cap: number; cauHoi: string }
export interface HuyGiaSuV2 { turnId: string }
export interface XacNhanHuyGiaSuV2 { sessionId: string; turnId: string; status: Exclude<TrangThaiLuot, 'generating'>; lastSeq: number }
interface EventData { v: 2; sessionId: string; turnId: string; seq: number }
export type SuKienSse =
  | { event: 'start'; id?: string; data: EventData & { requestId: string; maBuoc: string; cap: number; contextVersion: string } }
  | { event: 'delta'; id?: string; data: EventData & { sentenceIndex: number; text: string } }
  | { event: 'regen'; id?: string; data: EventData & { attempt: number; mode: 'rewrite' | 'strict_question' } }
  | { event: 'done'; id?: string; data: EventData & { status: 'completed' | 'cancelled'; lastSentenceIndex: number; tokens?: number; durationMs?: number } }
  | { event: 'error'; id?: string; data: EventData & { code: string; message: string; retryable: boolean } }
  | { event: 'heartbeat'; id?: string; data: { sessionId: string; turnId?: string; lastSeq: number } };

export interface CauGiaSu { sentenceIndex: number; text: string }
/** Nguồn cho bộ hiển thị Markdown; hợp đồng gia sư hiện tại không gửi trường này. */
export interface NguonGiaSu { index: number; text: string; latex?: string }
/** Trạng thái UI, không gửi các trường riêng (sentences, retryOfTurnId, error) lên API. */
export interface LuotGiaSuV2 {
  requestId: string;
  turnId: string;
  maBuoc: string;
  cap: number;
  cauHoi: string;
  retryOfTurnId?: string;
  sentences: readonly CauGiaSu[];
  status: TrangThaiLuot;
  error?: { message: string; retryable: boolean };
}
