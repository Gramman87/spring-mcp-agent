import { useRef, useState } from "react";
import { streamChat, type ToolCall } from "./api";

interface Turn {
  user: string;
  toolCall?: ToolCall;
  toolOk?: boolean;
  answer: string;
  streaming: boolean;
}

const SUGGESTIONS = [
  "What is 12 * 8?",
  "Look up customer C-1001",
  "What's the weather in Boulder?",
];

export default function App() {
  const [input, setInput] = useState("");
  const [turns, setTurns] = useState<Turn[]>([]);
  const [busy, setBusy] = useState(false);
  const cancelRef = useRef<(() => void) | null>(null);

  function send(message: string) {
    const text = message.trim();
    if (!text || busy) return;

    setBusy(true);
    setInput("");
    const index = turns.length;
    setTurns((t) => [...t, { user: text, answer: "", streaming: true }]);

    const update = (patch: Partial<Turn>) =>
      setTurns((t) => t.map((turn, i) => (i === index ? { ...turn, ...patch } : turn)));

    cancelRef.current = streamChat(text, {
      onToolCall: (call) => update({ toolCall: call }),
      onToolResult: (result) => update({ toolOk: Boolean(result.ok) }),
      onToken: (token) => setTurns((t) =>
        t.map((turn, i) => (i === index ? { ...turn, answer: turn.answer + token } : turn)),
      ),
      onDone: () => {
        update({ streaming: false });
        setBusy(false);
      },
      onError: () => {
        update({ answer: "⚠️ Could not reach the agent. Is the backend running on :8080?", streaming: false });
        setBusy(false);
      },
    });
  }

  return (
    <div className="app">
      <header>
        <h1>Spring MCP Agent</h1>
        <p>Spring Boot tool server + streaming agent · React SSE client</p>
      </header>

      <div className="thread">
        {turns.length === 0 && (
          <div className="empty">
            <p>Ask the agent something. It discovers tools over the MCP surface and streams back.</p>
            <div className="suggestions">
              {SUGGESTIONS.map((s) => (
                <button key={s} onClick={() => send(s)}>{s}</button>
              ))}
            </div>
          </div>
        )}

        {turns.map((turn, i) => (
          <div key={i} className="turn">
            <div className="msg user">{turn.user}</div>
            {turn.toolCall && (
              <div className="tool">
                <span className="badge">tool</span>
                <code>{turn.toolCall.tool}({JSON.stringify(turn.toolCall.args)})</code>
                {turn.toolOk !== undefined && (
                  <span className={turn.toolOk ? "ok" : "err"}>{turn.toolOk ? "✓" : "✕"}</span>
                )}
              </div>
            )}
            <div className="msg agent">
              {turn.answer}
              {turn.streaming && <span className="cursor">▍</span>}
            </div>
          </div>
        ))}
      </div>

      <form
        className="composer"
        onSubmit={(e) => {
          e.preventDefault();
          send(input);
        }}
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about math, customer C-1001, or the weather…"
          disabled={busy}
        />
        <button type="submit" disabled={busy || !input.trim()}>Send</button>
      </form>
    </div>
  );
}
