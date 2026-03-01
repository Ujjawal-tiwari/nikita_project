/**
 * Credit Risk Explanation Service - Frontend JavaScript
 * Handles API calls and UI updates for all analysis modes
 */

const API_BASE = '/api/risk';

// ===== MAIN ANALYZE FUNCTION =====
async function analyzeRisk() {
    const customerId = document.getElementById('customerSelect').value;
    const mode = document.getElementById('analysisMode').value;
    const btn = document.getElementById('analyzeBtn');

    // Show loading
    btn.disabled = true;
    document.getElementById('loadingOverlay').style.display = 'flex';
    document.getElementById('resultsSection').style.display = 'none';

    // Update loading text based on mode
    const modeLabels = {
        explain: 'Analyzing with Spring AI (RAG + Prompt)...',
        langchain: 'Running LangChain Sequential Chain...',
        langgraph: 'Executing LangGraph State Machine...',
        mcp: 'Processing with MCP Function Calling...'
    };
    document.getElementById('loadingText').textContent = modeLabels[mode] || 'Analyzing...';

    try {
        // Fetch score first
        const scoreRes = await fetch(`${API_BASE}/score/${customerId}`);
        const scoreData = await scoreRes.json();

        let resultData;

        switch (mode) {
            case 'explain':
                resultData = await analyzeSpringAI(customerId);
                renderSpringAIResults(resultData, scoreData);
                break;
            case 'langchain':
                resultData = await analyzeLangChain(customerId);
                renderLangChainResults(resultData, scoreData);
                break;
            case 'langgraph':
                resultData = await analyzeLangGraph(customerId);
                renderLangGraphResults(resultData, scoreData);
                break;
            case 'mcp':
                resultData = await analyzeMCP(customerId);
                renderMCPResults(resultData, scoreData);
                break;
        }

        document.getElementById('resultsSection').style.display = 'grid';
        document.getElementById('resultsSection').classList.add('animate-in');
    } catch (error) {
        console.error('Analysis failed:', error);
        showError(error.message || 'Analysis failed. Please try again.');
    } finally {
        btn.disabled = false;
        document.getElementById('loadingOverlay').style.display = 'none';
    }
}

// ===== API CALLS =====

async function analyzeSpringAI(customerId) {
    const res = await fetch(`${API_BASE}/explain`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerId })
    });
    if (!res.ok) throw new Error('Spring AI analysis failed');
    return res.json();
}

async function analyzeLangChain(customerId) {
    const res = await fetch(`${API_BASE}/langchain`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerId })
    });
    if (!res.ok) throw new Error('LangChain analysis failed');
    return res.json();
}

async function analyzeLangGraph(customerId) {
    const res = await fetch(`${API_BASE}/langgraph`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerId })
    });
    if (!res.ok) throw new Error('LangGraph analysis failed');
    return res.json();
}

async function analyzeMCP(customerId) {
    const res = await fetch(`${API_BASE}/mcp`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerId })
    });
    if (!res.ok) throw new Error('MCP analysis failed');
    return res.json();
}

// ===== RENDER FUNCTIONS =====

function renderScoreGauge(score) {
    const maxScore = 900;
    const minScore = 300;
    const normalized = (score - minScore) / (maxScore - minScore);
    const arcLength = normalized * 251; // 251 is total arc length

    document.getElementById('scoreValue').textContent = score;
    const arc = document.getElementById('gaugeArc');
    arc.style.transition = 'stroke-dasharray 1.5s ease-out';
    arc.setAttribute('stroke-dasharray', `${arcLength} 251`);
}

function renderRiskBadge(riskLevel) {
    const badge = document.getElementById('riskBadge');
    badge.textContent = riskLevel + ' Risk';
    badge.className = 'risk-badge';
    switch ((riskLevel || '').toUpperCase()) {
        case 'HIGH': badge.classList.add('risk-high'); break;
        case 'MEDIUM': badge.classList.add('risk-medium'); break;
        case 'LOW': badge.classList.add('risk-low'); break;
    }
}

function renderScoreMeta(scoreData) {
    const meta = document.getElementById('scoreMeta');
    meta.innerHTML = `
        <div class="meta-row"><span>DTI Ratio</span><span>${scoreData.debtToIncomeRatio || 0}%</span></div>
        <div class="meta-row"><span>EMI/Income</span><span>${scoreData.emiToIncomeRatio || 0}%</span></div>
        <div class="meta-row"><span>Risk Level</span><span>${scoreData.riskLevel || '--'}</span></div>
    `;
}

