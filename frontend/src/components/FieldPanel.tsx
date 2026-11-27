import type { ExtractedCase } from "../api/types";
import {
  countByRisk,
  matchesFilter,
  needsReview,
  RISK_LABEL,
  RISK_ORDER,
  type FieldFilter,
  type SectionGroup,
} from "../lib/review";
import { FieldRow } from "./FieldRow";

interface Props {
  cases: ExtractedCase[];
  caseIndex: number;
  onCase: (index: number) => void;
  groups: SectionGroup[];
  filter: FieldFilter;
  onFilter: (filter: FieldFilter) => void;
  selectedFieldId: string | null;
  onSelect: (fieldId: string) => void;
}

export function FieldPanel({ cases, caseIndex, onCase, groups, filter, onFilter, selectedFieldId, onSelect }: Props) {
  const counts = countByRisk(groups);
  const total = RISK_ORDER.reduce((sum, risk) => sum + counts[risk], 0);
  const reviewCount = RISK_ORDER.filter(needsReview).reduce((sum, risk) => sum + counts[risk], 0);
  const filters: { id: FieldFilter; label: string; count: number }[] = [
    { id: "review", label: "Needs review", count: reviewCount },
    { id: "unknown", label: "Not stated", count: counts.unknown },
    { id: "all", label: "All fields", count: total },
  ];

  return (
    <section className="field-panel" aria-label="Extracted fields">
      {cases.length > 1 ? (
        <div className="case-tabs" role="tablist" aria-label="Cases in this document">
          {cases.map((kase, index) => (
            <button
              key={kase.case_index}
              type="button"
              role="tab"
              aria-selected={index === caseIndex}
              className={`case-tab${index === caseIndex ? " is-active" : ""}`}
              onClick={() => onCase(index)}
            >
              <span className="case-tab-index">Case {index + 1}</span>
              <span className="case-tab-label">{kase.label ?? `Patient ${index + 1}`}</span>
            </button>
          ))}
        </div>
      ) : null}

      <div className="risk-summary">
        <div className="risk-bar" role="img" aria-label={RISK_ORDER.map((r) => `${counts[r]} ${RISK_LABEL[r]}`).join(", ")}>
          {RISK_ORDER.filter((risk) => counts[risk] > 0).map((risk) => (
            <span key={risk} className={`risk-bar-part risk-${risk}`} style={{ flexGrow: counts[risk] }} />
          ))}
        </div>
        <div className="risk-legend">
          {RISK_ORDER.map((risk) => (
            <span key={risk} className={`risk-legend-item risk-${risk}${counts[risk] === 0 ? " is-zero" : ""}`}>
              <span className="risk-glyph" aria-hidden="true" />
              {counts[risk]} {RISK_LABEL[risk].toLowerCase()}
            </span>
          ))}
        </div>
        <div className="filter-chips" role="radiogroup" aria-label="Show fields">
          {filters.map((f) => (
            <button
              key={f.id}
              type="button"
              role="radio"
              aria-checked={filter === f.id}
              className={`chip${filter === f.id ? " is-active" : ""}`}
              onClick={() => onFilter(f.id)}
            >
              {f.label}
              <span className="chip-count">{f.count}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="field-sections">
        {groups.map((group) => {
          const visible = group.entries.filter((entry) => matchesFilter(entry.risk, filter));
          if (visible.length === 0) return null;
          return (
            <section key={group.id} className="field-section" aria-labelledby={`section-${group.id}`}>
              <h3 className="section-title" id={`section-${group.id}`}>
                {group.label}
                <span className="section-count">
                  {visible.length}
                  {visible.length !== group.entries.length ? ` of ${group.entries.length}` : ""}
                </span>
              </h3>
              {visible.map((entry) => (
                <FieldRow
                  key={entry.field.id}
                  entry={entry}
                  selected={entry.field.id === selectedFieldId}
                  onSelect={() => onSelect(entry.field.id)}
                />
              ))}
            </section>
          );
        })}
        {groups.every((g) => g.entries.every((e) => !matchesFilter(e.risk, filter))) ? (
          <p className="filter-empty">
            {filter === "review" ? "Nothing needs review in this case: every value is stated explicitly or not stated at all." : "No field matches this filter."}
          </p>
        ) : null}
      </div>
    </section>
  );
}
