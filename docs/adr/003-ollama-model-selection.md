# ADR-003: Ollama Model Selection for Local Development

## Status
Accepted

## Context
The RAG (Retrieval-Augmented Generation) architecture requires two fundamentally different AI capabilities:

1. **Embedding Model:** Converts text into numerical vectors for similarity search
2. **Chat Model:** Generates natural language responses from retrieved context

These are distinct tasks that benefit from specialized models rather than a single general-purpose model.

## Decision
We use **two separate Ollama models** for local development:

| Model | Type | Purpose | Vector Dimensions |
|-------|------|---------|-------------------|
| `nomic-embed-text` | Embedding | Text-to-vector conversion | 768 |
| `qwen2.5:7b` | Chat/LLM | Response generation | N/A |

## Considered Alternatives

### Option 1: Single Multimodal Model
**Approach:** Use one model (e.g., `llama3.2`) for both embeddings and chat.

| Pro | Con |
|-----|-----|
| Single model to manage | LLMs produce suboptimal embeddings |
| Simpler setup | Higher resource usage for embedding tasks |
| | Slower embedding generation |
| | Not designed for similarity search |

### Option 2: OpenAI Embeddings + Local Chat
**Approach:** Use OpenAI's `text-embedding-3-small` for embeddings, local Ollama for chat.

| Pro | Con |
|-----|-----|
| High-quality embeddings | API costs for embedding |
| Proven at scale | Requires internet connection |
| | Data leaves local environment |
| | Different embedding dimensions require migration |

### Option 3: All OpenAI
**Approach:** Use OpenAI for both embeddings and chat generation.

| Pro | Con |
|-----|-----|
| Consistent provider | Ongoing API costs |
| High quality | Internet dependency |
| Simple configuration | Privacy concerns for rule queries |
| | No offline capability |

### Option 4: Specialized Ollama Models (chosen)
**Approach:** Use `nomic-embed-text` for embeddings, `qwen2.5:7b` for chat.

| Pro | Con |
|-----|-----|
| **Free** (no API costs) | Two models to download (~5 GB total) |
| **Offline capable** | Requires sufficient RAM (16GB+ recommended) |
| Specialized models for each task | Slightly longer initial setup |
| Privacy (data stays local) | |
| `nomic-embed-text` optimized for RAG | |
| `qwen2.5:7b` excellent multilingual support | |
| `qwen2.5:7b` strong reasoning capabilities | |

## Implementation Details

### Model Specifications

**nomic-embed-text:**
- Size: ~274 MB
- Output dimensions: 768
- Optimized for: Retrieval, similarity search
- Spring AI config: `spring.ai.ollama.embedding.options.model`

**qwen2.5:7b:**
- Size: ~4.7 GB (7B parameter version)
- Context window: 128k tokens
- Optimized for: Instruction following, Q&A, multilingual (especially German)
- Spring AI config: `spring.ai.ollama.chat.options.model`
- Note: Upgraded from `llama3.2` (3B) for better multilingual support and reasoning

### RAG Workflow
```
Ingestion:   Rule text → nomic-embed-text → vector → PGVector
Retrieval:   Query → nomic-embed-text → similarity search → documents
Generation:  Documents + Query → qwen2.5:7b → Answer
```

### Configuration (application.yaml)
```yaml
spring:
  ai:
    ollama:
      embedding:
        model: nomic-embed-text
      chat:
        model: qwen2.5:7b
```

## Consequences

### Positive
- Zero API costs for local development
- Full offline capability for demos and testing
- Each model optimized for its specific task
- `nomic-embed-text` produces high-quality embeddings for RAG
- Privacy-friendly (no data leaves the machine)
- Reproducible local development environment

### Negative
- ~5 GB disk space for both models
- Requires machine with sufficient RAM (16GB+ recommended)
- Initial model download on first setup
- Two models to keep updated

### Risks
- Ollama version updates may change model behavior (mitigated by version pinning)
- Embedding dimension changes require re-vectorization (768 is stable for nomic-embed-text)
- Resource constraints on lower-spec machines
