import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, resolve } from "node:path";
import { describe, expect, it } from "vitest";
import { catalog } from "./test/data";

/** The brief: no field id appears in the UI source. Everything field-specific comes from GET /api/catalog. */
function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) return name === "test" ? [] : sourceFiles(path);
    return /\.(ts|tsx|css)$/.test(name) && !/\.test\.tsx?$/.test(name) ? [path] : [];
  });
}

describe("UI source", () => {
  it("names no catalog field id", () => {
    const offenders: string[] = [];
    for (const file of sourceFiles(resolve(__dirname))) {
      const text = readFileSync(file, "utf8");
      for (const field of catalog.fields) {
        if (new RegExp(`\\b${field.id}\\b`).test(text)) offenders.push(`${field.id} in ${file}`);
      }
    }
    expect(offenders).toEqual([]);
  });
});
