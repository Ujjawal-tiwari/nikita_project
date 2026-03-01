package com.nikita.creditrisk.langchain;

import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.CreditRiskScore;
import com.nikita.creditrisk.model.RiskFactor;
import com.nikita.creditrisk.service.CustomerDataService;
import com.nikita.creditrisk.service.RAGService;
import com.nikita.creditrisk.service.RobustAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * LANGCHAIN - SEQUENTIAL CHAIN PATTERN IMPLEMENTATION
 * 
 * LangChain is a framework for building AI applications using "chains" -
 * sequences of operations where each step's output feeds into the next step.
 * 
 * This implementation demonstrates the LangChain concept using Spring AI:
 * 
 * Chain 1: SCORE ANALYSIS → Analyzes the numeric credit score
 * Chain 2: POLICY RETRIEVAL → Retrieves relevant RBI policies via RAG
 * Chain 3: EXPLANATION → Generates human-readable explanation
 * Chain 4: RECOMMENDATION → Produces risk reduction suggestions
 * 
 * Each chain passes its output as input to the next chain, building context.
 */
@Service
public class CreditRiskChain {

    private static final Logger log = LoggerFactory.getLogger(CreditRiskChain.class);

    private final RobustAiClient chatClient;
    private final RAGService ragService;
    private final CustomerDataService customerDataService;

    // Chain context - carries data through the chain
    private final Map<String, Function<Map<String, Object>, Map<String, Object>>> chainSteps = new LinkedHashMap<>();

    public CreditRiskChain(RobustAiClient chatClient,
            RAGService ragService,
            CustomerDataService customerDataService) {
        this.chatClient = chatClient;
        this.ragService = ragService;
        this.customerDataService = customerDataService;

        // Register chain steps in sequence
        registerChainSteps();
    }

    /**
     * LANGCHAIN CONCEPT: Define the sequential chain steps.
     * Each step is a Function that takes context and returns enriched context.
     */
    private void registerChainSteps() {
        // Chain 1: Score Analysis - Classifies and interprets the score
        chainSteps.put("scoreAnalysis", this::scoreAnalysisChain);

        // Chain 2: Policy Retrieval - Uses RAG to find relevant policies
        chainSteps.put("policyRetrieval", this::policyRetrievalChain);

        // Chain 3: Explanation Generation - AI generates explanation
        chainSteps.put("explanationGeneration", this::explanationGenerationChain);

        // Chain 4: Recommendation - AI generates improvement suggestions
        chainSteps.put("recommendation", this::recommendationChain);
    }

    /**
     * LANGCHAIN: Execute the full chain for a customer.
     * Runs all chain steps sequentially, passing context between them.
     */
    public Map<String, Object> executeChain(String customerId) {
        log.info("🔗 LANGCHAIN: Starting sequential chain execution for {}", customerId);

        // Initialize chain context
        Map<String, Object> context = new HashMap<>();
        context.put("customerId", customerId);
        context.put("startTime", System.currentTimeMillis());

        // Execute each chain step sequentially
        int stepNum = 1;
        for (Map.Entry<String, Function<Map<String, Object>, Map<String, Object>>> step : chainSteps.entrySet()) {
            String stepName = step.getKey();
            log.info("  → Chain Step {}/{}: {}", stepNum, chainSteps.size(), stepName);

            try {
                context = step.getValue().apply(context);
                context.put(stepName + "_status", "COMPLETED");
                log.info("  ✅ {} completed successfully", stepName);
            } catch (Exception e) {
                log.error("  ❌ {} failed: {}", stepName, e.getMessage());
                context.put(stepName + "_status", "FAILED");
                context.put(stepName + "_error", e.getMessage());
            }
            stepNum++;
        }

        long elapsed = System.currentTimeMillis() - (long) context.get("startTime");
        context.put("totalProcessingTimeMs", elapsed);
        log.info("🔗 LANGCHAIN: Chain completed in {}ms", elapsed);

        return context;
    }

    // ===== CHAIN STEP 1: Score Analysis =====
    private Map<String, Object> scoreAnalysisChain(Map<String, Object> context) {
        String customerId = (String) context.get("customerId");

        CustomerProfile profile = customerDataService.getCustomerProfile(customerId);
        CreditRiskScore score = customerDataService.getCreditScore(customerId);

        if (profile == null || score == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }

        // Score classification logic
        String riskCategory;
        if (score.getScore() >= 750)
            riskCategory = "LOW_RISK";
        else if (score.getScore() >= 550)
            riskCategory = "MEDIUM_RISK";
        else
            riskCategory = "HIGH_RISK";

        context.put("customerProfile", profile);
        context.put("creditScore", score);
        context.put("riskCategory", riskCategory);
        context.put("scoreAnalysis", "Score " + score.getScore() + "/900 classified as " + riskCategory);

        return context;
    }

    // ===== CHAIN STEP 2: Policy Retrieval (RAG) =====
    private Map<String, Object> policyRetrievalChain(Map<String, Object> context) {
        CreditRiskScore score = (CreditRiskScore) context.get("creditScore");
        CustomerProfile profile = (CustomerProfile) context.get("customerProfile");

        // Use RAG to retrieve relevant policy context
        String policyContext = ragService.retrieveCreditRiskContext(
                score.getScore(), score.getDebtToIncomeRatio(), profile.getMissedPayments());

        context.put("policyContext", policyContext);
        context.put("ragRetrievalDone", true);

        return context;
    }

    // ===== CHAIN STEP 3: Explanation Generation (AI) =====
    private Map<String, Object> explanationGenerationChain(Map<String, Object> context) {
        CustomerProfile profile = (CustomerProfile) context.get("customerProfile");
        CreditRiskScore score = (CreditRiskScore) context.get("creditScore");
        String policyContext = (String) context.get("policyContext");

        String prompt = "Based on the following credit data and RBI policies, explain the credit risk:\n\n"
                + "Customer: " + profile.getName() + "\n"
                + "Score: " + score.getScore() + "/900 (" + context.get("riskCategory") + ")\n"
                + "DTI Ratio: " + score.getDebtToIncomeRatio() + "%\n"
                + "Missed Payments: " + profile.getMissedPayments() + "\n\n"
                + "Relevant RBI Policies:\n" + policyContext + "\n\n"
                + "Provide a clear explanation of WHY this score is what it is, "
                + "citing specific RBI policies. Keep it concise (3-4 paragraphs).";

        String explanation = chatClient.call(prompt, "Credit risk explanation unavailable due to AI limits.");
        context.put("explanation", explanation);

        return context;
    }

    // ===== CHAIN STEP 4: Recommendation (AI) =====
    private Map<String, Object> recommendationChain(Map<String, Object> context) {
        String riskCategory = (String) context.get("riskCategory");
        String explanation = (String) context.get("explanation");
        CustomerProfile profile = (CustomerProfile) context.get("customerProfile");

        String prompt = "Based on this credit risk analysis:\n\n"
                + explanation + "\n\n"
                + "Risk Level: " + riskCategory + "\n"
                + "Customer Employment: " + profile.getEmploymentType() + "\n\n"
                + "Provide 3-5 specific, actionable recommendations for the customer "
                + "to improve their credit score. Be practical and reference RBI guidelines where applicable.";

        String recommendations = chatClient.call(prompt, "1. Ensure timely payments.\n2. Reduce Debt-to-Income ratio.");
        context.put("recommendations", recommendations);

        return context;
    }
}
