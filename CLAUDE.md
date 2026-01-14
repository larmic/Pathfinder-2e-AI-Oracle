# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pathfinder 2e AI Oracle - a multilingual AI-powered rules assistant for the Pathfinder 2e tabletop RPG. Uses RAG (Retrieval-Augmented Generation) to provide accurate rule answers without hallucination by querying official game data. Supports queries in any language with consistent, accurate responses.

## Tech Stack

- **Language**: Kotlin 2.3.0
- **Framework**: Spring Boot 4.0.1 with Spring AI 2.0.0-M1
- **Interface**: MCP (Model Context Protocol) server for integration with Claude, GPT-4, etc.
- **Data Source**: JSON files from the Foundry VTT PF2e repository (https://github.com/foundryvtt/pf2e)
- **Vector Store**: SimpleVectorStore initially, PGVector (PostgreSQL) for production
- **Local Development**: Ollama for local LLM testing
- **JDK**: Java 25 (GraalVM CE)

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
1. **Data Ingestion**: Parse Foundry PF2e JSONs (spells, feats, items), clean up Foundry-specific tags (`@UUID`, `@Check`, `@Localize`), vectorize with metadata (traits, level) for filtering
2. **RAG Service**: Similarity search with metadata filters exposed as Spring AI `@Tool` annotated methods (`searchSpells`, `searchFeats`, etc.)
3. **MCP Server**: Exposes RAG tools via Model Context Protocol for external AI clients
4. **Translation Layer**: English source data with multilingual output using glossary mappings for official terminology

## Key Design Decisions

- Traits and level stored as explicit vector store metadata to enable hard-filtering (e.g., "spells with Fire trait")
- Translations preserve English terms in parentheses for reference
- Foundry-specific markup must be converted to plain text for AI readability
