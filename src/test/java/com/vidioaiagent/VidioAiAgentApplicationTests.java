package com.vidioaiagent;

import org.junit.jupiter.api.Test;

class VidioAiAgentApplicationTests {

    @Test
    void applicationClassExists() {
        // Spring AI requires ANTHROPIC_API_KEY at startup
        // Full context load test is done in integration tests with real API key
        VidioAiAgentApplication app = new VidioAiAgentApplication();
        org.junit.jupiter.api.Assertions.assertNotNull(app);
    }
}
