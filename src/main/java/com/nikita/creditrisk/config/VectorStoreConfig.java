package com.nikita.creditrisk.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SPRING AI - VECTOR STORE & EMBEDDING CONFIGURATION
 * 
 * Configures:
 * 1. VectorStore (SimpleVectorStore) - In-memory vector database for storing embeddings
 * 2. EmbeddingModel - Auto-configured by Spring AI OpenAI starter (uses Gemini text-embedding-004)
 * 
 * The VectorStore stores document chunks as numerical vectors (embeddings),
 * enabling semantic similarity search for the RAG pipeline.
 */
@Configuration
public class VectorStoreConfig {

    /**
     * Creates an in-memory SimpleVectorStore bean.
     * 
     * SPRING AI - VECTOR STORE:
     * SimpleVectorStore stores embeddings in-memory using a ConcurrentHashMap.
     * For production, use PGVector, ChromaDB, Pinecone, etc.
     * 
     * @param embeddingModel Auto-configured by Spring AI (Gemini text-embedding-004)
     * @return Configured VectorStore instance
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }
}
