import { Muc4 } from './hoc-sinh';
/** JSON contract: docs/API_UX.md. Details are typed per screen. */
export interface Item<D=Record<string,unknown>>{id:string;title:string;subtitle:string;status:string;details:D;}
export interface Result{message:string;count:number;}
export interface Skill{code:string;name:string;level4:Muc4;level3:string;mastery:number;overridden:boolean;}
export interface StudentDetails{skills:Skill[];submitted:number;}
export interface BankDetails{code:string;skillCode:string;level4:Muc4;gate:string;latex:string;}
export interface ModeDetails{dueAt:string|null;}
export interface RecommendationDetails{level4:Muc4;skillCode:string;}
export interface ScheduleDetails{startsAt:string;endsAt:string;}
export interface FormulaDetails{statement:string;tier1:string;tier2:string;version:number;}
export interface Library{formulas:Item<FormulaDetails>[];documents:Item[];}
export interface Dashboard{students:number;stuck:number;pending:number;assignments:number;distribution:Record<string,number>;deadlines:Item[];}
export interface Settings{lopId:string;revealSolutionAfterSubmit:boolean;aiProvider:string;}
export interface UserDetails{enabled:boolean;synthetic:boolean;}
export interface StudentProfile{student:Item<StudentDetails>;history:Item[];}
