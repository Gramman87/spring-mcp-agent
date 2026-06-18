package dev.grahamanderson.mcpagent.tools;

import dev.grahamanderson.mcpagent.tools.impl.CalculatorTool;
import dev.grahamanderson.mcpagent.tools.impl.WeatherTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    private final ToolRegistry registry =
            new ToolRegistry(List.of(new CalculatorTool(), new WeatherTool()));

    @Test
    void registersDiscoveredToolsByName() {
        assertTrue(registry.has("calculate"));
        assertTrue(registry.has("get_weather"));
        assertEquals(2, registry.all().size());
    }

    @Test
    void invokesToolThroughRegistry() {
        ToolResult result = registry.invoke("calculate", Map.of("a", 6, "b", 7, "op", "multiply"));

        assertTrue(result.ok());
        assertEquals(42.0, ((Map<?, ?>) result.content()).get("result"));
    }

    @Test
    void throwsOnUnknownTool() {
        assertThrows(IllegalArgumentException.class, () -> registry.get("does_not_exist"));
    }
}
