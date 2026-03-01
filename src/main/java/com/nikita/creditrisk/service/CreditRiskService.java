package com.nikita.creditrisk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikita.creditrisk.langchain.CreditRiskChain;
import com.nikita.creditrisk.langgraph.GraphState;
import com.nikita.creditrisk.langgraph.RiskAssessmentGraph;
import com.nikita.creditrisk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * MAIN ORCHESTRATION SERVICE
 * 
 * Coordinates all components:
 * - Spring AI (Prompt, RAG, Embedding, Vector)
 * - LangChain (Sequential Chain)
 * - LangGraph (State Graph)
 * - MCP (Function Calling)
 * - Error Handling & Fallback
 */
@Service
public class CreditRiskService {

    private static final Logger log = LoggerFactory.getLogger(CreditRiskService.class);

    private final CustomerDataService customerDataService;
    private final PromptService promptService;
    private final FallbackService fallbackService;
    private final CreditRiskChain creditRiskChain;
    private final RiskAssessmentGraph riskAssessmentGraph;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public CreditRiskService(CustomerDataService customerDataService,
                              PromptService promptService,
                              FallbackService fallbackService,
                              CreditRiskChain creditRiskChain,
                              RiskAssessmentGraph riskAssessmentGraph,
                              ChatClient.Builder chatClientBuilder) {
        this.customerDataService = customerDataService;
        this.promptService = promptService;
        this.fallbackService = fallbackService;
        this.creditRiskChain = creditRiskChain;
        this.riskAssessmentGraph = riskAssessmentGraph;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Main endpoint: Generate full credit risk explanation.
     * Uses Spring AI RAG + Prompt Engineering + Response Formatting.
     */
    public RiskExplanationResponse explainRisk(String customerId) {
        log.info("==== Starting Credit Risk Explanation for {} ====", customerId);

        CustomerProfile customer = customerDataService.getCustomerProfile(customerId);
        CreditRiskScore score = customerDataService.getCreditScore(customerId);

        if (customer == null || score == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }

        try {
            // Use Spring AI (RAG + Prompt + Response Formatting)
            String aiResponse = promptService.generateRiskExplanation(customer, score);
            RiskExplanationResponse response = parseAIResponse(aiResponse, customerId, score);
            response.setProcessingMode("AI");
            response.setTimestamp(LocalDateTime.now().toString());
            return response;
        } catch (Exception e) {
            log.warn("AI explanation failed, using fallback: {}", e.getMessage());
            String fallbackJson = fallbackService.generateFallbackExplanation(customer, score);
            RiskExplanationResponse response = parseAIResponse(fallbackJson, customerId, score);
            response.setProcessingMode("FALLBACK");
            response.setTimestamp(LocalDateTime.now().toString());
            return response;
        }
    }

    /**
     * LangChain endpoint: Execute sequential chain analysis.
     */
    public Map<String, Object> explainWithLangChain(String customerId) {
        validateCustomer(customerId);
        log.info("==== LangChain Analysis for {} ====", customerId);
        return creditRiskChain.executeChain(customerId);
    }

    /**
     * LangGraph endpoint: Execute state graph workflow.
     */
    public Map<String, Object> explainWithLangGraph(String customerId) {
        validateCustomer(customerId);
        log.info("==== LangGraph Analysis for {} ====", customerId);

        GraphState state = riskAssessmentGraph.execute(customerId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerId", state.getCustomerId());
        result.put("riskLevel", state.getRiskLevel());
        result.put("creditScore", state.getCreditRiskScore() != null ? state.getCreditRiskScore().getScore() : 0);
        result.put("complianceStatus", state.getComplianceStatus());
        result.put("explanation", state.getExplanation());
        result.put("recommendations", state.getRecommendations());
        result.put("visitedNodes", state.getVisitedNodes());
        result.put("metadata", state.getMetadata());
        result.put("errors", state.getErrors());
        result.put("processingMode", "LANGGRAPH");
        return result;
    }

    /**
     * MCP endpoint: Use function calling to let AI query tools.
     */
    public String explainWithMCP(String customerId) {
        validateCustomer(customerId);
        log.info("==== MCP Function Calling Analysis for {} ====", customerId);

        try {
            String response = chatClient.prompt()
                .user("Analyze the credit risk for customer " + customerId + ". "
                    + "First look up their credit score, then get their full profile, "
                    + "then analyze their risk factors, and finally search for relevant "
                    + "RBI policies. Provide a comprehensive risk assessment report.")
                .functions("lookupCreditScore", "getCustomerProfile",
                          "analyzeRiskFactors", "searchRBIPolicies")
                .call()
                .content();

            return response;
        } catch (Exception e) {
            log.warn("MCP analysis failed: {}", e.getMessage());
            CreditRiskScore score = customerDataService.getCreditScore(customerId);
            CustomerProfile customer = customerDataService.getCustomerProfile(customerId);
            return fallbackService.generateFallbackExplanation(customer, score);
        }
    }

    /**
     * Get compliance check result.
     */
    public String getComplianceCheck(String customerId) {
        CustomerProfile customer = customerDataService.getCustomerProfile(customerId);
        CreditRiskScore score = customerDataService.getCreditScore(customerId);
        validateCustomer(customerId);
        return promptService.generateComplianceCheck(customer, score);
    }

    /**
     * Get recommendations.
     */
    public String getRecommendations(String customerId) {
        CustomerProfile customer = customerDataService.getCustomerProfile(customerId);
        CreditRiskScore score = customerDataService.getCreditScore(customerId);
        validateCustomer(customerId);
        return promptService.generateRecommendations(customer, score);
    }

    private void validateCustomer(String customerId) {
        if (customerDataService.getCustomerProfile(customerId) == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId
                + ". Valid IDs: " + customerDataService.getAllCustomerIds());
        }
    }

    /**
     * SPRING AI - RESPONSE FORMATTING
     * Parses the AI JSON response into structured RiskExplanationResponse.
     */
    private RiskExplanationResponse parseAIResponse(String jsonResponse, String customerId, CreditRiskScore score) {
        RiskExplanationResponse response = new RiskExplanationResponse();
        response.setCustomerId(customerId);
        response.setCreditScore(score.getScore());

        try {
            // Clean JSON (remove markdown code blocks if present)
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```json?\\s*", "").replaceAll("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(cleaned);

            response.setScoreInterpretation(getTextOrDefault(root, "scoreInterpretation", "Score: " + score.getScore()));
            response.setRiskLevel(getTextOrDefault(root, "riskLevel", score.getRiskLevel()));
            response.setComplianceStatus(getTextOrDefault(root, "complianceStatus", "UNKNOWN"));
            response.setSummary(getTextOrDefault(root, "summary", "Analysis completed for " + customerId));

            // Parse key factors
            List<RiskExplanationResponse.RiskFactorDetail> factors = new ArrayList<>();
            if (root.has("keyFactors") && root.get("keyFactors").isArray()) {
                for (JsonNode f : root.get("keyFactors")) {
                    factors.add(new RiskExplanationResponse.RiskFactorDetail(
                        getTextOrDefault(f, "factor", "Unknown"),
                        getTextOrDefault(f, "impact", "MEDIUM"),
                        getTextOrDefault(f, "description", "")
                    ));
                }
            }
            response.setKeyFactors(factors);

            // Parse policy violations
            List<RiskExplanationResponse.PolicyViolation> violations = new ArrayList<>();
            if (root.has("policyViolations") && root.get("policyViolations").isArray()) {
                for (JsonNode v : root.get("policyViolations")) {
                    violations.add(new RiskExplanationResponse.PolicyViolation(
                        getTextOrDefault(v, "policyName", "Unknown"),
                        getTextOrDefault(v, "section", ""),
                        getTextOrDefault(v, "violation", ""),
                        getTextOrDefault(v, "severity", "MEDIUM")
                    ));
                }
            }
            response.setPolicyViolations(violations);

            // Parse recommendations
            List<String> recs = new ArrayList<>();
            if (root.has("recommendations") && root.get("recommendations").isArray()) {
                for (JsonNode r : root.get("recommendations")) {
                    recs.add(r.asText());
                }
            }
            response.setRecommendations(recs);

        } catch (Exception e) {
            log.warn("Failed to parse AI response as JSON, using raw text: {}", e.getMessage());
            response.setRiskLevel(score.getRiskLevel());
            response.setSummary(jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) : jsonResponse);
            response.setKeyFactors(Collections.emptyList());
            response.setPolicyViolations(Collections.emptyList());
            response.setRecommendations(Collections.singletonList("Please contact your relationship manager for details."));
        }

        return response;
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultVal) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : defaultVal;
    }
}
