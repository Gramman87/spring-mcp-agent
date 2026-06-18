package dev.grahamanderson.mcpagent.mcp;

import dev.grahamanderson.mcpagent.tools.Tool;
import dev.grahamanderson.mcpagent.tools.ToolRegistry;
import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP producer surface. Publishes the tool catalog for discovery and lets a
 * client invoke a tool by name — the HTTP analogue of an MCP server's
 * {@code tools/list} and {@code tools/call}. The same {@link ToolRegistry}
 * backs the in-process agent, so producer and consumer share one source.
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final ToolRegistry registry;

    public McpController(ToolRegistry registry) {
        this.registry = registry;
    }

    /** tools/list — discover available tools and their input schemas. */
    @GetMapping("/tools")
    public List<Map<String, Object>> listTools() {
        return registry.all().stream().map(McpController::descriptor).toList();
    }

    /** tools/call — invoke a tool by name with a JSON argument map. */
    @PostMapping("/tools/{name}/invoke")
    public ResponseEntity<ToolResult> invoke(@PathVariable String name,
                                             @RequestBody(required = false) Map<String, Object> args) {
        if (!registry.has(name)) {
            return ResponseEntity.notFound().build();
        }
        ToolResult result = registry.invoke(name, args);
        return ResponseEntity.ok(result);
    }

    private static Map<String, Object> descriptor(Tool tool) {
        return Map.of(
                "name", tool.name(),
                "description", tool.description(),
                "inputSchema", tool.inputSchema());
    }
}
