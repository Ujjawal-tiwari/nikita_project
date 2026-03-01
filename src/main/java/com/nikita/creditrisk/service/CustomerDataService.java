package com.nikita.creditrisk.service;

import com.nikita.creditrisk.model.CreditRiskScore;
import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.RiskFactor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Mock Customer Data Service.
 * In production, this would connect to Core Banking System (CBS) and Credit Bureau (CIBIL).
 * Provides sample customer profiles for demonstration.
 */
@Service
public class CustomerDataService {

    private final Map<String, CustomerProfile> customers = new HashMap<>();
    private final Map<String, CreditRiskScore> scores = new HashMap<>();
    private final Map<String, List<RiskFactor>> riskFactors = new HashMap<>();

    public CustomerDataService() {
        initializeSampleData();
    }

    private void initializeSampleData() {
        // --- Customer 1: HIGH RISK - Poor credit history ---
        customers.put("CUST001", new CustomerProfile(
            "CUST001", "Rajesh Kumar", 35, 480000, 850000,
            2, 5, 4, "SELF_EMPLOYED", 28000, 50000, false
        ));
        scores.put("CUST001", new CreditRiskScore("CUST001", 520, "HIGH", 177.0, 70.0));
        riskFactors.put("CUST001", Arrays.asList(
            new RiskFactor("High Missed Payments", "HIGH", 0.35, "4 missed payments in last 12 months significantly reduce credit score", "PAYMENT_HISTORY"),
            new RiskFactor("Excessive Debt-to-Income", "HIGH", 0.30, "DTI ratio of 177% far exceeds RBI limit of 50%", "DEBT_RATIO"),
            new RiskFactor("Short Credit History", "MEDIUM", 0.15, "Only 2 years of credit history - below preferred 3 years", "CREDIT_AGE"),
            new RiskFactor("Too Many Active Loans", "MEDIUM", 0.10, "5 active loans indicates over-leveraging", "CREDIT_MIX"),
            new RiskFactor("No Collateral", "MEDIUM", 0.10, "No secured assets to back outstanding loans", "CREDIT_MIX")
        ));

        // --- Customer 2: MEDIUM RISK - Fair credit ---
        customers.put("CUST002", new CustomerProfile(
            "CUST002", "Priya Sharma", 42, 960000, 400000,
            8, 3, 1, "SALARIED", 22000, 300000, true
        ));
        scores.put("CUST002", new CreditRiskScore("CUST002", 650, "MEDIUM", 41.6, 27.5));
        riskFactors.put("CUST002", Arrays.asList(
            new RiskFactor("One Missed Payment", "MEDIUM", 0.35, "1 missed payment in the last year moderately affects score", "PAYMENT_HISTORY"),
            new RiskFactor("Moderate DTI Ratio", "MEDIUM", 0.30, "DTI of 41.6% is within acceptable but elevated range", "DEBT_RATIO"),
            new RiskFactor("Strong Credit History", "LOW", 0.15, "8 years of credit history is excellent", "CREDIT_AGE"),
            new RiskFactor("Good Credit Mix", "LOW", 0.10, "Balanced mix of secured and unsecured loans", "CREDIT_MIX")
        ));

        // --- Customer 3: LOW RISK - Excellent credit ---
        customers.put("CUST003", new CustomerProfile(
            "CUST003", "Amit Patel", 50, 1800000, 300000,
            15, 2, 0, "SALARIED", 18000, 1200000, true
        ));
        scores.put("CUST003", new CreditRiskScore("CUST003", 810, "LOW", 16.6, 12.0));
        riskFactors.put("CUST003", Arrays.asList(
            new RiskFactor("Perfect Payment History", "LOW", 0.35, "Zero missed payments - excellent track record", "PAYMENT_HISTORY"),
            new RiskFactor("Low DTI Ratio", "LOW", 0.30, "DTI of 16.6% well below RBI threshold", "DEBT_RATIO"),
            new RiskFactor("Excellent Credit History", "LOW", 0.15, "15 years of consistent credit history", "CREDIT_AGE"),
            new RiskFactor("Strong Collateral", "LOW", 0.10, "Has collateral assets backing loans", "CREDIT_MIX")
        ));

        // --- Customer 4: HIGH RISK - NPA candidate ---
        customers.put("CUST004", new CustomerProfile(
            "CUST004", "Suresh Reddy", 28, 360000, 600000,
            1, 4, 6, "SELF_EMPLOYED", 25000, 20000, false
        ));
        scores.put("CUST004", new CreditRiskScore("CUST004", 380, "HIGH", 166.6, 83.3));
        riskFactors.put("CUST004", Arrays.asList(
            new RiskFactor("Severe Payment Default", "HIGH", 0.35, "6 missed payments - potential NPA classification", "PAYMENT_HISTORY"),
            new RiskFactor("Critical DTI Ratio", "HIGH", 0.30, "DTI of 166.6% is extremely high risk", "DEBT_RATIO"),
            new RiskFactor("Minimal Credit History", "HIGH", 0.15, "Only 1 year of credit history", "CREDIT_AGE"),
            new RiskFactor("EMI Exceeds Income Capacity", "HIGH", 0.10, "EMI/Income at 83.3% - severe financial stress", "NEW_CREDIT"),
            new RiskFactor("No Savings Buffer", "HIGH", 0.10, "Very low savings with high debt", "CREDIT_MIX")
        ));
    }

    public CustomerProfile getCustomerProfile(String customerId) {
        return customers.get(customerId);
    }

    public CreditRiskScore getCreditScore(String customerId) {
        return scores.get(customerId);
    }

    public List<RiskFactor> getRiskFactors(String customerId) {
        return riskFactors.getOrDefault(customerId, Collections.emptyList());
    }

    public List<String> getAllCustomerIds() {
        return new ArrayList<>(customers.keySet());
    }
}
