package com.nikita.creditrisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Credit Risk Explanation Service - Main Application
 * 
 * Demonstrates:
 * - Spring AI (Chunking, Vector, Embedding, Prompt, RAG, Response Formatting)
 * - Error Handling & Fallback Response
 * - LangChain (Sequential Chain Pattern)
 * - LangGraph (State Graph with Conditional Routing)
 * - MCP (Model Context Protocol / Function Calling)
 */
@SpringBootApplication
@EnableRetry  // Spring AI - Error Handling: enables @Retryable for automatic retry logic
public class CreditRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditRiskApplication.class, args);
    }
}
