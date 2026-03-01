package com.nikita.creditrisk.service;

import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.CreditRiskScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SPRING AI - PROMPT ENGINEERING & RESPONSE FORMATTING SERVICE
 * 
 * Demonstrates:
 * 1. PROMPT TEMPLATES - Dynamic prompt construction with variables
 * 2. STRUCTURED PROMPTS - Injecting RAG context + customer data
 * 3. RESPONSE FORMATTING - Requesting JSON-structured responses
 * 4. ERROR HANDLING - @Retryable with exponential backoff
 */
@Service
public class PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptService.class);

    private final RobustAiClient chatClient;
    private final RAGService ragService;
    private final FallbackService fallbackService;

    public PromptService(RobustAiClient chatClient,
            RAGService ragService,
            FallbackService fallbackService) {
        this.chatClient = chatClient;
        this.ragService = ragService;
        this.fallbackService = fallbackService;
    }

    /**
     * SPRING AI - PROMPT ENGINEERING + RAG + RESPONSE FORMATTING
     * ERROR HANDLING: @Retryable with exponential backoff
     */
    public String generateRiskExplanation(CustomerProfile customer, CreditRiskScore score) {
        log.info("🤖 PROMPT SERVICE: Generating risk explanation for {}", customer.getCustomerId());

        try {
            // Step 1: RAG - Retrieve relevant policy context
            String ragContext = ragService.retrieveCreditRiskContext(
                    score.getScore(), score.getDebtToIncomeRatio(), customer.getMissedPayments());

            // Step 2: Build prompt with all context
            String prompt = buildExplanationPrompt(customer, score, ragContext);
            log.info("📝 PROMPT: Built prompt with {} chars, RAG context: {} chars", prompt.length(),
                    ragContext.length());

            // Step 3: Call AI model using RobustAiClient (handles rate limits)
            String response = chatClient.call(prompt, "{}"); // Fallback to empty JSON object if fully failed

            // Check if response is empty JSON which means RobustAiClient triggered its
            // fallback
            if (response == null || "{}".equals(response.trim())) {
                log.warn("⚠ RobustAiClient returned fallback. Using FallbackService generation.");
                return fallbackService.generateFallbackExplanation(customer, score);
            }

            log.info("✅ AI Response received: {} chars", response.length());
            return response;
        } catch (Exception e) {
            log.warn("⚠ Error in prompt generation. Using FALLBACK. Error: {}", e.getMessage());
            return fallbackService.generateFallbackExplanation(customer, score);
        }
    }

    public String generateComplianceCheck(CustomerProfile customer, CreditRiskScore score) {
        try {
            String ragContext = ragService.retrieveContext("RBI compliance credit risk DTI ratio NPA classification");
            String prompt = buildCompliancePrompt(customer, score, ragContext);
            String response = chatClient.call(prompt, "COMPLIANT");

            if (response == null || "COMPLIANT".equals(response.trim())) {
                return fallbackService.generateFallbackCompliance(customer, score);
            }
            return response;
        } catch (Exception e) {
            return fallbackService.generateFallbackCompliance(customer, score);
        }
    }

    public String generateRecommendations(CustomerProfile customer, CreditRiskScore score) {
        try {
            String ragContext = ragService.retrieveContext("credit score improvement risk reduction strategies");
            String prompt = buildRecommendationsPrompt(customer, score, ragContext);
            String response = chatClient.call(prompt, "[]");

            if (response == null || "[]".equals(response.trim())) {
                return fallbackService.generateFallbackRecommendations(score);
            }
            return response;
        } catch (Exception e) {
            return fallbackService.generateFallbackRecommendations(score);
        }
    }

    // ===================== PROMPT BUILDERS =====================

    private String buildExplanationPrompt(CustomerProfile c, CreditRiskScore s, String context) {
        return """
                You are a Credit Risk Analyst AI for an Indian bank regulated by RBI.

                RELEVANT RBI POLICY CONTEXT (Retrieved via RAG):
                %s

                CUSTOMER PROFILE:
                - Customer ID: %s
                - Name: %s
                - Age: %d
                - Annual Income: Rs. %.0f
                - Total Debt: Rs. %.0f
                - Credit History: %d years
                - Active Loans: %d
                - Missed Payments: %d
                - Employment: %s
                - Monthly EMI: Rs. %.0f

                CREDIT SCORE: %d/900 (Risk Level: %s)
                DTI Ratio: %.1f%% | EMI-to-Income: %.1f%%

                Provide a detailed credit risk explanation in this EXACT JSON format:
                {"scoreInterpretation": "Detailed explanation of what this credit score means for the customer", "riskLevel": "HIGH or MEDIUM or LOW", "keyFactors": [{"factor": "Factor name", "impact": "HIGH or MEDIUM or LOW", "description": "Detailed explanation of this factor"}], "policyViolations": [{"policyName": "RBI Policy name", "section": "Relevant section", "violation": "What was violated and why", "severity": "HIGH or MEDIUM or LOW"}], "recommendations": ["Specific actionable recommendation 1", "Specific actionable recommendation 2", "Specific actionable recommendation 3"], "complianceStatus": "COMPLIANT or PARTIALLY_COMPLIANT or NON_COMPLIANT", "summary": "A comprehensive 3-4 sentence summary of the overall credit risk assessment"}

                IMPORTANT: Return ONLY valid JSON. No markdown, no code blocks, no extra text.
                """
                .formatted(
                        context, c.getCustomerId(), c.getName(), c.getAge(),
                        c.getAnnualIncome(), c.getTotalDebt(), c.getCreditHistoryYears(),
                        c.getNumberOfLoans(), c.getMissedPayments(), c.getEmploymentType(),
                        c.getMonthlyEMI(), s.getScore(), s.getRiskLevel(),
                        s.getDebtToIncomeRatio(), s.getEmiToIncomeRatio());
    }

    private String buildCompliancePrompt(CustomerProfile c, CreditRiskScore s, String context) {
        return """
                You are an RBI Compliance Officer AI. Check this customer against RBI regulations.

                RBI POLICY CONTEXT:
                %s

                Customer: %s (%s), Score: %d/900, DTI: %.1f%%, Missed Payments: %d, Employment: %s

                Return ONLY valid JSON with this structure:
                {"complianceStatus": "COMPLIANT or PARTIALLY_COMPLIANT or NON_COMPLIANT", "violations": [{"policyName": "Policy", "section": "Section", "violation": "Details", "severity": "HIGH/MEDIUM/LOW"}], "summary": "Overall compliance summary"}
                """
                .formatted(context, c.getCustomerId(), c.getName(), s.getScore(),
                        s.getDebtToIncomeRatio(), c.getMissedPayments(), c.getEmploymentType());
    }

    private String buildRecommendationsPrompt(CustomerProfile c, CreditRiskScore s, String context) {
        StringBuilder issues = new StringBuilder();
        if (s.getDebtToIncomeRatio() > 50)
            issues.append("High DTI (").append(String.format("%.1f", s.getDebtToIncomeRatio())).append("%). ");
        if (c.getMissedPayments() > 0)
            issues.append(c.getMissedPayments()).append(" missed payments. ");
        if (s.getEmiToIncomeRatio() > 40)
            issues.append("EMI exceeds 40%% of income. ");
        if (c.getCreditHistoryYears() < 3)
            issues.append("Short credit history. ");

        return """
                You are a Credit Risk Advisor AI for an Indian bank.

                CONTEXT: %s

                Customer: %s, Score: %d/900 (%s risk), Key Issues: %s

                Provide 5 specific, actionable recommendations as a JSON array:
                ["Recommendation 1 with specific steps", "Recommendation 2", "Recommendation 3", "Recommendation 4", "Recommendation 5"]

                Return ONLY the JSON array, nothing else.
                """
                .formatted(context, c.getName(), s.getScore(), s.getRiskLevel(),
                        issues.length() > 0 ? issues.toString() : "No major issues");
    }
}
