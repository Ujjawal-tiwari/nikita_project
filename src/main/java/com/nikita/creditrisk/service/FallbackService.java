package com.nikita.creditrisk.service;

import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.CreditRiskScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SPRING AI - FALLBACK RESPONSE SERVICE
 * 
 * Provides rule-based fallback responses when the AI service is unavailable.
 * This is a critical part of Error Handling strategy:
 * 
 * 1. AI Service Down → FallbackService provides pre-computed explanations
 * 2. API Rate Limited → FallbackService serves cached/rule-based responses
 * 3. Invalid API Key → FallbackService ensures the app still functions
 * 
 * The fallback uses deterministic business rules instead of AI generation.
 */
@Service
public class FallbackService {

    private static final Logger log = LoggerFactory.getLogger(FallbackService.class);

    /**
     * Generates a rule-based credit risk explanation without AI.
     * Uses score ranges and business rules from RBI guidelines.
     */
    public String generateFallbackExplanation(CustomerProfile customer, CreditRiskScore score) {
        log.info("🔄 FALLBACK: Generating rule-based explanation for score {}", score.getScore());

        String riskLevel = classifyRisk(score.getScore());
        String interpretation = getScoreInterpretation(score.getScore());

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"scoreInterpretation\": \"").append(interpretation).append("\",\n");
        json.append("  \"riskLevel\": \"").append(riskLevel).append("\",\n");
        json.append("  \"keyFactors\": [\n");

        // Rule-based factor analysis
        if (customer.getMissedPayments() > 2) {
            json.append("    {\"factor\": \"Payment History\", \"impact\": \"HIGH\", \"description\": \"")
                .append(customer.getMissedPayments()).append(" missed payments severely impact credit score\"},\n");
        }
        if (score.getDebtToIncomeRatio() > 50) {
            json.append("    {\"factor\": \"Debt-to-Income Ratio\", \"impact\": \"HIGH\", \"description\": \"DTI of ")
                .append(String.format("%.1f", score.getDebtToIncomeRatio()))
                .append("% exceeds RBI guideline of 50%\"},\n");
        }
        if (customer.getCreditHistoryYears() < 3) {
            json.append("    {\"factor\": \"Credit History Length\", \"impact\": \"MEDIUM\", \"description\": \"Only ")
                .append(customer.getCreditHistoryYears()).append(" years of history - minimum 3 years preferred\"},\n");
        }
        if (score.getEmiToIncomeRatio() > 40) {
            json.append("    {\"factor\": \"EMI Burden\", \"impact\": \"HIGH\", \"description\": \"EMI/Income ratio of ")
                .append(String.format("%.1f", score.getEmiToIncomeRatio()))
                .append("% exceeds recommended 40%\"},\n");
        }
        if (customer.getNumberOfLoans() > 3) {
            json.append("    {\"factor\": \"Multiple Loans\", \"impact\": \"MEDIUM\", \"description\": \"")
                .append(customer.getNumberOfLoans()).append(" active loans indicate over-leveraging\"}\n");
        } else {
            json.append("    {\"factor\": \"Loan Portfolio\", \"impact\": \"LOW\", \"description\": \"")
                .append(customer.getNumberOfLoans()).append(" active loans within acceptable range\"}\n");
        }

        json.append("  ],\n");
        json.append("  \"policyViolations\": [\n");

        if (score.getDebtToIncomeRatio() > 50) {
            json.append("    {\"policyName\": \"RBI Credit Risk Guidelines\", \"section\": \"Section 2.1\", ")
                .append("\"violation\": \"DTI ratio exceeds 50% limit\", \"severity\": \"HIGH\"},\n");
        }
        if (score.getEmiToIncomeRatio() > 40) {
            json.append("    {\"policyName\": \"RBI Credit Risk Guidelines\", \"section\": \"Section 2.2\", ")
                .append("\"violation\": \"EMI/Income exceeds 40% threshold\", \"severity\": \"HIGH\"},\n");
        }
        if (customer.getMissedPayments() > 3) {
            json.append("    {\"policyName\": \"RBI NPA Classification\", \"section\": \"Section 3.1\", ")
                .append("\"violation\": \"Potential NPA - overdue payments beyond 90 days\", \"severity\": \"HIGH\"}\n");
        } else {
            json.append("    {\"policyName\": \"General Compliance\", \"section\": \"N/A\", ")
                .append("\"violation\": \"No major violations detected\", \"severity\": \"LOW\"}\n");
        }

        json.append("  ],\n");
        json.append("  \"recommendations\": [\n");
        json.append("    \"").append(getRecommendation(score.getScore(), 1)).append("\",\n");
        json.append("    \"").append(getRecommendation(score.getScore(), 2)).append("\",\n");
        json.append("    \"").append(getRecommendation(score.getScore(), 3)).append("\"\n");
        json.append("  ],\n");

