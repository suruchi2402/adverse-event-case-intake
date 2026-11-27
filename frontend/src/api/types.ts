/**
 * Shapes of the backend API. Field ids, labels, sections, types and options always come from the catalog
 * at runtime; nothing here names a field.
 */

export type FieldType =
  | "string"
  | "integer"
  | "number"
  | "date"
  | "coded"
  | "coded_multi"
  | "string_list";

export interface CatalogOption {
  code: string;
  label: string;
}

export interface CatalogField {
  id: string;
  label: string;
  section: string;
  type: FieldType;
  options?: CatalogOption[];
  validation?: { pattern?: string; min?: number; max?: number };
  guidance?: string;
}

export interface CatalogSection {
  id: string;
  label: string;
}

export interface Catalog {
  catalog_version: string;
  sections: CatalogSection[];
  fields: CatalogField[];
}

export type FieldStatus = "extracted" | "unknown" | "conflict";
export type Confidence = "high" | "medium" | "low";

export interface Provenance {
  quote: string;
  /** Unicode code point offset, inclusive. */
  start: number;
  /** Unicode code point offset, exclusive. */
  end: number;
  page?: number;
}

export type FieldValue = string | number | string[] | null;

export interface FieldResult {
  status: FieldStatus;
  value?: FieldValue;
  confidence?: Confidence;
  provenance?: Provenance[];
  note?: string;
}

export interface ExtractedCase {
  case_index: number;
  label?: string;
  fields: Record<string, FieldResult>;
}

export interface Extraction {
  document_id: string;
  catalog_version: string;
  model?: string;
  flags?: string[];
  cases: ExtractedCase[];
}

export interface DocumentSummary {
  id: string;
  title: string;
  caseCount: number;
  flags: string[];
  hasOutput: boolean;
  hasText: boolean;
}

export interface DocumentDetail {
  id: string;
  title: string;
  text: string | null;
  extraction: Extraction | null;
}
