/** TutorSessionDto.UsageReport: số liệu tích lũy, chưa có giới hạn hoặc kỳ tính trong hợp đồng. */
export interface SuDungGiaSu {
  studentId: string;
  requests: number;
  inputTokens: number;
  outputTokens: number;
  estimatedTokens: number;
}
export interface BaoCaoGiaSu extends Omit<SuDungGiaSu, 'studentId'> {
  classId: string;
  students: SuDungGiaSu[];
}
