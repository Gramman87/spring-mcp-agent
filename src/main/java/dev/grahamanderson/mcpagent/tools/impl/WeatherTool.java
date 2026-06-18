package dev.grahamanderson.mcpagent.tools.impl;

import dev.grahamanderson.mcpagent.tools.Tool;
import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Returns canned current-conditions for a city. Self-contained, no external API. */
@Component
public class WeatherTool implements Tool {

    private static final Map<String, Map<String, Object>> CONDITIONS = Map.of(
            "boulder", Map.of("city", "Boulder", "tempF", 68, "conditions", "Sunny"),
            "denver", Map.of("city", "Denver", "tempF", 71, "conditions", "Partly cloudy"),
            "evergreen", Map.of("city", "Evergreen", "tempF", 61, "conditions", "Clear")
    );

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public String description() {
        return "Get current weather conditions for a supported city (Boulder, Denver, Evergreen).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("city", Map.of("type", "string")),
                "required", new String[]{"city"}
        );
    }

    @Override
    public ToolResult invoke(Map<String, Object> args) {
        String city = String.valueOf(args.get("city")).trim().toLowerCase();
        Map<String, Object> record = CONDITIONS.get(city);
        if (record == null) {
            return ToolResult.error("No weather data for " + args.get("city"));
        }
        return ToolResult.ok(record);
    }
}
