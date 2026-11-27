import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { Catalog, CatalogField, DocumentDetail, DocumentSummary, ExtractedCase, FieldResult, FieldType } from "../api/types";

/** The committed catalog, so tests follow it; field ids are picked by type, never written out. */
export const catalog: Catalog = JSON.parse(readFileSync(resolve(__dirname, "../../../catalog/field_catalog.json"), "utf8"));

export function fieldOfType(type: FieldType, nth = 0): CatalogField {
  const field = catalog.fields.filter((f) => f.type === type)[nth];
  if (!field) throw new Error(`No ${type} field in the catalog`);
  return field;
}

export const TEXT = "Patient: Mrs A.B., 54 years. Started drug on 2 March 2026. Rash 😀 on 4 March 2026. Age also given as 45.";

/** Code point offsets of a substring, as the backend writes them. */
export function span(quote: string) {
  const utf16 = TEXT.indexOf(quote);
  if (utf16 < 0) throw new Error(`"${quote}" not in TEXT`);
  const start = Array.from(TEXT.slice(0, utf16)).length;
  return { quote, start, end: start + Array.from(quote).length };
}

const date = fieldOfType("date");
const coded = fieldOfType("coded");
const integer = fieldOfType("integer");
const list = fieldOfType("string_list");

export const IDS = { date: date.id, coded: coded.id, integer: integer.id, list: list.id };

function firstCase(): ExtractedCase {
  const fields: Record<string, FieldResult> = {
    [date.id]: { status: "extracted", value: "2026-03-02", confidence: "high", provenance: [span("Started drug on 2 March 2026")] },
    [coded.id]: { status: "extracted", value: coded.options![0]!.code, confidence: "medium", provenance: [span("Mrs A.B.")] },
    [integer.id]: { status: "conflict", note: "54 or 45" },
    [list.id]: { status: "extracted", value: ["rash"], confidence: "low", provenance: [span("Rash 😀 on 4 March 2026")] },
  };
  return { case_index: 0, label: "Mrs A.B.", fields };
}

export const documents: DocumentSummary[] = [
  { id: "D01", title: "Two patients", caseCount: 2, flags: ["multiple_patients"], hasOutput: true, hasText: true },
  { id: "D06", title: "Quality complaint", caseCount: 0, flags: ["no_adverse_event"], hasOutput: true, hasText: true },
  { id: "S03", title: "S03", caseCount: 0, flags: ["unsupported_format"], hasOutput: true, hasText: false },
];

export const details: Record<string, DocumentDetail> = {
  D01: {
    id: "D01",
    title: "Two patients",
    text: TEXT,
    extraction: {
      document_id: "D01",
      catalog_version: "1.0",
      model: "test/model",
      flags: ["multiple_patients"],
      cases: [firstCase(), { case_index: 1, label: "Infant", fields: {} }],
    },
  },
  D06: {
    id: "D06",
    title: "Quality complaint",
    text: "Capsules were discoloured. No patient received them.",
    extraction: { document_id: "D06", catalog_version: "1.0", flags: ["no_adverse_event"], cases: [] },
  },
  S03: {
    id: "S03",
    title: "S03",
    text: null,
    extraction: { document_id: "S03", catalog_version: "1.0", flags: ["unsupported_format"], cases: [] },
  },
};

/** A fetch stand-in serving the three GET endpoints from the data above. */
export function fakeFetch(overrides: Partial<Record<string, () => Response>> = {}) {
  return async (input: RequestInfo | URL): Promise<Response> => {
    const path = String(input);
    const override = overrides[path];
    if (override) return override();
    if (path === "/api/catalog") return json(catalog);
    if (path === "/api/documents") return json(documents);
    const id = decodeURIComponent(path.replace("/api/documents/", ""));
    const detail = details[id];
    return detail ? json(detail) : json({ error: `No document with id '${id}'` }, 404);
  };
}

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}
