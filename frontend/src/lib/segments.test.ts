import { describe, expect, it } from "vitest";
import { codePointIndexer, segmentText } from "./segments";

describe("codePointIndexer", () => {
  it("maps code point offsets to UTF-16 indexes past astral characters", () => {
    const text = "a😀b";
    const toUtf16 = codePointIndexer(text);
    expect(toUtf16(0)).toBe(0);
    expect(toUtf16(1)).toBe(1);
    expect(toUtf16(2)).toBe(3);
    expect(text.slice(toUtf16(2), toUtf16(3))).toBe("b");
  });

  it("clamps offsets outside the text", () => {
    const toUtf16 = codePointIndexer("abc");
    expect(toUtf16(99)).toBe(3);
    expect(toUtf16(-1)).toBe(0);
  });
});

describe("segmentText", () => {
  it("cuts at every boundary and keeps overlapping owners", () => {
    const segments = segmentText("0123456789", [
      { start: 2, end: 6, fieldId: "a" },
      { start: 4, end: 8, fieldId: "b" },
    ]);
    expect(segments.map((s) => [s.text, s.fieldIds])).toEqual([
      ["01", []],
      ["23", ["a"]],
      ["45", ["a", "b"]],
      ["67", ["b"]],
      ["89", []],
    ]);
  });

  it("ignores spans outside the text and reassembles the text exactly", () => {
    const text = "hello world";
    const segments = segmentText(text, [{ start: 6, end: 99, fieldId: "x" }, { start: 0, end: 5, fieldId: "y" }]);
    expect(segments.map((s) => s.text).join("")).toBe(text);
    expect(segments.filter((s) => s.fieldIds.length > 0)).toHaveLength(1);
  });
});
