package com.nikita.creditrisk.model;

/**
 * Represents an RBI (Reserve Bank of India) policy or regulation
 * related to credit risk management.
 */
public class RBIPolicy {
    private String policyId;
    private String policyName;
    private String section;
    private String description;
    private String category;     // NPA, INCOME_RECOGNITION, PROVISIONING, etc.

    public RBIPolicy() {}

    public RBIPolicy(String policyId, String policyName, String section,
                     String description, String category) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.section = section;
        this.description = description;
        this.category = category;
    }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "RBIPolicy{'" + policyName + "', section='" + section + "'}";
    }
}
