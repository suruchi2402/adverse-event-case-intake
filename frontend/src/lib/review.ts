import type { Catalog, CatalogField, ExtractedCase, FieldResult } from "../api/types";

/**
 * How much reviewer attention a field needs, most urgent first.
 * conflict: the document contradicts itself. low/medium: a value the model was unsure of or computed.
 * unknown: a gap to fill. high: stated explicitly.
 */
export type Risk = "conflict" | "low" | "medium" | "unknown" | "high";

export const RISK_ORDER: Risk[] = ["conflict", "low", "medium", "unknown", "high"];

export const RISK_LABEL: Record<Risk, string> = {
  conflict: "Conflict",
  low: "Low confidence",
  medium: "Medium confidence",
  unknown: "Not stated",
  high: "High confidence",
};

export const RISK_SHORT: Record<Risk, string> = {
  conflict: "Conflict",
  low: "Low",
  medium: "Medium",
  unknown: "Unknown",
  high: "High",
};

/** A field that is missing from the output is treated as unknown: every catalog field is shown. */
export const MISSING_RESULT: FieldResult = {
  status: "unknown",
  note: "No result for this field in the output.",
};

export function riskOf(result: FieldResult): Risk {
  if (result.status === "conflict") return "conflict";
  if (result.status === "unknown") return "unknown";
  return result.confidence ?? "high";
}

export function needsReview(risk: Risk): boolean {
  return risk === "conflict" || risk === "low" || risk === "medium";
}

export interface FieldEntry {
  field: CatalogField;
  result: FieldResult;
  risk: Risk;
}

export interface SectionGroup {
  id: string;
  label: string;
  entries: FieldEntry[];
}

/** Catalog sections in catalog order, each with its fields in catalog order. */
export function groupBySection(catalog: Catalog, kase: ExtractedCase): SectionGroup[] {
  return catalog.sections
    .map((section) => ({
      id: section.id,
      label: section.label,
      entries: catalog.fields
        .filter((field) => field.section === section.id)
        .map((field) => {
          const result = kase.fields[field.id] ?? MISSING_RESULT;
          return { field, result, risk: riskOf(result) };
        }),
    }))
    .filter((group) => group.entries.length > 0);
}

export function countByRisk(groups: SectionGroup[]): Record<Risk, number> {
  const counts: Record<Risk, number> = { conflict: 0, low: 0, medium: 0, unknown: 0, high: 0 };
  for (const group of groups) for (const entry of group.entries) counts[entry.risk] += 1;
  return counts;
}

export type FieldFilter = "all" | "review" | "unknown";

export function matchesFilter(risk: Risk, filter: FieldFilter): boolean {
  if (filter === "review") return needsReview(risk);
  if (filter === "unknown") return risk === "unknown";
  return true;
}

/** Human labels for the flags the pipeline writes; anything else is shown as written. */
export const FLAG_LABEL: Record<string, string> = {
  no_adverse_event: "No adverse event",
  multiple_patients: "Several patients",
  unsupported_format: "Unsupported format",
  possible_duplicate: "Possible duplicate",
  embedded_instructions: "Embedded instructions ignored",
};

export function flagLabel(flag: string): string {
  return FLAG_LABEL[flag] ?? flag;
}
