package com.nikita.creditrisk.mcp;

import com.nikita.creditrisk.model.CreditRiskScore;
import com.nikita.creditrisk.model.CustomerProfile;
import com.nikita.creditrisk.model.RiskFactor;
import com.nikita.creditrisk.service.CustomerDataService;
import com.nikita.creditrisk.service.RAGService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP (MODEL CONTEXT PROTOCOL) - TOOL PROVIDER
 * 
 * MCP allows AI models to call back into the application to retrieve data
 * or perform actions. In Spring AI, this is implemented via Function Calling.
 * 
 * Each @Bean method defines a "tool" that the AI model can invoke:
 * 1. lookupCreditScore   → Retrieves a customer's credit score from the system
 * 2. searchRBIPolicies   → Searches the vector store for relevant RBI policies
 * 3. analyzeRiskFactors  → Returns detailed risk factor breakdown
 * 4. getCustomerProfile  → Retrieves full customer profile
 * 
 * When the AI needs data, it calls these functions automatically.
 * The results are sent back to the AI to generate informed responses.
 */
@Configuration
public class MCPToolProvider {

    private static final Logger log = LoggerFactory.getLogger(MCPToolProvider.class);

    // --- MCP Tool Request/Response Records ---
    public record CreditScoreRequest(String customerId) {}
    public record CreditScoreResponse(String customerId, int score, String riskLevel,
                                       double dtiRatio, double emiRatio, String message) {}

    public record PolicySearchRequest(String query) {}
    public record PolicySearchResponse(String context, int chunksFound, String message) {}

    public record RiskFactorRequest(String customerId) {}
    public record RiskFactorResponse(List<RiskFactor> factors, String summary) {}

    public record CustomerProfileRequest(String customerId) {}
    public record CustomerProfileResponse(CustomerProfile profile, String message) {}

    /**
     * MCP TOOL 1: lookupCreditScore
     * AI can call this to get a customer's credit score from the banking system.
     */
    @Bean
    @Description("Look up the credit score and risk metrics for a customer by their ID. " +
                 "Returns the CIBIL score (300-900), risk level, DTI ratio, and EMI ratio.")
    public Function<CreditScoreRequest, CreditScoreResponse> lookupCreditScore(
            CustomerDataService customerDataService) {
        return request -> {
            log.info("🔧 MCP TOOL CALL: lookupCreditScore({})", request.customerId());
            CreditRiskScore score = customerDataService.getCreditScore(request.customerId());
            if (score == null) {
                return new CreditScoreResponse(request.customerId(), 0, "UNKNOWN",
                    0, 0, "Customer not found");
            }
            return new CreditScoreResponse(
                score.getCustomerId(), score.getScore(), score.getRiskLevel(),
                score.getDebtToIncomeRatio(), score.getEmiToIncomeRatio(),
                "Score retrieved successfully"
            );
        };
    }

    /**
     * MCP TOOL 2: searchRBIPolicies
     * AI can call this to search the RAG vector store for relevant RBI policies.
     */
    @Bean
    @Description("Search the RBI policy knowledge base for regulations relevant to the given query. " +
                 "Returns matching policy text chunks retrieved via semantic similarity search.")
    public Function<PolicySearchRequest, PolicySearchResponse> searchRBIPolicies(
            RAGService ragService) {
        return request -> {
            log.info("🔧 MCP TOOL CALL: searchRBIPolicies('{}')", request.query());
            String context = ragService.retrieveContext(request.query());
            int chunks = context.split("--- Source:").length - 1;
            return new PolicySearchResponse(context, Math.max(chunks, 0),
                "Policy search completed");
        };
    }

    /**
     * MCP TOOL 3: analyzeRiskFactors
     * AI can call this to get the risk factor breakdown for a customer.
     */
    @Bean
    @Description("Analyze and return the detailed risk factors for a customer including " +
                 "payment history, debt ratio, credit age, and credit mix impacts.")
    public Function<RiskFactorRequest, RiskFactorResponse> analyzeRiskFactors(
            CustomerDataService customerDataService) {
        return request -> {
            log.info("🔧 MCP TOOL CALL: analyzeRiskFactors({})", request.customerId());
            List<RiskFactor> factors = customerDataService.getRiskFactors(request.customerId());
            String summary = factors.isEmpty() ? "No risk factors found"
                : factors.size() + " risk factors identified";
            return new RiskFactorResponse(factors, summary);
        };
    }

    /**
     * MCP TOOL 4: getCustomerProfile
     * AI can call this to retrieve the full customer profile.
     */
    @Bean
    @Description("Retrieve the complete customer profile including personal information, " +
                 "financial data, employment type, and loan details.")
    public Function<CustomerProfileRequest, CustomerProfileResponse> getCustomerProfile(
            CustomerDataService customerDataService) {
        return request -> {
            log.info("🔧 MCP TOOL CALL: getCustomerProfile({})", request.customerId());
            CustomerProfile profile = customerDataService.getCustomerProfile(request.customerId());
            if (profile == null) {
                return new CustomerProfileResponse(null, "Customer not found");
            }
            return new CustomerProfileResponse(profile, "Profile retrieved successfully");
        };
    }
}
