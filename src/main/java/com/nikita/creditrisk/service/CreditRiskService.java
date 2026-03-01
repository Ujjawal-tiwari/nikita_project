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
 * Coordinates Spring AI, LangChain, LangGraph, MCP, and Fallback.
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

    public RiskExplanationResponse explainRisk(String customerId) {
        log.info("==== Starting Credit Risk Explanation for {} ====", customerId);

        CustomerProfile customer = customerDataService.getCustomerProfile(customerId);
        CreditRiskScore score = customerDataService.getCreditScore(customerId);
        if (customer == null || score == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }

        try {
            String aiResponse = promptService.generateRiskExplanation(customer, score);
            log.info("🤖 Raw AI response (first 500 chars): {}",
                    aiResponse != null ? aiResponse.substring(0, Math.min(500, aiResponse.length())) : "NULL");
            RiskExplanationResponse response = parseAIResponse(aiResponse, customerId, score);
            response.setProcessingMode("AI");
            response.setTimestamp(LocalDateTime.now().toString());
            return response;
        } catch (Exception e) {
            log.warn("⚠ AI explanation failed, using fallback: {}", e.getMessage());
            return buildFallbackResponse(customer, score, customerId);
        }
    }

    public Map<String, Object> explainWithLangChain(String customerId) {
        validateCustomer(customerId);
        return creditRiskChain.executeChain(customerId);
    }

    public Map<String, Object> explainWithLangGraph(String customerId) {
        validateCustomer(customerId);
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

    public String explainWithMCP(String customerId) {
        validateCustomer(customerId);
        try {
            return chatClient.prompt()
                    .user("Analyze credit risk for customer " + customerId + ". "
                            + "Look up their credit score, get their profile, analyze risk factors, "
                            + "and search for relevant RBI policies. Provide a comprehensive report.")
                    .functions("lookupCreditScore", "getCustomerProfile",
                            "analyzeRiskFactors", "searchRBIPolicies")
                    .call().content();
        } catch (Exception e) {
            log.warn("MCP failed: {}", e.getMessage());
            CustomerProfile c = customerDataService.getCustomerProfile(customerId);
            CreditRiskScore s = customerDataService.getCreditScore(customerId);
            return fallbackService.generateFallbackExplanation(c, s);
        }
    }

    public String getComplianceCheck(String customerId) {
        validateCustomer(customerId);
        CustomerProfile c = customerDataService.getCustomerProfile(customerId);
        CreditRiskScore s = customerDataService.getCreditScore(customerId);
        return promptService.generateComplianceCheck(c, s);
    }

    public String getRecommendations(String customerId) {
        validateCustomer(customerId);
        CustomerProfile c = customerDataService.getCustomerProfile(customerId);
        CreditRiskScore s = customerDataService.getCreditScore(customerId);
        return promptService.generateRecommendations(c, s);
    }

    private void validateCustomer(String customerId) {
        if (customerDataService.getCustomerProfile(customerId) == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
    }

    /**
     * Builds a detailed fallback response when AI is unavailable.
     */
    private RiskExplanationResponse buildFallbackResponse(CustomerProfile c, CreditRiskScore s, String customerId) {
        RiskExplanationResponse r = new RiskExplanationResponse();
        r.setCustomerId(customerId);
        r.setCreditScore(s.getScore());
        r.setRiskLevel(s.getRiskLevel());
        r.setProcessingMode("FALLBACK");
        r.setTimestamp(LocalDateTime.now().toString());
        r.setScoreInterpretation(
                "Credit score of " + s.getScore() + "/900 indicates " + s.getRiskLevel() + " risk level. " +
                        "DTI ratio is " + String.format("%.1f", s.getDebtToIncomeRatio()) + "%, EMI-to-income is " +
                        String.format("%.1f", s.getEmiToIncomeRatio()) + "%.");
        r.setComplianceStatus(s.getDebtToIncomeRatio() > 50 ? "NON_COMPLIANT"
                : c.getMissedPayments() > 2 ? "PARTIALLY_COMPLIANT" : "COMPLIANT");
        r.setSummary("This assessment uses rule-based analysis. The customer has a " + s.getRiskLevel() +
                " risk profile with " + c.getMissedPayments() + " missed payments and a DTI of " +
                String.format("%.1f", s.getDebtToIncomeRatio()) + "%.");

        List<RiskExplanationResponse.RiskFactorDetail> factors = new ArrayList<>();
        if (s.getDebtToIncomeRatio() > 50)
            factors.add(new RiskExplanationResponse.RiskFactorDetail(
                    "High Debt-to-Income Ratio", "HIGH", "DTI of " + String.format("%.1f", s.getDebtToIncomeRatio())
                            + "% exceeds RBI recommended limit of 50%."));
        if (c.getMissedPayments() > 0)
            factors.add(new RiskExplanationResponse.RiskFactorDetail(
                    "Payment Defaults", c.getMissedPayments() > 3 ? "HIGH" : "MEDIUM",
                    c.getMissedPayments() + " missed payments affect creditworthiness."));
        if (s.getEmiToIncomeRatio() > 40)
            factors.add(new RiskExplanationResponse.RiskFactorDetail(
                    "High EMI Burden", "HIGH",
                    "EMI-to-income ratio of " + String.format("%.1f", s.getEmiToIncomeRatio()) + "% is above 40%."));
        if (c.getCreditHistoryYears() < 3)
            factors.add(new RiskExplanationResponse.RiskFactorDetail(
                    "Short Credit History", "MEDIUM",
                    "Only " + c.getCreditHistoryYears() + " years of credit history."));
        r.setKeyFactors(factors);

        List<RiskExplanationResponse.PolicyViolation> violations = new ArrayList<>();
        if (s.getDebtToIncomeRatio() > 50)
            violations.add(new RiskExplanationResponse.PolicyViolation(
                    "RBI DTI Guidelines", "FOIR Norms", "DTI exceeds 50% threshold", "HIGH"));
        if (c.getMissedPayments() >= 3)
            violations.add(new RiskExplanationResponse.PolicyViolation(
                    "RBI NPA Classification", "DOR.STR.REC.12", "Multiple payment defaults detected", "HIGH"));
        r.setPolicyViolations(violations);

        r.setRecommendations(List.of(
                "Reduce total debt to bring DTI below 50%",
                "Clear missed payments and maintain regular payment schedule",
                "Consider debt consolidation for better EMI management",
                "Build longer credit history with small secured loans"));
        return r;
    }

    /**
     * SPRING AI - RESPONSE FORMATTING
     * Parses AI JSON response into structured RiskExplanationResponse.
     */
    private RiskExplanationResponse parseAIResponse(String aiResponse, String customerId, CreditRiskScore score) {
        RiskExplanationResponse response = new RiskExplanationResponse();
        response.setCustomerId(customerId);
        response.setCreditScore(score.getScore());

        if (aiResponse == null || aiResponse.isBlank()) {
            log.warn("AI returned empty response, using fallback");
            CustomerProfile c = customerDataService.getCustomerProfile(customerId);
            return buildFallbackResponse(c, score, customerId);
        }

        try {
            // Clean JSON - remove markdown code blocks if present
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```json?\\s*\n?", "").replaceAll("\n?\\s*```$", "");
            }

            // Try to find JSON in the response
            int jsonStart = cleaned.indexOf('{');
            int jsonEnd = cleaned.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode root = objectMapper.readTree(cleaned);

            response.setScoreInterpretation(getText(root, "scoreInterpretation", "Score: " + score.getScore()));
            response.setRiskLevel(getText(root, "riskLevel", score.getRiskLevel()));
            response.setComplianceStatus(getText(root, "complianceStatus", "UNKNOWN"));
            response.setSummary(getText(root, "summary", "AI analysis completed."));

            // Parse key factors
            List<RiskExplanationResponse.RiskFactorDetail> factors = new ArrayList<>();
            if (root.has("keyFactors") && root.get("keyFactors").isArray()) {
                for (JsonNode f : root.get("keyFactors")) {
                    factors.add(new RiskExplanationResponse.RiskFactorDetail(
                            getText(f, "factor", "Unknown"),
                            getText(f, "impact", "MEDIUM"),
                            getText(f, "description", "")));
                }
            }
            response.setKeyFactors(factors);

            // Parse policy violations
            List<RiskExplanationResponse.PolicyViolation> violations = new ArrayList<>();
            if (root.has("policyViolations") && root.get("policyViolations").isArray()) {
                for (JsonNode v : root.get("policyViolations")) {
                    violations.add(new RiskExplanationResponse.PolicyViolation(
                            getText(v, "policyName", "Unknown"),
                            getText(v, "section", ""),
                            getText(v, "violation", ""),
                            getText(v, "severity", "MEDIUM")));
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

            log.info("✅ Parsed AI response: {} factors, {} violations, {} recommendations",
                    factors.size(), violations.size(), recs.size());

        } catch (Exception e) {
            log.warn("⚠ JSON parse failed: {}. Using raw AI text as summary.", e.getMessage());
            response.setRiskLevel(score.getRiskLevel());
            response.setSummary(aiResponse.length() > 1000 ? aiResponse.substring(0, 1000) : aiResponse);
            response.setScoreInterpretation("AI analysis for score " + score.getScore() + "/900");
            response.setComplianceStatus(score.getDebtToIncomeRatio() > 50 ? "NON_COMPLIANT" : "PARTIALLY_COMPLIANT");
            response.setKeyFactors(Collections.emptyList());
            response.setPolicyViolations(Collections.emptyList());
            response.setRecommendations(List.of("Please review the detailed analysis in the summary above."));
        }

        return response;
    }

    private String getText(JsonNode node, String field, String def) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : def;
    }
}
