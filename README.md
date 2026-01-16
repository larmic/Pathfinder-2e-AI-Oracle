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

## Testing the MCP Server

The MCP server uses **SSE (Server-Sent Events) transport**, which requires a session to be established before sending messages. A direct POST to `/mcp/message` will return `"Session ID missing in message endpoint"` because there's no session context.

### Option A: Use an MCP Client (Recommended)

Configure an MCP-compatible client like Claude Desktop or Cursor:

**Claude Desktop** (`~/Library/Application Support/Claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "pf2e-oracle": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

### Option B: Use mcp-cli Tool

Install and use the official MCP CLI inspector:
```bash
npx @anthropic-ai/mcp-inspector http://localhost:8080/sse
```

### Option C: Manual SSE Testing with curl

1. **Open SSE connection** (in terminal 1):
   ```bash
   curl -N http://localhost:8080/sse
   ```
   This will output events including the session ID.

2. **Send message with session ID** (in terminal 2):
   ```bash
   curl -X POST "http://localhost:8080/mcp/message?sessionId=YOUR_SESSION_ID" \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
   ```

### Available MCP Tools

Once connected, the server exposes these tools:

| Tool | Description |
|------|-------------|
| `searchRules` | Search general PF2e rules, mechanics, and game information |
| `searchSpells` | Search spells with optional level filtering |
| `searchFeats` | Search feats with optional maxLevel filtering |
| `searchActions` | Search actions and activities |
| `searchEquipment` | Search equipment with optional maxLevel and rarity filtering |
| `searchConditions` | Search conditions and status effects |
| `getEntry` | Get a specific entry by its exact name |
