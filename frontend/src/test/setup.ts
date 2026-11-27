import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// jsdom has no layout, so scrolling is a no-op that tests can observe.
Element.prototype.scrollIntoView = function scrollIntoView() {};

afterEach(() => {
  cleanup();
  window.location.hash = "";
});
