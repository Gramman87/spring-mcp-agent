// SSE consumer for the Spring Boot streaming agent.
// EventSource handles reconnection and event-type dispatch over GET /api/agent/chat.

// Same-origin: in production the Spring Boot app serves this bundle, so the API
// is reached via relative paths. In dev, the Vite proxy (see vite.config.ts)
// forwards /api and /v1 to the backend on :8080.
const API_BASE = "";

export interface ToolCall {
  tool: string;
  args: Record<string, unknown>;
}

export interface AgentHandlers {
  onToolCall?: (call: ToolCall) => void;
  onToolResult?: (result: Record<string, unknown>) => void;
  onToken?: (token: string) => void;
  onDone?: () => void;
  onError?: (err: unknown) => void;
}

/**
 * Opens an SSE stream for one agent turn and dispatches each named event.
 * Returns a cancel function that closes the stream.
 */
export function streamChat(message: string, handlers: AgentHandlers): () => void {
  const url = `${API_BASE}/api/agent/chat?message=${encodeURIComponent(message)}`;
  const source = new EventSource(url);

  source.addEventListener("tool_call", (e) =>
    handlers.onToolCall?.(JSON.parse((e as MessageEvent).data)),
  );
  source.addEventListener("tool_result", (e) =>
    handlers.onToolResult?.(JSON.parse((e as MessageEvent).data)),
  );
  source.addEventListener("token", (e) =>
    handlers.onToken?.((e as MessageEvent).data),
  );
  source.addEventListener("done", () => {
    handlers.onDone?.();
    source.close();
  });
  source.onerror = (err) => {
    handlers.onError?.(err);
    source.close();
  };

  return () => source.close();
}
