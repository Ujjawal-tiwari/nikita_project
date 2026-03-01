package com.nikita.creditrisk.model;

/**
 * Domain model representing a bank customer's profile.
 * Contains personal and financial data used for credit risk assessment.
 */
public class CustomerProfile {

    private String customerId;
    private String name;
    private int age;
    private double annualIncome;
    private double totalDebt;
    private int creditHistoryYears;
    private int numberOfLoans;
    private int missedPayments;
    private String employmentType; // SALARIED, SELF_EMPLOYED, BUSINESS
    private double monthlyEMI;
    private double savingsBalance;
    private boolean hasCollateral;

    public CustomerProfile() {}

    public CustomerProfile(String customerId, String name, int age, double annualIncome,
                           double totalDebt, int creditHistoryYears, int numberOfLoans,
                           int missedPayments, String employmentType, double monthlyEMI,
                           double savingsBalance, boolean hasCollateral) {
        this.customerId = customerId;
        this.name = name;
        this.age = age;
        this.annualIncome = annualIncome;
        this.totalDebt = totalDebt;
        this.creditHistoryYears = creditHistoryYears;
        this.numberOfLoans = numberOfLoans;
        this.missedPayments = missedPayments;
        this.employmentType = employmentType;
        this.monthlyEMI = monthlyEMI;
        this.savingsBalance = savingsBalance;
        this.hasCollateral = hasCollateral;
    }

    // --- Getters and Setters ---
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public double getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(double annualIncome) { this.annualIncome = annualIncome; }
    public double getTotalDebt() { return totalDebt; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }
    public int getCreditHistoryYears() { return creditHistoryYears; }
    public void setCreditHistoryYears(int creditHistoryYears) { this.creditHistoryYears = creditHistoryYears; }
    public int getNumberOfLoans() { return numberOfLoans; }
    public void setNumberOfLoans(int numberOfLoans) { this.numberOfLoans = numberOfLoans; }
    public int getMissedPayments() { return missedPayments; }
    public void setMissedPayments(int missedPayments) { this.missedPayments = missedPayments; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public double getMonthlyEMI() { return monthlyEMI; }
    public void setMonthlyEMI(double monthlyEMI) { this.monthlyEMI = monthlyEMI; }
    public double getSavingsBalance() { return savingsBalance; }
    public void setSavingsBalance(double savingsBalance) { this.savingsBalance = savingsBalance; }
    public boolean isHasCollateral() { return hasCollateral; }
    public void setHasCollateral(boolean hasCollateral) { this.hasCollateral = hasCollateral; }

    @Override
    public String toString() {
        return "CustomerProfile{customerId='" + customerId + "', name='" + name +
               "', age=" + age + ", annualIncome=" + annualIncome +
               ", totalDebt=" + totalDebt + ", creditHistoryYears=" + creditHistoryYears +
               ", numberOfLoans=" + numberOfLoans + ", missedPayments=" + missedPayments +
               ", employmentType='" + employmentType + "', monthlyEMI=" + monthlyEMI + "}";
    }
}
