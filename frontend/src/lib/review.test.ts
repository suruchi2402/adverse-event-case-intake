import { describe, expect, it } from "vitest";
import { catalog, details } from "../test/data";
import { countByRisk, groupBySection, matchesFilter, riskOf } from "./review";

describe("riskOf", () => {
  it("ranks status before confidence", () => {
    expect(riskOf({ status: "conflict" })).toBe("conflict");
    expect(riskOf({ status: "unknown" })).toBe("unknown");
    expect(riskOf({ status: "extracted", confidence: "low", value: "x" })).toBe("low");
    expect(riskOf({ status: "extracted", value: "x" })).toBe("high");
  });
});

describe("groupBySection", () => {
  const kase = details.D01!.extraction!.cases[0]!;
  const groups = groupBySection(catalog, kase);

  it("follows catalog section and field order and shows every field", () => {
    expect(groups.map((g) => g.id)).toEqual(catalog.sections.map((s) => s.id));
    const ids = groups.flatMap((g) => g.entries.map((e) => e.field.id));
    expect(new Set(ids)).toEqual(new Set(catalog.fields.map((f) => f.id)));
    for (const group of groups) {
      const order = catalog.fields.filter((f) => f.section === group.id).map((f) => f.id);
      expect(group.entries.map((e) => e.field.id)).toEqual(order);
    }
  });

  it("treats fields missing from the output as unknown and counts risks", () => {
    const counts = countByRisk(groups);
    expect(counts.conflict).toBe(1);
    expect(counts.low).toBe(1);
    expect(counts.medium).toBe(1);
    expect(counts.high).toBe(1);
    expect(counts.unknown).toBe(catalog.fields.length - 4);
  });

  it("filters the risky fields", () => {
    expect(matchesFilter("conflict", "review")).toBe(true);
    expect(matchesFilter("high", "review")).toBe(false);
    expect(matchesFilter("unknown", "unknown")).toBe(true);
    expect(matchesFilter("high", "all")).toBe(true);
  });
});
