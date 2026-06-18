package dev.grahamanderson.mcpagent.agent;

import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Streaming agent (the MCP consumer). Emits the agent's reasoning as Server-Sent
 * Events so a browser can render tool calls and tokens in real time:
 * {@code status -> tool_call -> tool_result -> token* -> done}.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final long STREAM_TIMEOUT_MS = 60_000;

    private final AgentService agent;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentController(AgentService agent) {
        this.agent = agent;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam("message") String message) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        executor.execute(() -> runAgent(message, emitter));
        return emitter;
    }

    private void runAgent(String message, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("status").data(Map.of("state", "planning")));

            AgentService.Plan plan = agent.plan(message);
            ToolResult result = null;

            if (plan.usesTool()) {
                emitter.send(SseEmitter.event().name("tool_call")
                        .data(Map.of("tool", plan.toolName(), "args", plan.args())));
                result = agent.invoke(plan);
                // Build with a null-tolerant map: on success error() is null, on
                // failure content() is null — Map.of() would throw NPE on either.
                Map<String, Object> resultPayload = new HashMap<>();
                resultPayload.put("tool", plan.toolName());
                resultPayload.put("ok", result.ok());
                resultPayload.put("content", result.content());
                resultPayload.put("error", result.error());
                emitter.send(SseEmitter.event().name("tool_result").data(resultPayload));
            }

            String answer = agent.compose(message, plan, result);
            TokenStream.emit(answer, token ->
                    emitter.send(SseEmitter.event().name("token").data(token)));

            emitter.send(SseEmitter.event().name("done").data(Map.of("usedTool", plan.usesTool())));
            emitter.complete();
        } catch (Exception e) {
            // Any failure (I/O, interruption, serialization) must complete the
            // emitter — otherwise the SSE connection hangs until it times out.
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            emitter.completeWithError(e);
        }
    }
}
