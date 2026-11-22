import { eq, isNull } from "drizzle-orm";
import { getDb } from "../../../db";
import { members, schedules, users } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";
import { addCalendarMonth, meetingDateFromLabel } from "../../meeting-date";
import { ensureScheduleSoftDeleteStorage } from "../../schedule-soft-delete-storage";
import { canAccessAnyTool } from "../../permissions";

type SavedPart={id?:number;title?:string;assignee?:string;assistant?:string;secondaryAssignee?:string;secondaryAssistant?:string};
export async function GET(request:Request){
  try{
    const auth=await getChatGPTUser();if(!auth)return Response.json({error:"Unauthorized"},{status:401});if(!await canAccessAnyTool(auth,["participants","assignments"],"view"))return Response.json({error:"This account cannot view participant rotation."},{status:403});
    await ensureScheduleSoftDeleteStorage();
    const db=getDb();const [user]=await db.select({id:users.id}).from(users).where(eq(users.email,auth.email)).limit(1);if(!user)return Response.json({error:"Unauthorized"},{status:401});
    const raw=new URL(request.url).searchParams.get("date")||"";const asOfDate=/^\d{4}-\d{2}-\d{2}$/.test(raw)?raw:meetingDateFromLabel(raw);if(!asOfDate)return Response.json({error:"Invalid meeting date"},{status:400});
    const [memberRows,scheduleRows]=await Promise.all([db.select().from(members),db.select({weekLabel:schedules.weekLabel,assignmentsJson:schedules.assignmentsJson}).from(schedules).where(isNull(schedules.deletedAt))]);
    const byName=new Map(memberRows.map(member=>[member.name.trim().toLocaleLowerCase("tl"),member.id]));
    const totals=new Map<number,{main:number;assistant:number;last:string|null;month:number}>();
    const month=asOfDate.slice(0,7);
    for(const schedule of scheduleRows){
      const meetingDate=meetingDateFromLabel(schedule.weekLabel);if(!meetingDate)continue;
      let parts:SavedPart[]=[];try{const parsed=JSON.parse(schedule.assignmentsJson);if(Array.isArray(parsed))parts=parsed;}catch{}
      for(const part of parts){
        const slots=[[part.assignee,"main"],[part.assistant,"assistant"],[part.secondaryAssignee,"main"],[part.secondaryAssistant,"assistant"]] as const;
        for(const [name,assignmentRole] of slots){
          const memberId=byName.get((name||"").trim().toLocaleLowerCase("tl"));if(!memberId)continue;
          const stat=totals.get(memberId)||{main:0,assistant:0,last:null,month:0};
          if(!stat.last||meetingDate>stat.last)stat.last=meetingDate;
          if(meetingDate.startsWith(month)){stat.month++;if(assignmentRole==="main")stat.main++;else stat.assistant++;}
          totals.set(memberId,stat);
        }
      }
    }
    const stats=memberRows.map(member=>{const assigned=totals.get(member.id);const last=assigned?.last||null;const nextEligibleDate=last?addCalendarMonth(last):null;const exempt=member.appointment!=="publisher";const mainThisMonth=assigned?.main||0;const assistantThisMonth=assigned?.assistant||0;const totalThisMonth=assigned?.month||0;const eligible=exempt||!nextEligibleDate||asOfDate>nextEligibleDate;return {memberId:member.id,mainThisMonth,assistantThisMonth,totalThisMonth,lastAssignmentDate:last,nextEligibleDate,eligible,exempt,neverAssigned:!assigned,recommended:!exempt&&totalThisMonth===0&&eligible};});
    return Response.json({asOfDate,month,stats,source:"saved-schedules"});
  }catch(error){console.error("Rotation calculation failed",error);return Response.json({error:"Unable to calculate assignment history."},{status:500});}
}
