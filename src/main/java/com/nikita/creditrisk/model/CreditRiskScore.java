package com.nikita.creditrisk.model;

/**
 * Represents a numeric credit risk score with metadata.
 * Score range: 300 (worst) to 900 (best), following Indian CIBIL scale.
 */
public class CreditRiskScore {

    private String customerId;
    private int score;           // 300-900
    private int maxScore;        // 900
    private String riskLevel;    // HIGH, MEDIUM, LOW
    private double debtToIncomeRatio;
    private double emiToIncomeRatio;
    private String calculatedAt;

    public CreditRiskScore() {
        this.maxScore = 900;
    }

    public CreditRiskScore(String customerId, int score, String riskLevel,
                           double debtToIncomeRatio, double emiToIncomeRatio) {
        this.customerId = customerId;
        this.score = score;
        this.maxScore = 900;
        this.riskLevel = riskLevel;
        this.debtToIncomeRatio = debtToIncomeRatio;
        this.emiToIncomeRatio = emiToIncomeRatio;
        this.calculatedAt = java.time.LocalDateTime.now().toString();
    }

    // --- Getters and Setters ---
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public double getDebtToIncomeRatio() { return debtToIncomeRatio; }
    public void setDebtToIncomeRatio(double debtToIncomeRatio) { this.debtToIncomeRatio = debtToIncomeRatio; }
    public double getEmiToIncomeRatio() { return emiToIncomeRatio; }
    public void setEmiToIncomeRatio(double emiToIncomeRatio) { this.emiToIncomeRatio = emiToIncomeRatio; }
    public String getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(String calculatedAt) { this.calculatedAt = calculatedAt; }

    @Override
    public String toString() {
        return "CreditRiskScore{customerId='" + customerId + "', score=" + score +
               "/" + maxScore + ", riskLevel='" + riskLevel + "'}";
    }
}
