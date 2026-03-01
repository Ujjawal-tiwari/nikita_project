package com.nikita.creditrisk.model;

/**
 * Represents an individual risk factor that contributes to the credit score.
 */
public class RiskFactor {
    private String factorName;
    private String impact;       // HIGH, MEDIUM, LOW
    private double weight;       // 0.0 to 1.0
    private String description;
    private String category;     // PAYMENT_HISTORY, DEBT_RATIO, CREDIT_AGE, etc.

    public RiskFactor() {}

    public RiskFactor(String factorName, String impact, double weight,
                      String description, String category) {
        this.factorName = factorName;
        this.impact = impact;
        this.weight = weight;
        this.description = description;
        this.category = category;
    }

    public String getFactorName() { return factorName; }
    public void setFactorName(String factorName) { this.factorName = factorName; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "RiskFactor{'" + factorName + "', impact='" + impact + "', weight=" + weight + "}";
    }
}
