package com.nikita.creditrisk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;

/**
 * VECTOR STORE & EMBEDDING CONFIGURATION
 *
 * EmbeddingModel: Nomic AI (free, OpenAI-compatible, nomic-embed-text-v1.5,
 * 768-dim)
 * VectorStore: SimpleVectorStore with FILE PERSISTENCE
 * - Saved to: ./data/credit-risk-vectors.json (open in any editor to inspect!)
 * - Loaded on startup if the file exists
 * - Saved immediately after document ingestion + on graceful shutdown
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${nomic.ai.api-key}")
    private String nomicApiKey;

    @Value("${credit-risk.vector-store.file-path:./data/credit-risk-vectors.json}")
    private String vectorStorePath;

    // Kept as a field so saveToDisk() can be called from DocumentIngestionService
    private SimpleVectorStore simpleVectorStore;
    private File vectorFile;

    /**
     * Nomic AI Embedding Model — @Primary so it takes precedence over the
     * Groq/OpenAI autoconfigured embedding (Groq has no embeddings endpoint).
     */
    @Bean
    @Primary
    public EmbeddingModel nomicEmbeddingModel() {
        OpenAiApi nomicApi = new OpenAiApi(
                "https://api-atlas.nomic.ai/v1",
                nomicApiKey);
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .withModel("nomic-embed-text-v1.5")
                .build();
        return new OpenAiEmbeddingModel(nomicApi, MetadataMode.EMBED, options);
    }

    /**
     * File-persisted SimpleVectorStore.
     * Loads existing vectors on startup; saves on shutdown and after ingestion.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        this.vectorFile = new File(vectorStorePath);
        this.simpleVectorStore = new SimpleVectorStore(embeddingModel);

        if (vectorFile.exists() && vectorFile.length() > 10) {
            log.info("📂 Loading existing vector store from: {}", vectorFile.getAbsolutePath());
            simpleVectorStore.load(vectorFile);
            log.info("✅ Vector store loaded successfully from disk.");
        } else {
            log.info("🆕 No existing vector store. Will embed documents and save to: {}", vectorFile.getAbsolutePath());
            vectorFile.getParentFile().mkdirs(); // create ./data/ directory
        }

        // Also save on graceful shutdown (Ctrl+C, SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> saveToDisk("shutdown")));

        return simpleVectorStore;
    }

    /**
     * Called by DocumentIngestionService after successful ingestion to persist
     * immediately.
     */
    public void saveToDisk(String reason) {
        if (simpleVectorStore == null || vectorFile == null)
            return;
        try {
            log.info("💾 Saving vector store to disk ({})...", reason);
            simpleVectorStore.save(vectorFile);
            log.info("✅ Saved! File size: {} bytes — open {} to inspect vectors.",
                    vectorFile.length(), vectorFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("❌ Failed to save vector store: {}", e.getMessage());
        }
    }
}
