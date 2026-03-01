package com.nikita.creditrisk.langgraph;

/**
 * LANGGRAPH - NODE INTERFACE
 * 
 * Each node in the LangGraph represents a processing step.
 * Nodes are functional units that:
 * 1. Read data from the GraphState
 * 2. Perform some processing (AI call, data lookup, business logic)
 * 3. Write results back to the GraphState
 * 4. Return the updated state
 */
@FunctionalInterface
public interface GraphNode {

    /**
     * Process the current state and return the updated state.
     * 
     * @param state Current graph state
     * @return Updated graph state after processing
     */
    GraphState process(GraphState state);
}
