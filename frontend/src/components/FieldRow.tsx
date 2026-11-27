import { useEffect, useId, useRef } from "react";
import type { FieldEntry } from "../lib/review";
import { FieldControl } from "./FieldControl";
import { RiskBadge } from "./RiskBadge";

const QUOTE_PREVIEW = 140;

export function FieldRow({ entry, selected, onSelect }: { entry: FieldEntry; selected: boolean; onSelect: () => void }) {
  const { field, result, risk } = entry;
  const labelId = useId();
  const ref = useRef<HTMLDivElement>(null);
  const quotes = result.status === "extracted" ? (result.provenance ?? []) : [];

  useEffect(() => {
    if (selected) ref.current?.scrollIntoView({ block: "nearest" });
  }, [selected]);

  return (
    <div
      ref={ref}
      className={`field-row risk-${risk}${selected ? " is-selected" : ""}`}
      data-status={result.status}
      onClick={onSelect}
    >
      <button type="button" className="field-head" aria-pressed={selected} title={field.guidance}>
        <span className="field-label" id={labelId}>
          {field.label}
        </span>
        <RiskBadge risk={risk} />
      </button>
      <div className="field-control">
        <FieldControl field={field} result={result} labelledBy={labelId} />
      </div>
      {quotes.length > 0 ? (
        <div className="field-quotes">
          {quotes.map((p) => (
            <span key={`${p.start}-${p.end}`} className="field-quote-line">
              <q className="field-quote">{p.quote.length > QUOTE_PREVIEW ? `${p.quote.slice(0, QUOTE_PREVIEW)}…` : p.quote}</q>
              {p.page ? <span className="field-page">p. {p.page}</span> : null}
            </span>
          ))}
        </div>
      ) : null}
      {result.note ? <p className="field-note">{result.note}</p> : null}
    </div>
  );
}
