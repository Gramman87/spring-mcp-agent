package dev.grahamanderson.mcpagent.agent;

import dev.grahamanderson.mcpagent.tools.ToolRegistry;
import dev.grahamanderson.mcpagent.tools.ToolResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "brain" of the agent. Decides which tool (if any) a user message needs,
 * then composes a grounded natural-language answer from the tool result.
 *
 * <p>The planner here is deterministic so the demo runs with no API key. The
 * seam is intentional: swap {@link #plan} and {@link #compose} for a Claude /
 * OpenAI tool-use call and the rest of the streaming pipeline is unchanged.
 */
@Service
public class AgentService {

    /** A decision to call a tool, or {@code none()} to answer directly. */
    public record Plan(String toolName, Map<String, Object> args) {
        public static Plan none() {
            return new Plan(null, Map.of());
        }

        public boolean usesTool() {
            return toolName != null;
        }
    }

    private static final Pattern CUSTOMER_ID = Pattern.compile("c-?(\\d{3,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARITHMETIC =
            Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/x])\\s*(-?\\d+(?:\\.\\d+)?)");

    private final ToolRegistry registry;

    public AgentService(ToolRegistry registry) {
        this.registry = registry;
    }

    public ToolRegistry registry() {
        return registry;
    }

    /** Route a user message to a tool call, or {@link Plan#none()}. */
    public Plan plan(String message) {
        String text = message == null ? "" : message.toLowerCase();

        Matcher math = ARITHMETIC.matcher(text);
        if (math.find()) {
            String op = switch (math.group(2)) {
                case "+" -> "add";
                case "-" -> "subtract";
                case "/" -> "divide";
                default -> "multiply"; // * or x
            };
            return new Plan("calculate", Map.of(
                    "a", Double.parseDouble(math.group(1)),
                    "b", Double.parseDouble(math.group(3)),
                    "op", op));
        }

        Matcher customer = CUSTOMER_ID.matcher(text);
        if (customer.find() && (text.contains("customer") || text.contains("account") || text.contains("arr"))) {
            return new Plan("lookup_customer", Map.of("id", "C-" + customer.group(1)));
        }

        if (text.contains("weather")) {
            for (String city : new String[]{"boulder", "denver", "evergreen"}) {
                if (text.contains(city)) {
                    return new Plan("get_weather", Map.of("city", city));
                }
            }
        }

        return Plan.none();
    }

    public ToolResult invoke(Plan plan) {
        return registry.invoke(plan.toolName(), plan.args());
    }

    /** Build the final natural-language answer the agent streams back to the user. */
    public String compose(String message, Plan plan, ToolResult result) {
        if (!plan.usesTool()) {
            return "I can call tools to do math, look up customer accounts (try C-1001), "
                    + "or check the weather in Boulder, Denver, or Evergreen. What would you like?";
        }
        if (result != null && !result.ok()) {
            return "I tried the " + plan.toolName() + " tool but it returned an error: " + result.error() + ".";
        }
        Object content = result == null ? null : result.content();
        return switch (plan.toolName()) {
            case "calculate" -> {
                Map<?, ?> r = (Map<?, ?>) content;
                yield "The result of " + r.get("a") + " " + r.get("op") + " " + r.get("b")
                        + " is " + r.get("result") + ".";
            }
            case "lookup_customer" -> {
                Map<?, ?> r = (Map<?, ?>) content;
                yield r.get("name") + " (" + r.get("id") + ") is on the " + r.get("plan")
                        + " plan at $" + r.get("arr") + " ARR, status: " + r.get("status") + ".";
            }
            case "get_weather" -> {
                Map<?, ?> r = (Map<?, ?>) content;
                yield "It's currently " + r.get("tempF") + "°F and " + r.get("conditions")
                        + " in " + r.get("city") + ".";
            }
            default -> "Done. Tool " + plan.toolName() + " returned: " + content;
        };
    }
}
