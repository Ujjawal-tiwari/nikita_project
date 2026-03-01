package com.nikita.creditrisk.service;

import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.CreditRiskScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * SPRING AI - PROMPT ENGINEERING & RESPONSE FORMATTING SERVICE
 * 
 * Demonstrates:
 * 1. PROMPT TEMPLATES - Using .st files with variable substitution
 * 2. STRUCTURED PROMPTS - Injecting RAG context + customer data into prompts
 * 3. RESPONSE FORMATTING - Requesting JSON-structured responses from the AI
 * 4. ERROR HANDLING - @Retryable with exponential backoff
 */
@Service
public class PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptService.class);

    private final ChatClient chatClient;
    private final RAGService ragService;
    private final FallbackService fallbackService;

    // SPRING AI - PROMPT TEMPLATES: Loaded from classpath resources
    @Value("classpath:prompts/credit-risk-explanation.st")
    private Resource explanationPromptResource;

    @Value("classpath:prompts/compliance-check.st")
    private Resource compliancePromptResource;

    @Value("classpath:prompts/risk-reduction.st")
    private Resource riskReductionPromptResource;

    @Value("classpath:prompts/factor-analysis.st")
    private Resource factorAnalysisPromptResource;

    public PromptService(ChatClient.Builder chatClientBuilder,
                         RAGService ragService,
                         FallbackService fallbackService) {
        this.chatClient = chatClientBuilder.build();
        this.ragService = ragService;
        this.fallbackService = fallbackService;
    }

    /**
     * SPRING AI - PROMPT ENGINEERING + RAG + RESPONSE FORMATTING
     * 
     * Generates a credit risk explanation by:
     * 1. Retrieving relevant RBI policy context via RAG
     * 2. Building a structured prompt using PromptTemplate
     * 3. Sending to the AI model (Gemini)
     * 4. Receiving structured JSON response
     * 
     * ERROR HANDLING: @Retryable with exponential backoff
     * If all retries fail, falls back to FallbackService via @Recover
     */
    @Retryable(
        value = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public String generateRiskExplanation(CustomerProfile customer, CreditRiskScore score) {
        log.info("🤖 PROMPT SERVICE: Generating risk explanation for {}", customer.getCustomerId());

        // Step 1: RAG - Retrieve relevant policy context
        String ragContext = ragService.retrieveCreditRiskContext(
            score.getScore(), score.getDebtToIncomeRatio(), customer.getMissedPayments()
        );

        // Step 2: PROMPT TEMPLATE - Build prompt with variables
        PromptTemplate promptTemplate = new PromptTemplate(explanationPromptResource);

        // Step 3: PROMPT ENGINEERING - Substitute variables into template
        Map<String, Object> variables = buildPromptVariables(customer, score, ragContext);
        String promptText = promptTemplate.create(variables).getContents();

        log.info("📝 PROMPT: Built prompt with {} chars, RAG context: {} chars",
                 promptText.length(), ragContext.length());

        // Step 4: CALL AI MODEL - Send to Gemini via ChatClient
        String response = chatClient.prompt()
            .user(promptText)
            .call()
            .content();

        log.info("✅ AI Response received: {} chars", response.length());
        return response;
    }

    /**
     * FALLBACK RESPONSE - Called when all retries are exhausted
     */
    @Recover
    public String generateRiskExplanationFallback(Exception e, CustomerProfile customer, CreditRiskScore score) {
        log.warn("⚠ AI service failed after retries. Using FALLBACK response. Error: {}", e.getMessage());
        return fallbackService.generateFallbackExplanation(customer, score);
    }

    /**
     * Generate compliance check using dedicated prompt template.
     */
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public String generateComplianceCheck(CustomerProfile customer, CreditRiskScore score) {
        log.info("📋 Generating compliance check for {}", customer.getCustomerId());

        String ragContext = ragService.retrieveContext(
            "RBI compliance credit risk DTI ratio NPA classification regulatory guidelines"
        );

        PromptTemplate promptTemplate = new PromptTemplate(compliancePromptResource);
        Map<String, Object> variables = Map.of(
            "context", ragContext,
            "customerId", customer.getCustomerId(),
            "customerName", customer.getName(),
            "creditScore", String.valueOf(score.getScore()),
            "debtToIncomeRatio", String.format("%.1f", score.getDebtToIncomeRatio()),
            "emiToIncomeRatio", String.format("%.1f", score.getEmiToIncomeRatio()),
            "missedPayments", String.valueOf(customer.getMissedPayments()),
            "employmentType", customer.getEmploymentType()
        );

        String promptText = promptTemplate.create(variables).getContents();
        return chatClient.prompt().user(promptText).call().content();
    }

    @Recover
    public String generateComplianceCheckFallback(Exception e, CustomerProfile customer, CreditRiskScore score) {
        log.warn("⚠ Compliance check AI failed. Using fallback.");
        return fallbackService.generateFallbackCompliance(customer, score);
    }

    /**
     * Generate risk reduction recommendations.
     */
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public String generateRecommendations(CustomerProfile customer, CreditRiskScore score) {
        log.info("💡 Generating recommendations for {}", customer.getCustomerId());

        String ragContext = ragService.retrieveContext(
            "credit score improvement risk reduction strategies payment history debt management"
        );

        PromptTemplate promptTemplate = new PromptTemplate(riskReductionPromptResource);
        Map<String, Object> variables = Map.of(
            "context", ragContext,
            "customerId", customer.getCustomerId(),
            "customerName", customer.getName(),
            "creditScore", String.valueOf(score.getScore()),
            "riskLevel", score.getRiskLevel(),
            "keyIssues", buildKeyIssuesSummary(customer, score)
        );

        String promptText = promptTemplate.create(variables).getContents();
        return chatClient.prompt().user(promptText).call().content();
    }

    @Recover
    public String generateRecommendationsFallback(Exception e, CustomerProfile customer, CreditRiskScore score) {
        log.warn("⚠ Recommendations AI failed. Using fallback.");
        return fallbackService.generateFallbackRecommendations(score);
    }

    // --- Helper Methods ---

    private Map<String, Object> buildPromptVariables(CustomerProfile c, CreditRiskScore s, String context) {
        return Map.ofEntries(
            Map.entry("context", context),
            Map.entry("customerId", c.getCustomerId()),
            Map.entry("customerName", c.getName()),
            Map.entry("age", String.valueOf(c.getAge())),
            Map.entry("annualIncome", String.format("%.0f", c.getAnnualIncome())),
            Map.entry("totalDebt", String.format("%.0f", c.getTotalDebt())),
            Map.entry("creditHistoryYears", String.valueOf(c.getCreditHistoryYears())),
            Map.entry("numberOfLoans", String.valueOf(c.getNumberOfLoans())),
            Map.entry("missedPayments", String.valueOf(c.getMissedPayments())),
            Map.entry("employmentType", c.getEmploymentType()),
            Map.entry("monthlyEMI", String.format("%.0f", c.getMonthlyEMI())),
            Map.entry("creditScore", String.valueOf(s.getScore()))
        );
    }

    private String buildKeyIssuesSummary(CustomerProfile c, CreditRiskScore s) {
        StringBuilder issues = new StringBuilder();
        if (s.getDebtToIncomeRatio() > 50) issues.append("High DTI ratio (").append(String.format("%.1f", s.getDebtToIncomeRatio())).append("%). ");
        if (c.getMissedPayments() > 0) issues.append(c.getMissedPayments()).append(" missed payments. ");
        if (s.getEmiToIncomeRatio() > 40) issues.append("EMI exceeds 40% of income. ");
        if (c.getCreditHistoryYears() < 3) issues.append("Short credit history. ");
        if (issues.length() == 0) issues.append("No major issues identified.");
        return issues.toString();
    }
}
