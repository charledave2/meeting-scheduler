import { eq, isNull } from "drizzle-orm";
import { getDb } from "../../../db";
import { members, schedules, users } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";
import { addCalendarMonth, meetingDateFromLabel } from "../../meeting-date";
import { ensureScheduleSoftDeleteStorage } from "../../schedule-soft-delete-storage";

type SavedPart={id?:number;title?:string;assignee?:string;assistant?:string;secondaryAssignee?:string;secondaryAssistant?:string};
type DerivedRecord={memberId:number;meetingDate:string;assignmentRole:"main"|"assistant"};

export async function GET(request:Request){
  try{
    const auth=await getChatGPTUser();if(!auth)return Response.json({error:"Unauthorized"},{status:401});
    await ensureScheduleSoftDeleteStorage();
    const db=getDb();const [user]=await db.select({id:users.id}).from(users).where(eq(users.email,auth.email)).limit(1);if(!user)return Response.json({error:"Unauthorized"},{status:401});
    const raw=new URL(request.url).searchParams.get("date")||"";const asOfDate=/^\d{4}-\d{2}-\d{2}$/.test(raw)?raw:meetingDateFromLabel(raw);if(!asOfDate)return Response.json({error:"Invalid meeting date"},{status:400});
    const [memberRows,scheduleRows]=await Promise.all([db.select().from(members),db.select({weekLabel:schedules.weekLabel,assignmentsJson:schedules.assignmentsJson}).from(schedules).where(isNull(schedules.deletedAt))]);
    const byName=new Map(memberRows.map(member=>[member.name.trim().toLocaleLowerCase("tl"),member]));const records:DerivedRecord[]=[];
    for(const schedule of scheduleRows){const meetingDate=meetingDateFromLabel(schedule.weekLabel);if(!meetingDate)continue;let parts:SavedPart[]=[];try{const parsed=JSON.parse(schedule.assignmentsJson);if(Array.isArray(parsed))parts=parsed;}catch{}
      for(const part of parts){const slots=[[part.assignee,"main"],[part.assistant,"assistant"],[part.secondaryAssignee,"main"],[part.secondaryAssistant,"assistant"]] as const;for(const [name,assignmentRole] of slots){const member=byName.get((name||"").trim().toLocaleLowerCase("tl"));if(member)records.push({memberId:member.id,meetingDate,assignmentRole});}}
    }
    const month=asOfDate.slice(0,7);const stats=memberRows.map(member=>{const assigned=records.filter(row=>row.memberId===member.id).sort((a,b)=>a.meetingDate.localeCompare(b.meetingDate));const current=assigned.filter(row=>row.meetingDate.startsWith(month));const last=assigned.at(-1)?.meetingDate||null;const nextEligibleDate=last?addCalendarMonth(last):null;const exempt=member.appointment!=="publisher";const mainThisMonth=current.filter(row=>row.assignmentRole==="main").length;const assistantThisMonth=current.filter(row=>row.assignmentRole==="assistant").length;const totalThisMonth=current.length;const eligible=exempt||!nextEligibleDate||asOfDate>nextEligibleDate;return {memberId:member.id,mainThisMonth,assistantThisMonth,totalThisMonth,lastAssignmentDate:last,nextEligibleDate,eligible,exempt,neverAssigned:assigned.length===0,recommended:!exempt&&totalThisMonth===0&&eligible};});
    return Response.json({asOfDate,month,stats,source:"saved-schedules"});
  }catch(error){console.error("Rotation calculation failed",error);return Response.json({error:"Unable to calculate assignment history."},{status:500});}
}
