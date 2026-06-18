package dev.grahamanderson.mcpagent.agent;

import java.io.IOException;

/**
 * Splits a composed answer into whitespace-preserving tokens and emits each with
 * a fixed pacing delay, simulating token-by-token model output. Shared by the
 * SSE agent stream and the OpenAI-compatible stream so both pace identically.
 */
final class TokenStream {

    /** Delay between emitted tokens, in milliseconds. */
    static final long DELAY_MS = 30;

    /** Sends a single token to a transport (e.g. an SSE emitter). */
    @FunctionalInterface
    interface TokenSink {
        void send(String token) throws IOException;
    }

    private TokenStream() {
    }

    static void emit(String answer, TokenSink sink) throws IOException, InterruptedException {
        for (String token : answer.split("(?<=\\s)")) {
            sink.send(token);
            Thread.sleep(DELAY_MS);
        }
    }
}
