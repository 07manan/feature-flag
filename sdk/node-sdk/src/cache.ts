class CacheEntry<T> {
  public readonly value: T;
  public readonly createdAt: number;

  constructor(value: T) {
    this.value = value;
    this.createdAt = Date.now();
  }

  isExpired(ttlMs: number): boolean {
    return Date.now() - this.createdAt > ttlMs;
  }
}

export class LocalCache<T> {
  private readonly store = new Map<string, CacheEntry<T>>();
  private readonly ttlMs: number;
  private cleanupTimer: NodeJS.Timeout | null = null;

  private static readonly CLEANUP_INTERVAL_MS = 30_000;

  constructor(ttlMs: number) {
    this.ttlMs = ttlMs;
    this.startCleanup();
  }

  get(key: string): T | undefined {
    const entry = this.store.get(key);
    if (!entry) return undefined;

    if (entry.isExpired(this.ttlMs)) {
      this.store.delete(key);
      return undefined;
    }

    return entry.value;
  }

  set(key: string, value: T): void {
    this.store.set(key, new CacheEntry(value));
  }

  delete(key: string): void {
    this.store.delete(key);
  }

  clear(): void {
    this.store.clear();
  }

  get size(): number {
    return this.store.size;
  }

  shutdown(): void {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer);
      this.cleanupTimer = null;
    }
    this.clear();
  }

  private startCleanup(): void {
    this.cleanupTimer = setInterval(() => {
      for (const [key, entry] of this.store) {
        if (entry.isExpired(this.ttlMs)) {
          this.store.delete(key);
        }
      }
    }, LocalCache.CLEANUP_INTERVAL_MS);

    // Unreferenced so it won't prevent the Node.js process from exiting
    this.cleanupTimer.unref();
  }

  /** Key format: "flagKey:userId" (userId defaults to "null"). */
  static buildKey(flagKey: string, userId?: string): string {
    return `${flagKey}:${userId ?? "null"}`;
  }
}
