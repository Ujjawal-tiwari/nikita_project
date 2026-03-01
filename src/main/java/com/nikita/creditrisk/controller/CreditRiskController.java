package com.nikita.creditrisk.controller;

import com.nikita.creditrisk.model.RiskExplanationRequest;
import com.nikita.creditrisk.model.RiskExplanationResponse;
import com.nikita.creditrisk.model.RiskFactor;
import com.nikita.creditrisk.model.CreditRiskScore;
import com.nikita.creditrisk.service.CreditRiskService;
import com.nikita.creditrisk.service.CustomerDataService;
import com.nikita.creditrisk.service.DocumentIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Credit Risk Explanation Service.
 * 
 * Endpoints demonstrate all concepts:
 * - /api/risk/explain          → Spring AI (RAG + Prompt + Response Formatting + Error Handling)
 * - /api/risk/langchain        → LangChain Sequential Chain
 * - /api/risk/langgraph        → LangGraph State Machine
 * - /api/risk/mcp              → MCP Function Calling
 * - /api/risk/compliance       → Compliance Check
 * - /api/risk/recommendations  → Risk Reduction
 * - /api/risk/score/{id}       → Score Lookup
 * - /api/risk/factors/{id}     → Risk Factors
 * - /api/risk/ingest           → Document Ingestion
 */
@RestController
@RequestMapping("/api/risk")
@CrossOrigin(origins = "*")
public class CreditRiskController {

    private static final Logger log = LoggerFactory.getLogger(CreditRiskController.class);

    private final CreditRiskService creditRiskService;
    private final CustomerDataService customerDataService;
    private final DocumentIngestionService documentIngestionService;

    public CreditRiskController(CreditRiskService creditRiskService,
                                 CustomerDataService customerDataService,
                                 DocumentIngestionService documentIngestionService) {
        this.creditRiskService = creditRiskService;
        this.customerDataService = customerDataService;
        this.documentIngestionService = documentIngestionService;
    }

    /**
     * SPRING AI - Full risk explanation using RAG + Prompt + Response Formatting
     */
    @PostMapping("/explain")
    public ResponseEntity<RiskExplanationResponse> explainRisk(@RequestBody RiskExplanationRequest request) {
        log.info("📥 POST /api/risk/explain - Customer: {}", request.getCustomerId());
        RiskExplanationResponse response = creditRiskService.explainRisk(request.getCustomerId());
        return ResponseEntity.ok(response);
    }

    /**
     * LANGCHAIN - Sequential chain analysis
     */
    @PostMapping("/langchain")
    public ResponseEntity<Map<String, Object>> explainWithLangChain(@RequestBody RiskExplanationRequest request) {
        log.info("📥 POST /api/risk/langchain - Customer: {}", request.getCustomerId());
        Map<String, Object> result = creditRiskService.explainWithLangChain(request.getCustomerId());
        return ResponseEntity.ok(result);
    }

    /**
     * LANGGRAPH - State graph workflow analysis
     */
    @PostMapping("/langgraph")
    public ResponseEntity<Map<String, Object>> explainWithLangGraph(@RequestBody RiskExplanationRequest request) {
        log.info("📥 POST /api/risk/langgraph - Customer: {}", request.getCustomerId());
        Map<String, Object> result = creditRiskService.explainWithLangGraph(request.getCustomerId());
        return ResponseEntity.ok(result);
    }

    /**
     * MCP - Function calling analysis
     */
    @PostMapping("/mcp")
    public ResponseEntity<Map<String, String>> explainWithMCP(@RequestBody RiskExplanationRequest request) {
        log.info("📥 POST /api/risk/mcp - Customer: {}", request.getCustomerId());
        String result = creditRiskService.explainWithMCP(request.getCustomerId());
        return ResponseEntity.ok(Map.of("customerId", request.getCustomerId(),
                                        "analysis", result,
                                        "processingMode", "MCP"));
    }

    /**
     * Compliance check endpoint
     */
    @PostMapping("/compliance")
    public ResponseEntity<Map<String, String>> checkCompliance(@RequestBody RiskExplanationRequest request) {
        log.info("📥 POST /api/risk/compliance - Customer: {}", request.getCustomerId());
        String result = creditRiskService.getComplianceCheck(request.getCustomerId());
        return ResponseEntity.ok(Map.of("customerId", request.getCustomerId(),
                                        "compliance", result));
    }

    /**
     * Risk reduction recommendations
     */
    @PostMapping("/recommendations")
    public ResponseEntity<Map<String, String>> getRecommendations(@RequestBody RiskExplanationRequest request) {
        log.info("📥 POST /api/risk/recommendations - Customer: {}", request.getCustomerId());
        String result = creditRiskService.getRecommendations(request.getCustomerId());
        return ResponseEntity.ok(Map.of("customerId", request.getCustomerId(),
                                        "recommendations", result));
    }

    /**
     * Get credit score for a customer
     */
    @GetMapping("/score/{customerId}")
    public ResponseEntity<CreditRiskScore> getCreditScore(@PathVariable String customerId) {
        log.info("📥 GET /api/risk/score/{}", customerId);
        CreditRiskScore score = customerDataService.getCreditScore(customerId);
        if (score == null) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
        return ResponseEntity.ok(score);
    }

    /**
     * Get risk factors for a customer
     */
    @GetMapping("/factors/{customerId}")
    public ResponseEntity<List<RiskFactor>> getRiskFactors(@PathVariable String customerId) {
        log.info("📥 GET /api/risk/factors/{}", customerId);
        List<RiskFactor> factors = customerDataService.getRiskFactors(customerId);
        return ResponseEntity.ok(factors);
    }

    /**
     * Get list of all available customer IDs
     */
    @GetMapping("/customers")
    public ResponseEntity<List<String>> getCustomers() {
        return ResponseEntity.ok(customerDataService.getAllCustomerIds());
    }

    /**
     * Trigger document ingestion (re-ingest documents)
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestDocuments() {
        log.info("📥 POST /api/risk/ingest - Re-ingesting documents");
        try {
            documentIngestionService.ingestAllDocuments();
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Documents ingested successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("status", "FAILED", "message", e.getMessage()));
        }
    }
}
