package dev.grahamanderson.mcpagent.tools;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every {@link Tool} bean on the classpath. Backs both the MCP
 * producer surface and the in-process agent loop, so a tool is published and
 * callable from a single source of truth.
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<Tool> discovered) {
        for (Tool tool : discovered) {
            tools.put(tool.name(), tool);
        }
    }

    public List<Tool> all() {
        return List.copyOf(tools.values());
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    public Tool get(String name) {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return tool;
    }

    public ToolResult invoke(String name, Map<String, Object> args) {
        return get(name).invoke(args == null ? Map.of() : args);
    }
}