function renderProcessingMode(mode) {
    const el = document.getElementById('processingMode');
    const labels = { AI: 'AI Powered', FALLBACK: 'Fallback Mode', LANGCHAIN: 'LangChain', LANGGRAPH: 'LangGraph', MCP: 'MCP' };
    const classes = { AI: 'mode-ai', FALLBACK: 'mode-fallback', LANGCHAIN: 'mode-langchain', LANGGRAPH: 'mode-langgraph', MCP: 'mode-mcp' };
    el.textContent = labels[mode] || mode;
    el.className = 'processing-mode ' + (classes[mode] || 'mode-ai');
}

function renderFactors(factors) {
    const list = document.getElementById('factorsList');
    if (!factors || !factors.length) {
        list.innerHTML = '<p style="color: var(--text-muted);">No risk factors available.</p>';
        return;
    }
    list.innerHTML = factors.map(f => `
        <div class="factor-card">
            <div class="factor-header">
                <span class="factor-name">${f.factor || f.factorName || 'Unknown'}</span>
                <span class="impact-badge impact-${(f.impact || 'medium').toLowerCase()}">${f.impact || 'N/A'}</span>
            </div>
            <p class="factor-desc">${f.description || ''}</p>
        </div>
    `).join('');
}

function renderViolations(violations, complianceStatus) {
    // Compliance badge
    const badge = document.getElementById('complianceBadge');
    badge.textContent = (complianceStatus || 'UNKNOWN').replace(/_/g, ' ');
    badge.className = 'compliance-badge';
    if (complianceStatus === 'COMPLIANT') badge.classList.add('compliance-compliant');
    else if (complianceStatus === 'PARTIALLY_COMPLIANT') badge.classList.add('compliance-partial');
    else badge.classList.add('compliance-non');

    const list = document.getElementById('violationsList');
    if (!violations || !violations.length) {
        list.innerHTML = '<p style="color: var(--text-muted);">No policy violations detected.</p>';
        return;
    }
    list.innerHTML = violations.map(v => `
        <div class="violation-card severity-${(v.severity || 'medium').toLowerCase()}">
            <div class="violation-info">
                <h4>${v.policyName || 'Unknown Policy'}</h4>
                <p>${v.section ? v.section + ' — ' : ''}${v.violation || ''}</p>
            </div>
            <span class="impact-badge impact-${(v.severity || 'medium').toLowerCase()}">${v.severity || 'N/A'}</span>
        </div>
    `).join('');
}

function renderRecommendations(recs) {
    const list = document.getElementById('recommendationsList');
    if (!recs || !recs.length) {
        list.innerHTML = '<p style="color: var(--text-muted);">No recommendations available.</p>';
        return;
    }
    list.innerHTML = recs.map((r, i) => `
        <div class="recommendation-item">
            <span class="rec-number">${i + 1}</span>
            <span>${r}</span>
        </div>
    `).join('');
}

// ===== SPRING AI RESULTS =====
function renderSpringAIResults(data, scoreData) {
    renderScoreGauge(data.creditScore || scoreData.score);
    renderRiskBadge(data.riskLevel || scoreData.riskLevel);
    renderScoreMeta(scoreData);
    renderProcessingMode(data.processingMode || 'AI');

    const content = document.getElementById('explanationContent');
    content.innerHTML = `
        <p><strong>Score Interpretation:</strong> ${data.scoreInterpretation || 'N/A'}</p>
        <p>${data.summary || ''}</p>
    `;

    renderFactors(data.keyFactors);
    renderViolations(data.policyViolations, data.complianceStatus);
    renderRecommendations(data.recommendations);

    document.getElementById('workflowPanel').style.display = 'none';
}

// ===== LANGCHAIN RESULTS =====
function renderLangChainResults(data, scoreData) {
    const score = data.creditScore ? data.creditScore.score : scoreData.score;
    renderScoreGauge(score);
    renderRiskBadge(data.riskCategory || scoreData.riskLevel);
    renderScoreMeta(scoreData);
    renderProcessingMode('LANGCHAIN');

    const content = document.getElementById('explanationContent');
    content.innerHTML = `
        <p><strong>Score Analysis:</strong> ${data.scoreAnalysis || 'N/A'}</p>
        <p>${data.explanation || 'Chain execution completed.'}</p>
    `;

    // Show workflow panel with chain steps
    document.getElementById('workflowPanel').style.display = 'block';
    const workflow = document.getElementById('workflowContent');
    const steps = ['scoreAnalysis', 'policyRetrieval', 'explanationGeneration', 'recommendation'];
    const stepLabels = { scoreAnalysis: 'Score Analysis', policyRetrieval: 'Policy Retrieval (RAG)', explanationGeneration: 'Explanation Generation', recommendation: 'Recommendation' };
    workflow.innerHTML = steps.map((s, i) => {
        const status = data[s + '_status'] || 'COMPLETED';
        const icon = status === 'COMPLETED' ? '✅' : '❌';
        const cls = status === 'COMPLETED' ? 'step-completed' : 'step-failed';
        return `
            <div class="workflow-step">
                <div class="step-icon ${cls}">${i + 1}</div>
                <div class="step-info">
                    <h4>${stepLabels[s]} ${icon}</h4>
                    <p>Status: ${status}</p>
                </div>
            </div>`;
    }).join('');

    // Render recommendations from text
    if (data.recommendations) {
        const recs = data.recommendations.split(/\d+\.\s/).filter(r => r.trim());
        renderRecommendations(recs.length ? recs : [data.recommendations]);
    } else {
        renderRecommendations([]);
    }

    renderFactors([]);
    renderViolations([], 'UNKNOWN');
}

