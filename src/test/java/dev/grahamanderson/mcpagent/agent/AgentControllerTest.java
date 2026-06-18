package dev.grahamanderson.mcpagent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void streamsToolResultAndAnswerForSuccessfulToolCall() throws Exception {
        MvcResult started = mockMvc.perform(
                        get("/api/agent/chat").param("message", "Look up customer C-1001"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Regression: a successful tool call has error() == null. The tool_result
        // event must still serialize and the answer must stream — a Map.of() with a
        // null value would throw NPE, leaving the stream stuck after tool_call.
        assertTrue(body.contains("tool_call"), "missing tool_call event: " + body);
        assertTrue(body.contains("tool_result"), "missing tool_result event: " + body);
        assertTrue(body.contains("Northwind Traders"), "missing composed answer: " + body);
    }

    @Test
    void streamsDirectAnswerWhenNoToolMatches() throws Exception {
        MvcResult started = mockMvc.perform(
                        get("/api/agent/chat").param("message", "hello there"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains("done"), "stream should complete with a done event: " + body);
    }
}
