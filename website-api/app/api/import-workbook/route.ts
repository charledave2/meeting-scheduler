import { getChatGPTUser } from "../../chatgpt-auth";

type Section = "treasures" | "ministry" | "living";
const clean = (value: string) => value.replace(/<[^>]+>/g, " ").replace(/&nbsp;|&#160;/g, " ").replace(/&amp;/g, "&").replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/\s+/g, " ").replace(/\(\s+/g, "(").replace(/\s+\)/g, ")").trim();
const clock = (minutes: number) => `${Math.floor(minutes / 60) > 12 ? Math.floor(minutes / 60) - 12 : Math.floor(minutes / 60)}:${String(minutes % 60).padStart(2, "0")}`;
const sectionFromHeading = (heading: string): Section | null => {
  const value = heading.toUpperCase();
  if (value.includes("KAYAMANAN MULA")) return "treasures";
  if (value.includes("MAGING MAHUSAY")) return "ministry";
  if (value.includes("PAMUMUHAY BILANG")) return "living";
  return null;
};

export async function POST(request: Request) {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  if (user.role === "viewer") return Response.json({ error: "Viewer accounts cannot import workbooks." }, { status: 403 });
  try {
    const { url } = await request.json() as { url?: string };
    if (!url) return Response.json({ error: "Enter the JW.org link." }, { status: 400 });
    const parsed = new URL(url);
    const docid = parsed.searchParams.get("docid") || parsed.pathname.split("/").pop();
    if (!docid || !/^[0-9]+$/.test(docid)) return Response.json({ error: "The link does not contain a valid document ID." }, { status: 400 });
    const source = `https://wol.jw.org/tl/wol/d/r27/lp-tg/${docid}`;
    const response = await fetch(source, { headers: { "user-agent": "Meeting Scheduler/1.0" } });
    if (!response.ok) throw new Error("Unable to read the workbook page.");
    const html = await response.text();
    const title = clean((html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i) || [])[1] || "");
    const tokens = [...html.matchAll(/<h([23])[^>]*>([\s\S]*?)<\/h\1>/gi)].map((match) => ({ level: match[1], text: clean(match[2]) }));
    const reading = tokens.find((token) => token.level === "2" && !sectionFromHeading(token.text))?.text || "";
    const headingMatches = [...html.matchAll(/<h([23])\b[^>]*>([\s\S]*?)<\/h\1>/gi)];
    const parsedParts: Array<{ id: number; section: Section; title: string; durationMinutes: number; detail: string }> = [];
    let currentSection: Section | null = null;
    for (let index = 0; index < headingMatches.length; index++) {
      const match = headingMatches[index];
      const level = match[1];
      const heading = clean(match[2]);
      if (level === "2") {
        currentSection = sectionFromHeading(heading) || currentSection;
        continue;
      }
      const headingMatch = heading.match(/^(\d+)\.\s*(.+)$/i);
      if (!headingMatch || !currentSection) continue;
      const partNumber = Number(headingMatch[1]);
      const contentStart = (match.index || 0) + match[0].length;
      const contentEnd = headingMatches[index + 1]?.index ?? html.length;
      const block = html.slice(contentStart, contentEnd);
      const firstParagraph = clean((block.match(/<p\b[^>]*>([\s\S]*?)<\/p>/i) || [])[1] || "");
      const detailMatch = firstParagraph.match(/^\((\d+)\s*min\.\)\s*(.*)$/i);
      const durationMinutes = Number(detailMatch?.[1] || 0);
      const detail = detailMatch?.[2]?.trim() || "";
      parsedParts.push({ id: partNumber, section: currentSection, title: `${partNumber}. ${headingMatch[2]}`, durationMinutes, detail });
    }
    const lastMinistryId = parsedParts.filter((part) => part.section === "ministry").at(-1)?.id;
    const parts: Array<Record<string, unknown>> = [];
    let currentMinutes = 18 * 60 + 6;
    for (const part of parsedParts) {
      const isBibleReading = part.title.toLocaleLowerCase("tl").includes("pagbabasa ng bibliya");
      const isCongregationBibleStudy = part.title.toLocaleLowerCase("tl").includes("pag-aaral ng kongregasyon sa bibliya");
      const isWhatWouldYouSay = part.title.toLocaleLowerCase("tl").includes("ano ang sasabihin mo");
      const hasHalls = (part.section === "ministry" && !isWhatWouldYouSay) || isBibleReading;
      const hasAssistant = part.section === "ministry" && !isWhatWouldYouSay;
      parts.push({ id: part.id, time: clock(currentMinutes), section: part.section, title: part.title, duration: part.durationMinutes ? `${part.durationMinutes} min.` : "", detail: part.detail, assignee: "", ...(hasHalls ? { secondaryAssignee: "" } : {}), ...(hasAssistant ? { assistant: "", secondaryAssistant: "" } : {}), ...(isCongregationBibleStudy ? { assistant: "" } : {}) });
      currentMinutes += part.durationMinutes + (part.id >= 3 ? 1 : 0);
      if (part.id === lastMinistryId) currentMinutes += 5;
    }
    const songs = [...html.matchAll(/Awit(?:\s+Blg\.)?\s*(\d+)/gi)].map((match) => match[1]);
    if (!title || !reading || !parts.length) throw new Error("The workbook format was not recognized.");
    return Response.json({ meta: { date: title.replace(/\b([A-ZÁÉÍÓÚÑ]+)\b/g, (word) => word.charAt(0) + word.slice(1).toLowerCase()), reading: reading.toUpperCase(), openingSong: songs[0] || "", closingSong: songs.at(-1) || "", chairman: "", openingPrayer: "", closingPrayer: "", adviser: "", audienceGroup: "" }, parts, source });
  } catch (error) { return Response.json({ error: error instanceof Error ? error.message : "Import failed." }, { status: 400 }); }
}
