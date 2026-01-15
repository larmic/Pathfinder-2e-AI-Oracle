# Pathfinder 2e AI Oracle

## The Vision
A digital rules expert for Pathfinder 2e that provides game masters and players with precise, rules-compliant answers in seconds - without searching through thousands of PDF pages or websites.

Rules data is available at https://github.com/foundryvtt/pf2e/tree/v13-dev/packs/pf2e and can be imported from there.

## Why This Project?

### 1. The Problem: Complexity & Hallucinations
Pathfinder 2e is highly structured and uses precise keywords (Traits). Standard AIs like ChatGPT often hallucinate on rules questions, mixing up details or using outdated rules. A single wrong word can change an entire game mechanic.

### 2. The Solution: RAG + MCP
*   **RAG (Retrieval-Augmented Generation):** The AI no longer "guesses" but looks up official data (Archives of Nethys / Foundry JSONs) before answering.
*   **MCP (Model Context Protocol):** Instead of building a standalone app, this project acts as a "rules server" that can connect to any existing AI (Claude, GPT-4).

### 3. Multilingual Support
Source data is in English for maximum accuracy. The AI Oracle bridges languages: it queries exact English source data via RAG to prevent hallucinations, then responds in the user's language with consistent terminology.

## Why Java/Kotlin & Spring Boot?
*   **Enterprise-Grade:** Stable stack ensuring high data integrity.
*   **Spring AI:** Fast integration of vector databases and AI interfaces.
*   **Cost-Efficient:** Optimized operations via Docker containers and local AI models (Ollama).

## Target Audience
*   **Game Masters (GMs)** who need quick rule clarification during sessions.
*   **Players** who want to understand complex character interactions.
*   **Developers** seeking a reliable rules API for their own tools.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        PF2E ORACLE SYSTEM                       │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Embedding Provider (configurable)          │    │
│  │  ┌─────────────────┐     ┌─────────────────────────┐    │    │
│  │  │ Ollama (local)  │ OR  │ OpenAI (cloud/scalable) │    │    │
│  │  │ Profile: local  │     │ Profile: openai         │    │    │
│  │  └─────────────────┘     └─────────────────────────┘    │    │
│  └────────────────────────────┬────────────────────────────┘    │
│                               │                                 │
│                               ▼                                 │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────────┐    │
│  │   GitHub    │────▶│ VectorStore │◀────│   RAG Service   │    │
│  │  (PF2e Data)│     │ (PGVector)  │     │  (@Tool Search) │    │
│  └─────────────┘     └─────────────┘     └────────┬────────┘    │
│                                                   │             │
│                                                   ▼             │
│                                           ┌─────────────┐       │
│                                           │ MCP Server  │       │
│                                           └──────┬──────┘       │
└──────────────────────────────────────────────────┼──────────────┘
                                                   │
                                                   ▼
                                    ┌─────────────────────────┐
                                    │   External AI Clients   │
                                    │ (Claude Desktop, GPT-4) │
                                    └─────────────────────────┘
```

### How It Works

1. **Data Import:** PF2e rule data (spells, feats, items, etc.) is imported from the Foundry VTT GitHub repository
2. **Vectorization:** Text content is converted to embeddings using an Embedding Provider (Ollama for local dev, OpenAI for production)
3. **Storage:** Embeddings are stored in PGVector (PostgreSQL) for efficient similarity search
4. **RAG Service:** Semantic search with metadata filtering (type, level, traits, etc.)
5. **MCP Server:** Exposes search tools via Model Context Protocol
6. **AI Clients:** External AI models (Claude, GPT-4) call the MCP tools to retrieve accurate rule data

### Key Point
The embedding provider is configurable via Spring Profiles. Both providers use the same PGVector database - only the embedding generation differs.
