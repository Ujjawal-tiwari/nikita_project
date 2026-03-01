package com.nikita.creditrisk.langgraph;

import com.nikita.creditrisk.model.CreditRiskScore;
import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.RiskFactor;
import com.nikita.creditrisk.service.CustomerDataService;
import com.nikita.creditrisk.service.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * LANGGRAPH - RISK ASSESSMENT WORKFLOW GRAPH
 * 
 * LangGraph is a framework for building stateful, multi-step AI workflows
 * as directed graphs with conditional routing.
 * 
 * This implementation creates a workflow graph:
 * 
 *   [START]
 *      ↓
 *   [Score Classifier] → classifies risk level
 *      ↓
 *   [Conditional Router] → routes based on risk level
 *      ↓                    ↓                    ↓
 *   HIGH RISK           MEDIUM RISK           LOW RISK
 *   [Deep Audit]        [Standard Check]      [Quick Review]
 *      ↓                    ↓                    ↓
 *   [Policy Checker] ← ← ← ← ← ← ← ← ← ← ← ←
 *      ↓
 *   [Explanation Generator]
 *      ↓
 *   [Recommendation Engine]
 *      ↓
 *   [END]
 */
@Service
public class RiskAssessmentGraph {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentGraph.class);

    private final CustomerDataService customerDataService;
    private final RAGService ragService;
    private final ChatClient chatClient;

    // Graph structure: nodes and edges
    private final Map<String, GraphNode> nodes = new LinkedHashMap<>();
    private final Map<String, Function<GraphState, String>> conditionalEdges = new HashMap<>();

    // Special node names
    private static final String START = "START";
    private static final String END = "END";

    public RiskAssessmentGraph(CustomerDataService customerDataService,
                                RAGService ragService,
                                ChatClient.Builder chatClientBuilder) {
        this.customerDataService = customerDataService;
        this.ragService = ragService;
        this.chatClient = chatClientBuilder.build();

        // Build the graph
        buildGraph();
    }

    /**
     * LANGGRAPH: Define the graph structure with nodes and edges.
     */
    private void buildGraph() {
        // Register nodes
        nodes.put("scoreClassifier", this::scoreClassifierNode);
        nodes.put("deepAudit", this::deepAuditNode);
        nodes.put("standardCheck", this::standardCheckNode);
        nodes.put("quickReview", this::quickReviewNode);
        nodes.put("policyChecker", this::policyCheckerNode);
        nodes.put("explanationGenerator", this::explanationGeneratorNode);
        nodes.put("recommendationEngine", this::recommendationEngineNode);

        // Register conditional edge: scoreClassifier → routes based on risk level
        conditionalEdges.put("scoreClassifier", state -> {
            String riskLevel = state.getRiskLevel();
            log.info("  🔀 CONDITIONAL ROUTING: Risk level = {} ", riskLevel);
            switch (riskLevel) {
                case "HIGH": return "deepAudit";
                case "MEDIUM": return "standardCheck";
                case "LOW": return "quickReview";
                default: return "standardCheck";
            }
        });
    }

    /**
     * LANGGRAPH: Execute the graph from start to end.
     * Follows edges (including conditional routing) between nodes.
     */
    public GraphState execute(String customerId) {
        log.info("📊 LANGGRAPH: Starting risk assessment graph for {}", customerId);

        GraphState state = new GraphState(customerId);
        long startTime = System.currentTimeMillis();

        // Define execution order with conditional routing
        List<String> executionPath = new ArrayList<>();
        executionPath.add("scoreClassifier");

        // Execute scoreClassifier first
        executeNode("scoreClassifier", state);

        // Apply conditional routing
        if (conditionalEdges.containsKey("scoreClassifier")) {
            String nextNode = conditionalEdges.get("scoreClassifier").apply(state);
            executionPath.add(nextNode);
            executeNode(nextNode, state);
        }

        // Continue with fixed path: policyChecker → explanationGenerator → recommendationEngine
        String[] remainingNodes = { "policyChecker", "explanationGenerator", "recommendationEngine" };
        for (String nodeName : remainingNodes) {
            executionPath.add(nodeName);
            executeNode(nodeName, state);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        state.getMetadata().put("executionPath", executionPath);
        state.getMetadata().put("totalTimeMs", elapsed);

        log.info("📊 LANGGRAPH: Graph completed in {}ms. Path: {}", elapsed, executionPath);
        return state;
    }

    private void executeNode(String nodeName, GraphState state) {
        GraphNode node = nodes.get(nodeName);
        if (node != null) {
            log.info("  📌 Executing node: {}", nodeName);
            try {
                node.process(state);
                state.markNodeVisited(nodeName);
            } catch (Exception e) {
                log.error("  ❌ Node {} failed: {}", nodeName, e.getMessage());
                state.addError(nodeName + ": " + e.getMessage());
                state.markNodeVisited(nodeName + " (FAILED)");
            }
        }
    }

    // ===== GRAPH NODES =====

    /**
     * Node 1: Score Classifier - Loads customer data and classifies risk.
     */
    private GraphState scoreClassifierNode(GraphState state) {
        CustomerProfile profile = customerDataService.getCustomerProfile(state.getCustomerId());
        CreditRiskScore score = customerDataService.getCreditScore(state.getCustomerId());
        List<RiskFactor> factors = customerDataService.getRiskFactors(state.getCustomerId());

        if (profile == null || score == null) {
            throw new IllegalArgumentException("Customer not found: " + state.getCustomerId());
        }

        state.setCustomerProfile(profile);
        state.setCreditRiskScore(score);
        state.setRiskFactors(factors);
        state.setRiskLevel(score.getRiskLevel());

        return state;
    }

    /**
     * Node 2a: Deep Audit (HIGH RISK path) - Thorough analysis.
     */
    private GraphState deepAuditNode(GraphState state) {
        log.info("  🔴 HIGH RISK PATH: Performing deep audit for {}", state.getCustomerId());
        state.getMetadata().put("auditType", "DEEP_AUDIT");
        state.getMetadata().put("auditLevel", "ENHANCED_DUE_DILIGENCE");
        state.getMetadata().put("requiresManualReview", true);
        state.getMetadata().put("npaRiskFlag", state.getCreditRiskScore().getScore() < 450);
        return state;
    }

    /**
     * Node 2b: Standard Check (MEDIUM RISK path).
     */
    private GraphState standardCheckNode(GraphState state) {
        log.info("  🟡 MEDIUM RISK PATH: Standard check for {}", state.getCustomerId());
        state.getMetadata().put("auditType", "STANDARD_CHECK");
        state.getMetadata().put("auditLevel", "STANDARD");
        state.getMetadata().put("requiresManualReview", false);
        return state;
    }

    /**
     * Node 2c: Quick Review (LOW RISK path).
     */
    private GraphState quickReviewNode(GraphState state) {
        log.info("  🟢 LOW RISK PATH: Quick review for {}", state.getCustomerId());
        state.getMetadata().put("auditType", "QUICK_REVIEW");
        state.getMetadata().put("auditLevel", "BASIC");
        state.getMetadata().put("requiresManualReview", false);
        return state;
    }

    /**
     * Node 3: Policy Checker - Uses RAG to find relevant policies.
     */
    private GraphState policyCheckerNode(GraphState state) {
        CreditRiskScore score = state.getCreditRiskScore();
        CustomerProfile profile = state.getCustomerProfile();

        String context = ragService.retrieveCreditRiskContext(
            score.getScore(), score.getDebtToIncomeRatio(), profile.getMissedPayments()
        );
        state.setPolicyContext(context);

        // Determine compliance
        if (score.getDebtToIncomeRatio() > 50 || profile.getMissedPayments() > 3) {
            state.setComplianceStatus("NON_COMPLIANT");
        } else if (score.getDebtToIncomeRatio() > 40 || profile.getMissedPayments() > 1) {
            state.setComplianceStatus("PARTIALLY_COMPLIANT");
        } else {
            state.setComplianceStatus("COMPLIANT");
        }

        return state;
    }

    /**
     * Node 4: Explanation Generator - AI generates explanation.
     */
    private GraphState explanationGeneratorNode(GraphState state) {
        CustomerProfile p = state.getCustomerProfile();
        CreditRiskScore s = state.getCreditRiskScore();

        String prompt = "You are a credit risk analyst. Explain this assessment:\n\n"
            + "Customer: " + p.getName() + " (Score: " + s.getScore() + "/900, "
            + state.getRiskLevel() + " RISK)\n"
            + "DTI: " + s.getDebtToIncomeRatio() + "%, Missed Payments: " + p.getMissedPayments() + "\n"
            + "Audit Type: " + state.getMetadata().get("auditType") + "\n\n"
            + "RBI Policy Context:\n" + state.getPolicyContext() + "\n\n"
            + "Provide a clear, professional explanation (3-4 paragraphs) covering score meaning, "
            + "key risk factors, policy compliance, and overall assessment.";

        String explanation = chatClient.prompt().user(prompt).call().content();
        state.setExplanation(explanation);
        return state;
    }

    /**
     * Node 5: Recommendation Engine - AI suggests improvements.
     */
    private GraphState recommendationEngineNode(GraphState state) {
        String prompt = "Based on this credit risk assessment:\n\n"
            + "Risk Level: " + state.getRiskLevel() + "\n"
            + "Score: " + state.getCreditRiskScore().getScore() + "/900\n"
            + "Compliance: " + state.getComplianceStatus() + "\n\n"
            + state.getExplanation() + "\n\n"
            + "Provide 3-5 specific, prioritized recommendations to improve the credit score. "
            + "Format each as a numbered list with expected timeline and impact.";

        String recommendations = chatClient.prompt().user(prompt).call().content();
        state.setRecommendations(recommendations);
        return state;
    }
}
