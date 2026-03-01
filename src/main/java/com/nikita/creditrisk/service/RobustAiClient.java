package com.nikita.creditrisk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * ROBUST AI CLIENT
 * A wrapper around Spring AI ChatClient to handle free-tier API rate limits.
 * Protects against HTTP 429 (Quota Exceeded / Too Many Requests) by introducing
 * deliberate delays between back-to-back calls and providing a fallback
 * mechanism.
 */
@Service
public class RobustAiClient {

    private static final Logger log = LoggerFactory.getLogger(RobustAiClient.class);
    private final ChatClient chatClient;
    private long lastCallTime = 0;

    // Minimum time to wait between API calls to avoid hitting rate limits (8
    // seconds)
    private static final long MIN_DELAY_MS = 8000;

    public RobustAiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Executes a prompt with rate limit protection and fallback.
     */
    public String call(String prompt, String fallbackResponse) {
        enforceRateLimit();

        try {
            log.info("🚀 Calling AI Model. Prompt length: {}", prompt.length());
            String response = chatClient.prompt().user(prompt).call().content();
            log.info("✅ AI call successful. Response length: {}", response != null ? response.length() : 0);
            return response;
        } catch (Exception e) {
            log.warn("⚠ AI call failed (likely 429 Rate Limit): {}", e.getMessage());
            log.warn("🔄 Using fallback response instead.");
            return fallbackResponse;
        }
    }

    /**
     * Executes a prompt with function calling.
     */
    public String callWithFunctions(String prompt, String fallbackResponse, String... functions) {
        enforceRateLimit();

        try {
            log.info("🚀 Calling AI Model with Functions. Prompt length: {}", prompt.length());
            String response = chatClient.prompt()
                    .user(prompt)
                    .functions(functions)
                    .call()
                    .content();
            return response;
        } catch (Exception e) {
            log.warn("⚠ AI function call failed: {}. Using fallback.", e.getMessage());
            return fallbackResponse;
        }
    }

    private synchronized void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long timeSinceLastCall = now - lastCallTime;

        if (timeSinceLastCall < MIN_DELAY_MS) {
            long sleepTime = MIN_DELAY_MS - timeSinceLastCall;
            log.info("⏳ Enforcing rate limit: Waiting {} ms before next AI call...", sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTime = System.currentTimeMillis();
    }
}