        String compliance = score.getDebtToIncomeRatio() > 50 || customer.getMissedPayments() > 3 
            ? "NON_COMPLIANT" : (score.getDebtToIncomeRatio() > 40 ? "PARTIALLY_COMPLIANT" : "COMPLIANT");
        json.append("  \"complianceStatus\": \"").append(compliance).append("\",\n");
        json.append("  \"summary\": \"[FALLBACK MODE] Customer ").append(customer.getName())
            .append(" has a credit score of ").append(score.getScore())
            .append("/900 classified as ").append(riskLevel)
            .append(" risk. This is a rule-based analysis as AI service is currently unavailable.\"\n");
        json.append("}");

        return json.toString();
    }

    public String generateFallbackCompliance(CustomerProfile customer, CreditRiskScore score) {
        String compliance = score.getDebtToIncomeRatio() > 50 ? "NON_COMPLIANT" 
            : (score.getDebtToIncomeRatio() > 40 ? "PARTIALLY_COMPLIANT" : "COMPLIANT");
        
        return "{\"overallCompliance\":\"" + compliance + "\","
            + "\"checks\":[{\"regulation\":\"DTI Ratio Guideline\",\"section\":\"Section 2.1\","
            + "\"status\":\"" + (score.getDebtToIncomeRatio() > 50 ? "FAIL" : "PASS") + "\","
            + "\"detail\":\"DTI ratio is " + String.format("%.1f", score.getDebtToIncomeRatio()) + "%\"}],"
            + "\"riskFlags\":[" + (customer.getMissedPayments() > 3 ? "\"Potential NPA\"" : "") + "],"
            + "\"auditNotes\":\"[FALLBACK] Rule-based compliance check. AI unavailable.\"}";
    }

    public String generateFallbackRecommendations(CreditRiskScore score) {
        return "{\"currentScoreBand\":\"" + classifyBand(score.getScore()) + "\","
            + "\"targetScore\":750,\"timelineMonths\":12,"
            + "\"immediateActions\":[{\"action\":\"Set up automatic EMI payments\",\"expectedImpact\":\"+20 points\",\"timeline\":\"1-3 months\"}],"
            + "\"mediumTermActions\":[{\"action\":\"Reduce credit card utilization below 30%\",\"expectedImpact\":\"+40 points\",\"timeline\":\"3-6 months\"}],"
            + "\"longTermActions\":[{\"action\":\"Maintain consistent payment history for 12 months\",\"expectedImpact\":\"+80 points\",\"timeline\":\"6-12 months\"}],"
            + "\"warnings\":[\"Avoid applying for new credit in the next 6 months\","
            + "\"[FALLBACK] These are generic recommendations. AI service was unavailable.\"]}";
    }

    // --- Helper methods ---
    private String classifyRisk(int score) {
        if (score >= 750) return "LOW";
        if (score >= 550) return "MEDIUM";
        return "HIGH";
    }

    private String classifyBand(int score) {
        if (score >= 750) return "Excellent";
        if (score >= 650) return "Good";
        if (score >= 550) return "Fair";
        return "Poor";
    }

    private String getScoreInterpretation(int score) {
        if (score >= 750) return "Excellent credit score. Low risk of default. Eligible for preferential rates.";
        if (score >= 650) return "Good credit score. Moderate risk. Standard lending terms apply.";
        if (score >= 550) return "Fair credit score. Elevated risk. Additional scrutiny and higher rates may apply.";
        return "Poor credit score. High risk of default. Enhanced due diligence required per RBI norms.";
    }

    private String getRecommendation(int score, int index) {
        if (score < 550) {
            switch (index) {
                case 1: return "Immediately clear overdue payments to prevent NPA classification";
                case 2: return "Consolidate high-interest debt and negotiate restructuring with bank";
                case 3: return "Avoid taking any new loans for at least 12 months";
                default: return "Monitor credit report regularly";
            }
        } else if (score < 700) {
            switch (index) {
                case 1: return "Ensure all EMI payments are made on time going forward";
                case 2: return "Reduce debt-to-income ratio by paying down existing loans";
                case 3: return "Build emergency fund of 3-6 months expenses";
                default: return "Maintain good payment discipline";
            }
        } else {
            switch (index) {
                case 1: return "Continue maintaining excellent payment history";
                case 2: return "Consider diversifying credit portfolio for better score";
                case 3: return "Monitor credit report annually for any discrepancies";
                default: return "Keep credit utilization below 30%";
            }
        }
    }
}
