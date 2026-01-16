# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pathfinder 2e AI Oracle - a multilingual AI-powered rules assistant for the Pathfinder 2e tabletop RPG. Uses RAG (Retrieval-Augmented Generation) to provide accurate rule answers without hallucination by querying official game data. Supports queries in any language with consistent, accurate responses.

## Tech Stack

- **Language**: Kotlin 2.3.0
- **Framework**: Spring Boot 4.0.1 with Spring AI 2.0.0-M1
- **Interface**: MCP (Model Context Protocol) server for integration with Claude, GPT-4, etc.
- **Data Source**: JSON files from the Foundry VTT PF2e repository (https://github.com/foundryvtt/pf2e)
- **Vector Store**: PGVector (PostgreSQL) for all environments
- **Embeddings**: Ollama (local development), OpenAI (production - planned)
- **JDK**: Java 25 (GraalVM CE)

## Spring Profiles

- `local`: Uses local PostgreSQL and Ollama via Docker Compose
- `openai`: Uses OpenAI for embeddings (planned)

## My Preferences

- Always write documentation in English

## Build Commands

```bash
make help      # Show all available commands
make build     # Full build (clean, compile, test)
make run       # Start application
make test      # Run tests
make package   # Create JAR
```

Alternatively with Maven Wrapper:
```bash
./mvnw compile
./mvnw test
./mvnw spring-boot:run
```

## Architecture

The system follows a RAG pattern:
1. **Data Import** (implemented): Download Foundry PF2e JSONs from GitHub with incremental sync (SHA-based change detection)
2. **Data Ingestion** (implemented): Parse and clean Foundry-specific tags (`@UUID`, `@Check`, `@Localize`), vectorize with metadata (traits, level) for filtering
3. **RAG Service** (planned): Similarity search with metadata filters exposed as Spring AI `@Tool` annotated methods (`searchSpells`, `searchFeats`, etc.)
4. **MCP Server** (planned): Exposes RAG tools via Model Context Protocol for external AI clients
5. **Translation Layer** (planned): English source data with multilingual output using glossary mappings for official terminology

## Key Design Decisions

- Traits and level stored as explicit vector store metadata to enable hard-filtering (e.g., "spells with Fire trait")
- Translations preserve English terms in parentheses for reference
- Foundry-specific markup must be converted to plain text for AI readability

## Testing Guidelines

- **Database Tests**: Always use Testcontainers with the `pgvector/pgvector:pg17` image
- **No in-memory databases**: H2 or similar are not used to ensure production parity
- **Integration tests**: Use `@SpringBootTest` with `@Testcontainers` and `@ServiceConnection`
- **Migrations**: Flyway migrations are applied in all environments (including tests)
