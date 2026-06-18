package dev.grahamanderson.mcpagent.agent;

import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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
    private static final long TOKEN_DELAY_MS = 30;

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
                emitter.send(SseEmitter.event().name("tool_result")
                        .data(Map.of("tool", plan.toolName(), "ok", result.ok(),
                                "content", result.content(), "error", result.error())));
            }

            String answer = agent.compose(message, plan, result);
            for (String token : answer.split("(?<=\\s)")) {
                emitter.send(SseEmitter.event().name("token").data(token));
                Thread.sleep(TOKEN_DELAY_MS);
            }

            emitter.send(SseEmitter.event().name("done").data(Map.of("usedTool", plan.usesTool())));
            emitter.complete();
        } catch (IOException | InterruptedException e) {
            emitter.completeWithError(e);
        }
    }
}
