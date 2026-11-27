import { useEffect, useMemo, useRef } from "react";
import { RISK_ORDER, type FieldEntry } from "../lib/review";
import { codePointIndexer, segmentText, type Span } from "../lib/segments";

interface Props {
  text: string;
  entries: FieldEntry[];
  selectedFieldId: string | null;
  onSelect: (fieldId: string) => void;
}

/**
 * The document with every quote of the current case marked. The selected field's quotes are highlighted strongly
 * and scrolled into view; clicking any marked passage selects its field.
 */
export function DocumentPane({ text, entries, selectedFieldId, onSelect }: Props) {
  const scroller = useRef<HTMLDivElement>(null);
  const byId = useMemo(() => new Map(entries.map((e) => [e.field.id, e])), [entries]);

  const segments = useMemo(() => {
    const toUtf16 = codePointIndexer(text);
    const spans: Span[] = [];
    for (const { field, result } of entries) {
      if (result.status !== "extracted") continue;
      for (const p of result.provenance ?? []) {
        spans.push({ start: toUtf16(p.start), end: toUtf16(p.end), fieldId: field.id });
      }
    }
    return segmentText(text, spans);
  }, [text, entries]);

  const anchorIndex = selectedFieldId ? segments.findIndex((s) => s.fieldIds.includes(selectedFieldId)) : -1;

  useEffect(() => {
    if (anchorIndex < 0) return;
    const mark = scroller.current?.querySelector<HTMLElement>("[data-anchor='true']");
    mark?.scrollIntoView({ block: "center", behavior: prefersReducedMotion() ? "auto" : "smooth" });
  }, [anchorIndex, selectedFieldId]);

  return (
    <div className="document-scroll" ref={scroller}>
      <pre className="document-text">
        {segments.map((segment, index) => {
          if (segment.fieldIds.length === 0) return segment.text;
          const isSelected = selectedFieldId !== null && segment.fieldIds.includes(selectedFieldId);
          const owners = segment.fieldIds.map((id) => byId.get(id)).filter((e): e is FieldEntry => e !== undefined);
          const risk = RISK_ORDER.find((r) => owners.some((o) => o.risk === r)) ?? "high";
          // Clicking a passage that supports several fields cycles through them.
          const position = owners.findIndex((o) => o.field.id === selectedFieldId);
          const target = owners[(position + 1) % owners.length]?.field.id ?? null;
          return (
            <mark
              key={segment.start}
              className={`quote risk-${risk}${isSelected ? " is-selected" : ""}`}
              data-anchor={index === anchorIndex ? "true" : undefined}
              title={owners.map((o) => o.field.label).join(", ")}
              onClick={() => target && onSelect(target)}
            >
              {segment.text}
            </mark>
          );
        })}
      </pre>
    </div>
  );
}

function prefersReducedMotion(): boolean {
  return typeof window.matchMedia === "function" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}
