import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { LocalCache } from "../cache.js";

describe("LocalCache", () => {
  let cache: LocalCache<string>;

  beforeEach(() => {
    vi.useFakeTimers();
    cache = new LocalCache<string>(1000); // 1 second TTL
  });

  afterEach(() => {
    cache.shutdown();
    vi.useRealTimers();
  });

  it("should store and retrieve a value", () => {
    cache.set("key1", "value1");
    expect(cache.get("key1")).toBe("value1");
  });

  it("should return undefined for a missing key", () => {
    expect(cache.get("nonexistent")).toBeUndefined();
  });

  it("should return undefined and remove an expired entry", () => {
    cache.set("key1", "value1");
    expect(cache.get("key1")).toBe("value1");

    // Advance time past TTL
    vi.advanceTimersByTime(1100);

    expect(cache.get("key1")).toBeUndefined();
    expect(cache.size).toBe(0);
  });

  it("should return the value if TTL has not elapsed", () => {
    cache.set("key1", "value1");

    vi.advanceTimersByTime(500); // half of TTL
    expect(cache.get("key1")).toBe("value1");
  });

  it("should delete a specific key", () => {
    cache.set("key1", "value1");
    cache.set("key2", "value2");

    cache.delete("key1");

    expect(cache.get("key1")).toBeUndefined();
    expect(cache.get("key2")).toBe("value2");
  });

  it("should clear all entries", () => {
    cache.set("key1", "value1");
    cache.set("key2", "value2");

    cache.clear();

    expect(cache.size).toBe(0);
    expect(cache.get("key1")).toBeUndefined();
    expect(cache.get("key2")).toBeUndefined();
  });

  it("should overwrite an existing key", () => {
    cache.set("key1", "value1");
    cache.set("key1", "value2");

    expect(cache.get("key1")).toBe("value2");
  });

  it("should prune expired entries during background cleanup", () => {
    cache.set("key1", "value1");
    cache.set("key2", "value2");

    // Advance to just before cleanup fires (cleanup at 30s, TTL is 1s)
    vi.advanceTimersByTime(29_000);

    // Add a fresh entry right before cleanup fires
    cache.set("key3", "value3");

    // Advance 1s more to trigger the 30s cleanup interval
    vi.advanceTimersByTime(1_000);

    // key1 and key2 should be pruned (>1s old), key3 should survive (<1s old)
    expect(cache.size).toBe(1);
    expect(cache.get("key3")).toBe("value3");
  });

  it("should report correct size", () => {
    expect(cache.size).toBe(0);

    cache.set("a", "1");
    cache.set("b", "2");
    expect(cache.size).toBe(2);
  });

  describe("buildKey", () => {
    it("should build a key with userId", () => {
      expect(LocalCache.buildKey("my-flag", "user-42")).toBe("my-flag:user-42");
    });

    it("should build a key with 'null' when userId is undefined", () => {
      expect(LocalCache.buildKey("my-flag")).toBe("my-flag:null");
    });

    it("should build a key with 'null' when userId is explicitly undefined", () => {
      expect(LocalCache.buildKey("my-flag", undefined)).toBe("my-flag:null");
    });
  });
});
