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
 * RAG is a technique that enhances AI responses by:
 * 1. RETRIEVAL  - Searching the vector store for documents similar to the user's query
 * 2. AUGMENTATION - Adding retrieved documents as context to the AI prompt
 * 3. GENERATION - The AI generates responses using both the query AND retrieved context
 * 
 * This ensures the AI provides answers grounded in actual RBI policy documents
 * rather than relying solely on its training data.
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

    /**
     * RETRIEVAL STEP of RAG:
     * Performs semantic similarity search against the vector store.
     * Returns the most relevant document chunks for the given query.
     * 
     * @param query The search query (e.g., "What is the DTI ratio limit?")
     * @return List of relevant Document chunks with similarity scores
     */
    public List<Document> retrieveRelevantDocuments(String query) {
        log.info("🔍 RAG RETRIEVAL: Searching for documents relevant to: '{}'", 
                 query.length() > 80 ? query.substring(0, 80) + "..." : query);
        try {
            // Perform similarity search using vector embeddings
            // The query is automatically embedded and compared against stored vectors
            SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(similarityThreshold);

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            log.info("  → Found {} relevant document chunks (threshold: {})", 
                     results.size(), similarityThreshold);
            
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
                log.info("  → Chunk {}: source={}, length={}", i + 1, source, 
                         doc.getContent().length());
            }

            return results;
        } catch (Exception e) {
            log.error("❌ RAG retrieval failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * AUGMENTATION STEP of RAG:
     * Combines retrieved documents into a single context string
     * that will be injected into the AI prompt.
     * 
     * @param query The user's query
     * @return Formatted context string from retrieved documents
     */
    public String retrieveContext(String query) {
        List<Document> documents = retrieveRelevantDocuments(query);

        if (documents.isEmpty()) {
            log.warn("⚠ No relevant documents found for RAG context. Using empty context.");
            return "No specific policy documents found for this query.";
        }

        // Format retrieved chunks into a context block
        String context = documents.stream()
            .map(doc -> {
                String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
                return "--- Source: " + source + " ---\n" + doc.getContent();
            })
            .collect(Collectors.joining("\n\n"));

        log.info("📋 RAG AUGMENTATION: Built context with {} characters from {} chunks",
                 context.length(), documents.size());

        return context;
    }

    /**
     * Retrieves context specifically for credit risk policy queries.
     * Enhances the query with credit-risk-specific keywords for better retrieval.
     */
    public String retrieveCreditRiskContext(int creditScore, double dtiRatio, int missedPayments) {
        // Build a focused query for better RAG retrieval
        StringBuilder query = new StringBuilder();
        query.append("Credit risk assessment ");

        if (creditScore < 550) {
            query.append("poor credit score NPA classification provisioning ");
        } else if (creditScore < 700) {
            query.append("fair credit score risk factors improvement ");
        } else {
            query.append("good credit score compliance ");
        }

        if (dtiRatio > 50) {
            query.append("debt to income ratio high DTI guidelines ");
        }

        if (missedPayments > 0) {
            query.append("missed payments default payment history ");
        }

        query.append("RBI guidelines regulations");

        return retrieveContext(query.toString());
    }
}
