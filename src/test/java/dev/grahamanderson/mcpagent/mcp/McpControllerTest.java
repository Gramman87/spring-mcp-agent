package dev.grahamanderson.mcpagent.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsToolsForDiscovery() throws Exception {
        mockMvc.perform(get("/api/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("calculate")))
                .andExpect(jsonPath("$[*].name", hasItem("lookup_customer")));
    }

    @Test
    void invokesToolByName() throws Exception {
        mockMvc.perform(post("/api/mcp/tools/calculate/invoke")
                        .contentType("application/json")
                        .content("{\"a\":8,\"b\":2,\"op\":\"divide\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.content.result").value(4.0));
    }

    @Test
    void returnsNotFoundForUnknownTool() throws Exception {
        mockMvc.perform(post("/api/mcp/tools/nope/invoke")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestForMalformedArguments() throws Exception {
        // Non-numeric operand: a client error, not a 500.
        mockMvc.perform(post("/api/mcp/tools/calculate/invoke")
                        .contentType("application/json")
                        .content("{\"a\":\"not-a-number\",\"b\":2,\"op\":\"add\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Invalid arguments")));
    }

    @Test
    void treatsDivideByZeroAsHandledToolError() throws Exception {
        // Valid arguments, tool-level failure: still a 200 with ok == false.
        mockMvc.perform(post("/api/mcp/tools/calculate/invoke")
                        .contentType("application/json")
                        .content("{\"a\":1,\"b\":0,\"op\":\"divide\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(false));
    }
}