// ===== LANGGRAPH RESULTS =====
function renderLangGraphResults(data, scoreData) {
    renderScoreGauge(data.creditScore || scoreData.score);
    renderRiskBadge(data.riskLevel || scoreData.riskLevel);
    renderScoreMeta(scoreData);
    renderProcessingMode('LANGGRAPH');

    const content = document.getElementById('explanationContent');
    content.innerHTML = `
        <p>${data.explanation || 'Graph execution completed.'}</p>
    `;

    // Show workflow panel with graph nodes
    document.getElementById('workflowPanel').style.display = 'block';
    const workflow = document.getElementById('workflowContent');
    const visitedNodes = data.visitedNodes || [];
    const metadata = data.metadata || {};
    workflow.innerHTML = `
        <p style="margin-bottom: 12px; color: var(--text-secondary);">
            <strong>Audit Type:</strong> ${metadata.auditType || 'N/A'} | 
            <strong>Level:</strong> ${metadata.auditLevel || 'N/A'} |
            <strong>Time:</strong> ${metadata.totalTimeMs || 0}ms
        </p>
        ${visitedNodes.map((n, i) => {
            const failed = n.includes('FAILED');
            const cls = failed ? 'step-failed' : 'step-completed';
            const icon = failed ? '❌' : '✅';
            return `
                <div class="workflow-step">
                    <div class="step-icon ${cls}">${i + 1}</div>
                    <div class="step-info">
                        <h4>${n} ${icon}</h4>
                    </div>
                </div>`;
        }).join('')}
    `;

    if (data.recommendations) {
        const recs = data.recommendations.split(/\d+\.\s/).filter(r => r.trim());
        renderRecommendations(recs.length ? recs : [data.recommendations]);
    } else {
        renderRecommendations([]);
    }

    renderFactors([]);
    renderViolations([], data.complianceStatus || 'UNKNOWN');
}

// ===== MCP RESULTS =====
function renderMCPResults(data, scoreData) {
    renderScoreGauge(scoreData.score);
    renderRiskBadge(scoreData.riskLevel);
    renderScoreMeta(scoreData);
    renderProcessingMode('MCP');

    const content = document.getElementById('explanationContent');
    const analysis = data.analysis || 'MCP function calling analysis completed.';
    // Convert markdown-style text to paragraphs
    const paragraphs = analysis.split('\n').filter(p => p.trim());
    content.innerHTML = paragraphs.map(p => {
        if (p.startsWith('#')) return `<h4 style="margin-top:12px;">${p.replace(/^#+\s*/, '')}</h4>`;
        if (p.startsWith('- ') || p.startsWith('* ')) return `<p style="padding-left:16px;">• ${p.substring(2)}</p>`;
        return `<p>${p}</p>`;
    }).join('');

    renderFactors([]);
    renderViolations([], 'UNKNOWN');
    renderRecommendations([]);
    document.getElementById('workflowPanel').style.display = 'none';
}

// ===== ERROR DISPLAY =====
function showError(message) {
    document.getElementById('resultsSection').style.display = 'grid';
    document.getElementById('explanationContent').innerHTML = `
        <div style="color: var(--accent-red); padding: 16px; background: rgba(239,68,68,0.1); border-radius: 8px;">
            <strong>⚠ Error:</strong> ${message}
            <p style="margin-top: 8px; color: var(--text-secondary);">The system will try to use fallback responses. Please check the server logs.</p>
        </div>
    `;
    renderScoreGauge(300);
    renderRiskBadge('UNKNOWN');
}

// ===== INITIALIZATION =====
document.addEventListener('DOMContentLoaded', () => {
    console.log('Credit Risk Explanation Service - Dashboard loaded');
    // Add keyboard shortcut
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && e.ctrlKey) analyzeRisk();
    });
});
