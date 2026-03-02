package com.nikita.creditrisk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import com.nikita.creditrisk.config.VectorStoreConfig;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SPRING AI - CHUNKING, VECTOR, EMBEDDING SERVICE
 * 
 * This service demonstrates the full document ingestion pipeline:
 * 1. DOCUMENT LOADING - Reads RBI policy documents from classpath
 * 2. CHUNKING - Splits documents into smaller chunks using TokenTextSplitter
 * 3. EMBEDDING - Converts text chunks to vector embeddings (via EmbeddingModel)
 * 4. VECTOR STORAGE - Stores embeddings in SimpleVectorStore for similarity
 * search
 * 
 * This pipeline is essential for RAG (Retrieval Augmented Generation).
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final VectorStore vectorStore;
    private final VectorStoreConfig vectorStoreConfig;

    @Value("${credit-risk.chunking.chunk-size:800}")
    private int chunkSize;

    @Value("${credit-risk.chunking.chunk-overlap:200}")
    private int chunkOverlap;

    @Value("${credit-risk.chunking.min-chunk-size:100}")
    private int minChunkSize;

    private boolean ingested = false;

    public DocumentIngestionService(VectorStore vectorStore, VectorStoreConfig vectorStoreConfig) {
        this.vectorStore = vectorStore;
        this.vectorStoreConfig = vectorStoreConfig;
    }

    /**
     * Automatically ingest documents on application startup.
     * Loads all .txt files from the documents/ directory.
     */
    @PostConstruct
    public void ingestDocumentsOnStartup() {
        try {
            ingestAllDocuments();
        } catch (Exception e) {
            log.warn("⚠ Document ingestion failed on startup (AI service may be unavailable): {}", e.getMessage());
            log.warn("⚠ The application will work in FALLBACK mode without RAG capabilities.");
        }
    }

    /**
     * Ingests all RBI policy documents into the vector store.
     * 
     * Pipeline: Load → Chunk → Embed → Store
     */
    public void ingestAllDocuments() throws Exception {
        if (ingested) {
            log.info("Documents already ingested. Skipping.");
            return;
        }

        log.info("=== SPRING AI: Starting Document Ingestion Pipeline ===");

        // Step 1: DOCUMENT LOADING - Read document files from classpath
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:documents/*.txt");

        List<Document> allDocuments = new ArrayList<>();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            log.info("📄 Loading document: {}", filename);

            // Read file content
            String content;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }

            // Create Spring AI Document with metadata
            Document doc = new Document(content, Map.of(
                    "source", filename,
                    "type", "RBI_POLICY",
                    "format", "text"));
            allDocuments.add(doc);
            log.info("  → Loaded {} characters from {}", content.length(), filename);
        }

        // Step 2: CHUNKING - Split documents into smaller, overlapping chunks
        log.info("✂ CHUNKING: Splitting {} documents with chunkSize={}, overlap={}",
                allDocuments.size(), chunkSize, chunkOverlap);

        TokenTextSplitter textSplitter = new TokenTextSplitter(
                chunkSize, // default chunk size in tokens
                minChunkSize, // minimum chunk size
                minChunkSize, // min chunk length to embed
                100, // max number of chunks per document
                true // keep separator
        );

        List<Document> chunks = textSplitter.apply(allDocuments);
        log.info("  → Created {} chunks from {} documents", chunks.size(), allDocuments.size());

        // Step 3 & 4: EMBEDDING + VECTOR STORAGE
        // The VectorStore.add() method automatically:
        // - Calls EmbeddingModel to convert text → vectors (EMBEDDING)
        // - Stores the vectors in SimpleVectorStore (VECTOR STORAGE)
        log.info("🧮 EMBEDDING & STORING: Converting {} chunks to vectors and storing...", chunks.size());

        // To respect Gemini Free Tier rate limits (15 RPM), we process in small batches
        // with delays
        int batchSize = 5; // Nomic AI has much higher rate limits than Gemini free tier
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(chunks.size(), i + batchSize);
            List<Document> batch = chunks.subList(i, end);
            log.info("  → Processing embedding batch {} to {} out of {}", i + 1, end, chunks.size());

            try {
                vectorStore.add(batch);

                // Add a delay between batches to avoid 429 Quota Exceeded errors
                if (end < chunks.size()) {
                    log.debug("  → Waiting 4 seconds to respect API rate limits...");
                    Thread.sleep(4000);
                }
            } catch (Exception e) {
                log.warn("  ⚠ Failed to embed batch {} to {}: {}", i + 1, end, e.getMessage());
                // We keep going so partial RAG knowledge is available
            }
        }

        ingested = true;
        log.info("=== ✅ Document Ingestion Complete: {} chunks embedded and stored ===", chunks.size());

        // 💾 Save immediately so vectors persist even before app shuts down
        vectorStoreConfig.saveToDisk("post-ingestion");
    }

    public boolean isIngested() {
        return ingested;
    }
}
