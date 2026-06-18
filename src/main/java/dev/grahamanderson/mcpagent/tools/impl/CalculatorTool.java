package dev.grahamanderson.mcpagent.tools.impl;

import dev.grahamanderson.mcpagent.tools.Tool;
import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Arithmetic over two operands. Demonstrates a tool that can return a handled error. */
@Component
public class CalculatorTool implements Tool {

    @Override
    public String name() {
        return "calculate";
    }

    @Override
    public String description() {
        return "Evaluate a basic arithmetic operation (add, subtract, multiply, divide) on two numbers.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "a", Map.of("type", "number"),
                        "b", Map.of("type", "number"),
                        "op", Map.of("type", "string", "enum", new String[]{"add", "subtract", "multiply", "divide"})
                ),
                "required", new String[]{"a", "b", "op"}
        );
    }

    @Override
    public ToolResult invoke(Map<String, Object> args) {
        double a = toDouble(args.get("a"));
        double b = toDouble(args.get("b"));
        String op = String.valueOf(args.get("op"));

        double result;
        switch (op) {
            case "add" -> result = a + b;
            case "subtract" -> result = a - b;
            case "multiply" -> result = a * b;
            case "divide" -> {
                if (b == 0) {
                    return ToolResult.error("Cannot divide by zero");
                }
                result = a / b;
            }
            default -> {
                return ToolResult.error("Unsupported operation: " + op);
            }
        }
        return ToolResult.ok(Map.of("a", a, "b", b, "op", op, "result", result));
    }

    private static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
