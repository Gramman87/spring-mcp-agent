package dev.grahamanderson.mcpagent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenAiCompatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsOpenAiShapedCompletionWhenNotStreaming() throws Exception {
        mockMvc.perform(post("/v1/chat/completions")
                        .contentType("application/json")
                        .content("{\"messages\":[{\"role\":\"user\",\"content\":\"What is 12 * 8?\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("chat.completion"))
                .andExpect(jsonPath("$.choices[0].message.role").value("assistant"))
                .andExpect(jsonPath("$.choices[0].message.content").value("The result of 12.0 multiply 8.0 is 96.0."))
                .andExpect(jsonPath("$.choices[0].finish_reason").value("stop"));
    }

    @Test
    void streamsOpenAiChunksWhenStreaming() throws Exception {
        // Regression: streaming must return the SseEmitter directly. Wrapping it in
        // a ResponseEntity from an Object-typed method previously yielded HTTP 500
        // ("No converter for SseEmitter") and never started async. asyncStarted()
        // therefore guards the bug. (Full-stream content + [DONE] is covered by the
        // end-to-end HTTP smoke test; MockMvc does not block on emitter completion
        // for an Object-declared return type, so it only buffers the first chunk.)
        MvcResult started = mockMvc.perform(post("/v1/chat/completions")
                        .contentType("application/json")
                        .content("{\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"Look up customer C-1001\"}]}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(body.contains("chat.completion.chunk"), "missing chunk objects: " + body);
        // Intermediate chunks must carry finish_reason: null per the OpenAI format.
        assertTrue(body.contains("\"finish_reason\":null"), "intermediate chunk should have null finish_reason: " + body);
    }
}
