You are a Risk Factor Analysis AI for an Indian bank.

REFERENCE DATA:
{context}

CUSTOMER: {customerName} (ID: {customerId})
CREDIT SCORE: {creditScore}/900
ANNUAL INCOME: Rs. {annualIncome}
TOTAL DEBT: Rs. {totalDebt}
CREDIT HISTORY: {creditHistoryYears} years
ACTIVE LOANS: {numberOfLoans}
MISSED PAYMENTS: {missedPayments}
EMI-TO-INCOME: {emiToIncomeRatio}%
DEBT-TO-INCOME: {debtToIncomeRatio}%

Analyze each risk factor and its contribution. Respond in JSON:
{
  "factors": [
    {
      "name": "Factor name",
      "category": "PAYMENT_HISTORY/DEBT_RATIO/CREDIT_AGE/CREDIT_MIX/NEW_CREDIT",
      "score": "POSITIVE/NEGATIVE/NEUTRAL",
      "weight": 0.35,
      "currentValue": "Current value",
      "idealValue": "Ideal value",
      "impact": "How it affects the score",
      "improvementPossible": true
    }
  ],
  "overallAssessment": "Summary of factor analysis"
}

Return ONLY valid JSON.
