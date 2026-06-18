package dev.grahamanderson.mcpagent.tools.impl;

import dev.grahamanderson.mcpagent.tools.Tool;
import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Looks up a customer record from an in-memory store, standing in for a backing service. */
@Component
public class CustomerLookupTool implements Tool {

    private static final Map<String, Map<String, Object>> CUSTOMERS = Map.of(
            "C-1001", Map.of("id", "C-1001", "name", "Northwind Traders", "plan", "Enterprise", "arr", 240000, "status", "active"),
            "C-1002", Map.of("id", "C-1002", "name", "Globex Corp", "plan", "Growth", "arr", 48000, "status", "active"),
            "C-1003", Map.of("id", "C-1003", "name", "Initech", "plan", "Starter", "arr", 9000, "status", "churn-risk")
    );

    @Override
    public String name() {
        return "lookup_customer";
    }

    @Override
    public String description() {
        return "Fetch a customer account record (plan, ARR, status) by customer id, e.g. C-1001.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("id", Map.of("type", "string")),
                "required", new String[]{"id"}
        );
    }

    @Override
    public ToolResult invoke(Map<String, Object> args) {
        String id = String.valueOf(args.get("id")).trim().toUpperCase();
        Map<String, Object> record = CUSTOMERS.get(id);
        if (record == null) {
            return ToolResult.error("No customer found with id " + id);
        }
        return ToolResult.ok(record);
    }
}
