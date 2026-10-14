import { Muc4 } from './hoc-sinh';
/** JSON contract: docs/API_UX.md. Details are typed per screen. */
export interface Item<D=Record<string,unknown>>{id:string;title:string;subtitle:string;status:string;details:D;}
export interface Result{message:string;count:number;}
export interface Skill{code:string;name:string;level4:Muc4;level3:string;mastery:number;overridden:boolean;}
export interface StudentDetails{skills:Skill[];submitted:number;reveals?:number;}
export interface BankDetails{code:string;skillCode:string;skillName?:string;level4:Muc4;gate:string;latex:string;}
export interface ModeDetails{dueAt:string|null;/** Máy chủ tính theo giờ của nó; false là đã quá hạn. */conHan?:boolean;}
export interface RecommendationDetails{level4:Muc4;skillCode:string;}
export interface WeeklyPlan {sessions:Item<ScheduleDetails>[];loiKhuyen:string;}
export interface AlertDetails {studentId:string;}
export interface ScheduleDetails{startsAt:string;endsAt:string;}
/** Nhắc buổi học có startsAt/endsAt; nhắc bài được giao có dueAt. */
export interface ReminderDetails extends Partial<ScheduleDetails>{dueAt?:string|null;}
export interface FormulaDetails{statement:string;tier1:string;tier2:string;version:number;}
export interface Library{formulas:Item<FormulaDetails>[];documents:Item[];}
/** Chi tiết lượt giao; các trường bộ bài có trên lớp seed dev. */
export interface AssignmentDetails extends Record<string, unknown> {
  studentId?: string;
  problemCode?: string;
  mode?: string;
  dueAt?: string | null;
  tenBo?: string | null;
  setKey?: string;
  classId?: string;
  submitted?: boolean;
}
/** Tổng quan mới trả một dòng mỗi bộ; máy chủ cũ trả lượt giao. */
export interface DeadlineDetails extends AssignmentDetails {
  problemCodes?: string[];
  problemCount?: number;
  studentCount?: number;
  assignmentCount?: number;
  submittedCount?: number;
}
export interface Dashboard{students:number;stuck:number;pending:number;assignments:number;distribution:Record<string,number>;deadlines:Item<DeadlineDetails>[];}
export interface Settings{lopId:string;revealSolutionAfterSubmit:boolean;aiProvider:string;}
export interface UserDetails{enabled:boolean;synthetic:boolean;}
export interface StudentProfile{student:Item<StudentDetails>;history:Item[];}
