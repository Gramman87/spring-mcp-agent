package dev.grahamanderson.mcpagent.tools.impl;

import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorToolTest {

    private final CalculatorTool tool = new CalculatorTool();

    @Test
    void addsTwoNumbers() {
        ToolResult result = tool.invoke(Map.of("a", 2, "b", 3, "op", "add"));

        assertTrue(result.ok());
        Map<?, ?> content = (Map<?, ?>) result.content();
        assertEquals(5.0, content.get("result"));
    }

    @Test
    void dividesTwoNumbers() {
        ToolResult result = tool.invoke(Map.of("a", 10, "b", 4, "op", "divide"));

        assertTrue(result.ok());
        assertEquals(2.5, ((Map<?, ?>) result.content()).get("result"));
    }

    @Test
    void returnsHandledErrorOnDivideByZero() {
        ToolResult result = tool.invoke(Map.of("a", 1, "b", 0, "op", "divide"));

        assertFalse(result.ok());
        assertNull(result.content());
        assertTrue(result.error().contains("divide by zero"));
    }
}
