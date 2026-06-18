package dev.grahamanderson.mcpagent.agent;

import dev.grahamanderson.mcpagent.tools.ToolRegistry;
import dev.grahamanderson.mcpagent.tools.ToolResult;
import dev.grahamanderson.mcpagent.tools.impl.CalculatorTool;
import dev.grahamanderson.mcpagent.tools.impl.CustomerLookupTool;
import dev.grahamanderson.mcpagent.tools.impl.WeatherTool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentServiceTest {

    private final AgentService agent = new AgentService(new ToolRegistry(
            List.of(new CalculatorTool(), new CustomerLookupTool(), new WeatherTool())));

    @Test
    void routesArithmeticToCalculator() {
        AgentService.Plan plan = agent.plan("what is 12 * 3?");

        assertTrue(plan.usesTool());
        assertEquals("calculate", plan.toolName());
        assertEquals("multiply", plan.args().get("op"));
    }

    @Test
    void routesCustomerQueryToLookup() {
        AgentService.Plan plan = agent.plan("show me the customer account C-1001");

        assertEquals("lookup_customer", plan.toolName());
        assertEquals("C-1001", plan.args().get("id"));
    }

    @Test
    void composesGroundedAnswerFromToolResult() {
        AgentService.Plan plan = agent.plan("weather in Boulder");
        ToolResult result = agent.invoke(plan);

        String answer = agent.compose("weather in Boulder", plan, result);

        assertTrue(answer.contains("Boulder"));
        assertTrue(answer.contains("°F"));
    }

    @Test
    void answersDirectlyWhenNoToolMatches() {
        AgentService.Plan plan = agent.plan("hello there");

        assertFalse(plan.usesTool());
        assertTrue(agent.compose("hello there", plan, null).toLowerCase().contains("tool"));
    }
}
