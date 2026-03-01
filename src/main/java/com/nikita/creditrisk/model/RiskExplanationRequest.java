package com.nikita.creditrisk.model;

/**
 * Request DTO for credit risk explanation API.
 */
public class RiskExplanationRequest {
    private String customerId;
    private String query;  // Optional user question about the risk

    public RiskExplanationRequest() {}

    public RiskExplanationRequest(String customerId, String query) {
        this.customerId = customerId;
        this.query = query;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
