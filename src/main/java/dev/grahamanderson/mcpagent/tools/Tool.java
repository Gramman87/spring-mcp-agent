package dev.grahamanderson.mcpagent.tools;

import java.util.Map;

/**
 * A single capability the agent can call. Each Tool is published on the MCP
 * producer surface (see McpController) and invoked by the agent loop.
 */
public interface Tool {

    /** Stable, unique tool name (e.g. "calculate"). */
    String name();

    /** Human-readable description surfaced during tool discovery. */
    String description();

    /** JSON-Schema-style description of the accepted arguments. */
    Map<String, Object> inputSchema();

    /** Run the tool against the supplied arguments. */
    ToolResult invoke(Map<String, Object> args);
}
