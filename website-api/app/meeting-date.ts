const months: Record<string, number> = {
  jan: 1, feb: 2, mar: 3, apr: 4, jun: 6, jul: 7, aug: 8, sep: 9, sept: 9, oct: 10, nov: 11, dec: 12,
  enero: 1, january: 1, pebrero: 2, february: 2, marso: 3, march: 3,
  abril: 4, april: 4, mayo: 5, may: 5, hunyo: 6, june: 6,
  hulyo: 7, july: 7, agosto: 8, august: 8, setyembre: 9, september: 9,
  oktubre: 10, october: 10, nobyembre: 11, november: 11,
  disyembre: 12, december: 12,
};

const monthPattern = `(${Object.keys(months).sort((a, b) => b.length - a.length).join("|")})`;
const weekRangePattern = new RegExp(
  `^${monthPattern}\\s+(\\d{1,2})\\s*[–—-]\\s*(?:(${monthPattern})\\s+)?(\\d{1,2})(?:,?\\s+(20\\d{2}))?$`,
  "i",
);

function isoDate(year: number, month: number, day: number) {
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null;
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

/** Returns the Thursday meeting date parsed from a localized week-range label. */
export function meetingDateFromLabel(value: string) {
  const match = value.trim().match(weekRangePattern);
  if (!match) {
    // Preserve compatibility with older labels that contain a single date.
    const single = value.trim().match(new RegExp(`${monthPattern}\\s+(\\d{1,2})(?:,?\\s+(20\\d{2}))?`, "i"));
    if (!single) return null;
    const year = Number(single[3] || new Date().getUTCFullYear());
    const month = months[single[1].toLocaleLowerCase("en-US")];
    const day = Number(single[2]);
    return month ? isoDate(year, month, day) : null;
  }

  const startMonth = months[match[1].toLocaleLowerCase("en-US")];
  const startDay = Number(match[2]);
  const endMonthName = match[4];
  const endDay = Number(match[5]);
  const explicitYear = Number(match[6] || new Date().getUTCFullYear());
  if (!startMonth) return null;
  const endMonth = endMonthName ? months[endMonthName.toLocaleLowerCase("en-US")] : startMonth;
  if (!endMonth) return null;

  // The printed year belongs to the range's end date. Infer the prior year
  // for a range that crosses from December into January.
  const crossesNewYear = endMonth < startMonth || (endMonth === startMonth && endDay < startDay);
  const startYear = crossesNewYear ? explicitYear - 1 : explicitYear;
  const endYear = explicitYear;
  const start = new Date(Date.UTC(startYear, startMonth - 1, startDay));
  const end = new Date(Date.UTC(endYear, endMonth - 1, endDay));
  if (start.getUTCMonth() !== startMonth - 1 || start.getUTCDate() !== startDay ||
      end.getUTCMonth() !== endMonth - 1 || end.getUTCDate() !== endDay ||
      end.getTime() < start.getTime() || end.getTime() - start.getTime() > 7 * 86400000) return null;

  // Meeting weekday is Thursday: Monday week start + 3 days.
  const monday = new Date(start);
  monday.setUTCDate(start.getUTCDate() - ((start.getUTCDay() + 6) % 7));
  const thursday = new Date(monday);
  thursday.setUTCDate(monday.getUTCDate() + 3);
  return `${thursday.getUTCFullYear()}-${String(thursday.getUTCMonth() + 1).padStart(2, "0")}-${String(thursday.getUTCDate()).padStart(2, "0")}`;
}

export function scheduledMeetingDate(weekStart: unknown, label: string) {
  if (typeof weekStart === "string" && /^\d{4}-\d{2}-\d{2}$/.test(weekStart)) {
    const date = new Date(weekStart + "T00:00:00Z");
    if (date.getUTCDay() === 1 && date.toISOString().slice(0, 10) === weekStart) {
      date.setUTCDate(date.getUTCDate() + 3);
      return date.toISOString().slice(0, 10);
    }
  }
  return meetingDateFromLabel(label);
}

export function addCalendarMonth(iso: string) {
  const [year, month, day] = iso.split("-").map(Number);
  const nextMonth = month === 12 ? 1 : month + 1;
  const nextYear = month === 12 ? year + 1 : year;
  const lastDay = new Date(Date.UTC(nextYear, nextMonth, 0)).getUTCDate();
  return `${nextYear}-${String(nextMonth).padStart(2, "0")}-${String(Math.min(day, lastDay)).padStart(2, "0")}`;
}
