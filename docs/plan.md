# Pathfinder 2e AI Oracle - Implementation Plan

This document describes the implementation plan for an AI-powered rules system for Pathfinder 2e using Spring Boot, Kotlin, and the Model Context Protocol (MCP).

## 1. Architecture Concept
To avoid hallucinations and provide precise answers, a **RAG approach (Retrieval-Augmented Generation)** is used, connected to existing AI models (Claude, GPT-4, etc.) via an **MCP server**.

- **Backend:** Spring Boot with Kotlin
- **AI Framework:** Spring AI
- **Interface:** Model Context Protocol (MCP)
- **Data Source:** Structured JSON data from the Pathfinder 2e Foundry VTT repository.

## 2. Implementation Phases

### Phase 1: Data Ingestion & Preprocessing
- [x] **Data Acquisition:** Download official rule JSONs (Spells, Feats, Items) from the Foundry PF2e repo.
    - **GitHub API Client** with optional token support (without token: 60 req/h, with token: 5000 req/h)
    - **Recursive traversal** of directory structure (subdirectories follow links from API response)
    - **Data model per item:**
        - `id` (UUID v7, internal ID)
        - `foundryId` (ID from JSON content)
        - `itemType` (e.g., "Feat", "Spell", "Item")
        - `itemName` (item name)
        - `rawJsonContent` (raw JSON file content)
        - `githubSha` (SHA from directory listing for change detection)
        - `lastSync` (timestamp of last sync)
        - `githubPath` (file path in repository)
    - **Change Detection:** During sync, `githubSha` is compared - only changed files are reloaded
- [x] **Parser Development:**
    - Kotlin Data Classes for extracting relevant fields (`name`, `description`, `traits`).
    - **Cleanup Logic:** Convert Foundry-specific tags (e.g., `@UUID`, `@Check`, `@Localize`) to plain text for better AI readability.
- [ ] **Vectorization with Metadata:**
    - Implement `IngestionService`.
    - **Important:** Traits and level are stored as explicit metadata in the `VectorStore` to enable hard-filtering (e.g., "search only spells with trait 'Fire'").

### Phase 2: Rules Intelligence (The Core)
- [ ] **RAG Service:** Create a service for similarity search with metadata filters.
- [ ] **Translation & Glossary Logic:**
    - Integrate a **glossary mapping** (English -> target language) for core terms (Saves, Attributes, common Traits) to ensure consistency with official translations.
    - System prompt optimization: "Respond in the user's language, keeping English technical terms in parentheses."
- [ ] **Tool Definition:** Annotate search functions with `@Tool` (Spring AI) so the AI can search specific categories (e.g., `searchSpells`, `searchFeats`).

### Phase 3: MCP Interface (The Connection)
- [ ] **MCP Configuration:** Integrate `spring-ai-mcp-starter`.
- [ ] **Tool Exposure:** Provide RAG searches as MCP tools.
- [ ] **Client Test:** Connect to Claude Desktop via Stdio for local testing.

## 3. Cost Optimization & Hosting
- **Development:** $0 using local LLMs via **Ollama** (e.g., Llama 3 or Mistral).
- **Database:** Start with `SimpleVectorStore`, migrate to **PGVector (PostgreSQL)** as data grows for persistence and efficient metadata filtering.
- **Production:** Docker container on small VPS.

> **Note on Embedding Alternatives:** Ollama requires local installation. Alternatives for vectorization:
> - **OpenAI Embeddings** - No local service needed, but requires API key
> - **Anthropic/Cloud Providers** (Cohere, Mistral API) - Also API key based
> - **Pre-computed Embeddings** - Calculate once with any tool and store in PGVector

## 4. Next Steps
1. Add Spring AI & MCP dependencies to `pom.xml`.
2. Implement `PathfinderDataImporter.kt` including `@UUID` cleanup.
3. First local test: "How does the 'Incapacitation' rule work?"
