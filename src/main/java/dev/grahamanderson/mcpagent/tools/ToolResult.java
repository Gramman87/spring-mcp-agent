package dev.grahamanderson.mcpagent.tools;

/**
 * Outcome of a tool invocation. {@code ok} distinguishes a successful result
 * ({@code content} populated) from a handled error ({@code error} populated).
 */
public record ToolResult(boolean ok, Object content, String error) {

    public static ToolResult ok(Object content) {
        return new ToolResult(true, content, null);
    }

    public static ToolResult error(String message) {
        return new ToolResult(false, null, message);
    }
}
