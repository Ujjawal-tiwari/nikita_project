package com.nikita.creditrisk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SPRING AI - RAG (Retrieval Augmented Generation) SERVICE
 * 
 * RAG enhances AI responses by:
 * 1. RETRIEVAL  - Searching the vector store for similar documents
 * 2. AUGMENTATION - Adding retrieved docs as context to the prompt
 * 3. GENERATION - AI generates grounded responses using context
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);
    private final VectorStore vectorStore;

    @Value("${credit-risk.vector-store.top-k:5}")
    private int topK;

    @Value("${credit-risk.vector-store.similarity-threshold:0.5}")
    private double similarityThreshold;

    public RAGService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieveRelevantDocuments(String query) {
        log.info("🔍 RAG RETRIEVAL: Searching for '{}'", 
                 query.length() > 80 ? query.substring(0, 80) + "..." : query);
        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(similarityThreshold);

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("  → Found {} relevant chunks", results.size());
            return results;
        } catch (Exception e) {
            log.warn("⚠ RAG retrieval failed (embeddings may be unavailable): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public String retrieveContext(String query) {
        List<Document> documents = retrieveRelevantDocuments(query);

        if (documents.isEmpty()) {
            log.warn("⚠ No RAG results. Providing built-in RBI policy context as fallback.");
            return getBuiltInPolicyContext();
        }

        String context = documents.stream()
            .map(doc -> {
                String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
                return "--- Source: " + source + " ---\n" + doc.getContent();
            })
            .collect(Collectors.joining("\n\n"));

        log.info("📋 RAG AUGMENTATION: {} characters from {} chunks", context.length(), documents.size());
        return context;
    }

    public String retrieveCreditRiskContext(int creditScore, double dtiRatio, int missedPayments) {
        StringBuilder query = new StringBuilder("Credit risk assessment ");
        if (creditScore < 550) query.append("poor credit score NPA classification provisioning ");
        else if (creditScore < 700) query.append("fair credit score risk factors ");
        else query.append("good credit score compliance ");
        if (dtiRatio > 50) query.append("debt to income ratio high DTI guidelines ");
        if (missedPayments > 0) query.append("missed payments default payment history ");
        query.append("RBI guidelines regulations");
        return retrieveContext(query.toString());
    }

    /**
     * Built-in RBI policy context used when vector store/embeddings are unavailable.
     * This ensures the AI always has policy context for grounded responses.
     */
    private String getBuiltInPolicyContext() {
        return """
            === RBI Credit Risk Guidelines (Key Provisions) ===
            
            1. CREDIT SCORE CLASSIFICATION (CIBIL Scale 300-900):
               - 750-900: Excellent - Low risk, eligible for best rates
               - 650-749: Good - Moderate risk, standard terms
               - 550-649: Fair - Elevated risk, higher interest rates, additional scrutiny
               - 300-549: Poor - High risk, may require collateral or co-signer
            
            2. DEBT-TO-INCOME (DTI) RATIO LIMITS:
               - RBI recommends DTI should not exceed 50%
               - DTI above 40% requires enhanced due diligence
               - DTI above 60% is classified as high-risk lending
               - Banks must maintain FOIR (Fixed Obligation to Income Ratio) records
            
            3. NPA (NON-PERFORMING ASSET) CLASSIFICATION:
               - Standard: Payments current, no overdue
               - Sub-Standard: Overdue 90+ days, provisions at 15%
               - Doubtful (1 year): Provisions at 25% of secured + 100% unsecured
               - Doubtful (3 years): Provisions at 40% of secured + 100% unsecured
               - Loss Assets: Written off, 100% provisioned
            
            4. PAYMENT HISTORY IMPACT:
               - 1-2 missed payments: Minor negative impact
               - 3+ missed payments: Significant score reduction, flagged for review
               - 6+ missed payments: Potential NPA classification
               - 90+ days overdue: Sub-standard asset classification per RBI
            
            5. EMI-TO-INCOME RATIO:
               - Should not exceed 40-50% of monthly income
               - Includes all existing EMIs plus proposed loan EMI
               - Self-employed: Additional 10% margin required
            
            6. RBI CIRCULAR DOR.STR.REC.12/21.04.048/2023-24:
               - Income Recognition, Asset Classification and Provisioning
               - Mandatory quarterly review of borrower accounts
               - Early warning signals must be documented
            """;
    }
}
