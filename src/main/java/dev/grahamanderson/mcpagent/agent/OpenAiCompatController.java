package dev.grahamanderson.mcpagent.agent;

import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal OpenAI-compatible {@code /v1/chat/completions} endpoint. Lets any
 * OpenAI-style client (or the React UI) drive the same agent. Supports both a
 * single JSON completion and a streamed {@code text/event-stream} of delta
 * chunks terminated by {@code data: [DONE]}.
 */
@RestController
public class OpenAiCompatController {

    private static final long TOKEN_DELAY_MS = 30;

    private final AgentService agent;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public OpenAiCompatController(AgentService agent) {
        this.agent = agent;
    }

    @PostMapping("/v1/chat/completions")
    public Object completions(@RequestBody Map<String, Object> body) {
        String userMessage = lastUserMessage(body);
        String answer = runAgent(userMessage);
        boolean stream = Boolean.TRUE.equals(body.get("stream"));
        String id = "chatcmpl-" + Long.toHexString(System.nanoTime());

        if (!stream) {
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "object", "chat.completion",
                    "model", body.getOrDefault("model", "spring-mcp-agent"),
                    "choices", List.of(Map.of(
                            "index", 0,
                            "message", Map.of("role", "assistant", "content", answer),
                            "finish_reason", "stop"))));
        }

        // Return the SseEmitter directly. Wrapping it in a ResponseEntity from an
        // Object-typed method bypasses Spring's async emitter handler, which then
        // fails with "No converter for SseEmitter". Returning it bare lets the
        // emitter handler engage and set Content-Type: text/event-stream itself.
        SseEmitter emitter = new SseEmitter(60_000L);
        executor.execute(() -> streamChunks(emitter, id, answer));
        return emitter;
    }

    private void streamChunks(SseEmitter emitter, String id, String answer) {
        try {
            for (String token : answer.split("(?<=\\s)")) {
                emitter.send(chunk(id, Map.of("content", token), null));
                Thread.sleep(TOKEN_DELAY_MS);
            }
            emitter.send(chunk(id, Map.of(), "stop"));
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (IOException | InterruptedException e) {
            emitter.completeWithError(e);
        }
    }

    private static SseEmitter.SseEventBuilder chunk(String id, Map<String, Object> delta, String finishReason) {
        // HashMap so finish_reason can be JSON null on intermediate chunks, as the
        // OpenAI streaming format specifies (Map.of forbids null values).
        Map<String, Object> choice = new HashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finishReason);
        return SseEmitter.event().data(Map.of(
                "id", id,
                "object", "chat.completion.chunk",
                "choices", List.of(choice)));
    }

    private String runAgent(String message) {
        AgentService.Plan plan = agent.plan(message);
        ToolResult result = plan.usesTool() ? agent.invoke(plan) : null;
        return agent.compose(message, plan, result);
    }

    @SuppressWarnings("unchecked")
    private static String lastUserMessage(Map<String, Object> body) {
        Object messages = body.get("messages");
        if (messages instanceof List<?> list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i) instanceof Map<?, ?> m && "user".equals(m.get("role"))) {
                    return String.valueOf(m.get("content"));
                }
            }
        }
        return "";
    }
}
