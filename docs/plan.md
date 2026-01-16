# Pathfinder 2e AI Oracle - Implementation Plan

This document describes the implementation plan for an AI-powered rules system for Pathfinder 2e using Spring Boot, Kotlin, and the Model Context Protocol (MCP).

## 1. Architecture Concept
To avoid hallucinations and provide precise answers, a **RAG approach (Retrieval-Augmented Generation)** is used, connected to existing AI models (Claude, GPT-4, etc.) via an **MCP server**.

- **Backend:** Spring Boot with Kotlin
- **AI Framework:** Spring AI
- **Interface:** Model Context Protocol (MCP)
- **Data Source:** Structured JSON data from the Pathfinder 2e Foundry VTT repository.

## 2. Implementation Phases

### Phase 1: Data Ingestion & Preprocessing (Completed)
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
- [x] **Vectorization with Metadata:**
    - Implement `IngestionService`.
    - **Important:** Traits and level are stored as explicit metadata in the `VectorStore` to enable hard-filtering (e.g., "search only spells with trait 'Fire'").

### Phase 2: Rules Intelligence (The Core) - Planned
- [ ] **RAG Service:** Create a service for similarity search with metadata filters.
    - New file: `src/main/kotlin/de/larmic/pf2e/rag/RagService.kt`
    - Use `@Tool` annotations for MCP exposure
    - Example methods: `searchRules()`, `searchSpells()`, `searchFeats()`, `getEntry()`
- [ ] **OpenAI Embeddings for Production:** Add configurable embedding provider.
    - Add dependency to `pom.xml`:
      ```xml
      <dependency>
          <groupId>org.springframework.ai</groupId>
          <artifactId>spring-ai-starter-model-openai</artifactId>
      </dependency>
      ```
    - Create `application-openai.yml`:
      ```yaml
      spring:
        ai:
          ollama:
            embedding:
              enabled: false
          openai:
            api-key: ${OPENAI_API_KEY}
            embedding:
              model: text-embedding-3-small
      ```
    - Usage: `SPRING_PROFILES_ACTIVE=openai OPENAI_API_KEY=sk-... ./mvnw spring-boot:run`
    - Cost estimation: ~$0.00002 per 1K tokens (~$1.20/month for 10,000 requests/day)
- [ ] **Translation & Glossary Logic:**
    - Integrate a **glossary mapping** (English -> target language) for core terms (Saves, Attributes, common Traits) to ensure consistency with official translations.
    - System prompt optimization: "Respond in the user's language, keeping English technical terms in parentheses."
- [ ] **Tool Definition:** Annotate search functions with `@Tool` (Spring AI) so the AI can search specific categories (e.g., `searchSpells`, `searchFeats`).

### Phase 3: MCP Interface (The Connection) - Planned
- [ ] **MCP Configuration:** Integrate `spring-ai-mcp-starter`.
- [ ] **Tool Exposure:** Provide RAG searches as MCP tools.
- [ ] **Client Test:** Connect to Claude Desktop via Stdio for local testing.

## 3. Cost Optimization & Hosting

### Embedding Provider Options
| Provider | Profile | Use Case | Cost |
|----------|---------|----------|------|
| Ollama | `local` (default) | Local development | $0 |
| OpenAI | `openai` | Production, scalable | ~$1.20/month |

### Infrastructure
- **Database:** PGVector (PostgreSQL) for vector storage and metadata filtering
- **Production:** Docker container on small VPS
- **Important:** Ollama must be running for embedding generation (both data ingestion AND query processing)

## 4. Current Status

The project has completed **Phase 1** with the following components implemented:

- **GitHub Import Service:** Downloads Foundry VTT JSON data with incremental sync (SHA-based change detection)
- **Foundry Content Parser:** Cleans up Foundry-specific tags (~14 types) for AI readability
- **Ingestion Service:** Vectorizes entries into PGVector with metadata for filtering
- **REST API:** Endpoints for triggering import/ingestion jobs and tracking status
- **Async Job System:** Properly managed coroutine scopes with lifecycle-aware job execution

**Next steps:** Implement Phase 2 (RAG Service with `@Tool` annotations) and Phase 3 (MCP Server).
