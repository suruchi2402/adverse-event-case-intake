/**
 * Provenance offsets count Unicode code points; JavaScript strings index UTF-16 code units.
 * These helpers convert between the two and cut the text into segments for highlighting.
 */

/** A function from code point offset to UTF-16 index for this text. */
export function codePointIndexer(text: string): (codePoint: number) => number {
  const utf16At: number[] = [0];
  let index = 0;
  for (const char of text) {
    index += char.length;
    utf16At.push(index);
  }
  return (codePoint) => utf16At[Math.max(0, Math.min(codePoint, utf16At.length - 1))] ?? text.length;
}

export interface Span {
  /** UTF-16 start, inclusive. */
  start: number;
  /** UTF-16 end, exclusive. */
  end: number;
  /** The field this span supports. */
  fieldId: string;
}

export interface Segment {
  start: number;
  end: number;
  text: string;
  /** Fields whose quotes cover this segment, in the order their spans were given. */
  fieldIds: string[];
}

/** Splits the text at every span boundary. Overlapping quotes produce segments covered by several fields. */
export function segmentText(text: string, spans: Span[]): Segment[] {
  const valid = spans.filter((s) => s.start >= 0 && s.end <= text.length && s.start < s.end);
  const cuts = new Set<number>([0, text.length]);
  for (const s of valid) {
    cuts.add(s.start);
    cuts.add(s.end);
  }
  const points = [...cuts].sort((a, b) => a - b);
  const segments: Segment[] = [];
  for (let i = 0; i < points.length - 1; i++) {
    const start = points[i]!;
    const end = points[i + 1]!;
    const fieldIds = [...new Set(valid.filter((s) => s.start <= start && s.end >= end).map((s) => s.fieldId))];
    segments.push({ start, end, text: text.slice(start, end), fieldIds });
  }
  return segments;
}
