export async function retry<T>(
  fn: () => Promise<T>,
  attempts: number = 3,
  backoffMs: number = 200,
): Promise<T> {
  let lastErr: unknown;

  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      return await fn();
    } catch (err: any) {
      lastErr = err;

      // If this was the last attempt, rethrow
      if (attempt === attempts) {
        throw err;
      }

      const status = err?.response?.status;
      const message = err?.message ?? "";

      const shouldRetry =
        (typeof status === "number" && status >= 500) ||
        message.includes("unknown_error");

      if (!shouldRetry) {
        throw err;
      }

      // backoff before next attempt
      await new Promise((res) => setTimeout(res, backoffMs * attempt));
    }
  }

  throw lastErr;
}
