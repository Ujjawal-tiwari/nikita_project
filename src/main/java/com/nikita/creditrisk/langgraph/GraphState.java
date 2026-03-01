package com.nikita.creditrisk.langgraph;

import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.CreditRiskScore;
import com.nikita.creditrisk.model.RiskFactor;

import java.util.*;

/**
 * LANGGRAPH - STATE OBJECT
 * 
 * In LangGraph, a GraphState carries all data through the workflow graph.
 * Each node reads from and writes to this state, enabling data flow
 * between nodes without tight coupling.
 * 
 * The state is mutable and accumulates information as it passes
 * through each node in the graph.
 */
public class GraphState {

    private String customerId;
    private CustomerProfile customerProfile;
    private CreditRiskScore creditRiskScore;
    private List<RiskFactor> riskFactors = new ArrayList<>();
    private String riskLevel;            // HIGH, MEDIUM, LOW
    private String policyContext;        // RAG-retrieved policy text
    private String explanation;          // AI-generated explanation
    private String recommendations;      // AI-generated recommendations
    private String complianceStatus;     // COMPLIANT, PARTIALLY_COMPLIANT, NON_COMPLIANT
    private String currentNode;          // Current position in graph
    private List<String> visitedNodes = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private List<String> errors = new ArrayList<>();

    public GraphState(String customerId) {
        this.customerId = customerId;
    }

    // Track which nodes have been visited
    public void markNodeVisited(String nodeName) {
        visitedNodes.add(nodeName);
        currentNode = nodeName;
    }

    public void addError(String error) {
        errors.add(error);
    }

    // --- Getters and Setters ---
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public CustomerProfile getCustomerProfile() { return customerProfile; }
    public void setCustomerProfile(CustomerProfile customerProfile) { this.customerProfile = customerProfile; }
    public CreditRiskScore getCreditRiskScore() { return creditRiskScore; }
    public void setCreditRiskScore(CreditRiskScore creditRiskScore) { this.creditRiskScore = creditRiskScore; }
    public List<RiskFactor> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<RiskFactor> riskFactors) { this.riskFactors = riskFactors; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getPolicyContext() { return policyContext; }
    public void setPolicyContext(String policyContext) { this.policyContext = policyContext; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
    public String getCurrentNode() { return currentNode; }
    public List<String> getVisitedNodes() { return visitedNodes; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public List<String> getErrors() { return errors; }
}
