# ADR-002: PostgreSQL + PGVector for Persistent Storage

## Status
Accepted

## Context
The initial implementation uses an in-memory `ConcurrentHashMap` (`PathfinderItemStore`) for storing PathfinderItems. This approach has significant limitations:

- Data is lost on application restart
- No vector similarity search capability for RAG
- Cannot scale horizontally (no shared state)
- Change detection (SHA comparison) must re-download all files on restart

The application requires:
1. Persistent storage for ~28,000+ Pathfinder items
2. Vector similarity search for RAG-based rule queries
3. Metadata filtering (itemType, traits, level)
4. Efficient change detection for GitHub sync

## Decision
We use **PostgreSQL with PGVector extension** as the primary database, with a hybrid architecture:
- **JPA/Hibernate** for PathfinderItem entity persistence (relational queries)
- **Spring AI PgVectorStore** for vector embeddings and similarity search (future RAG)

## Considered Alternatives

### Option 1: Pure PgVectorStore
**Approach:** Store everything in Spring AI's VectorStore only.

| Pro | Con |
|-----|-----|
| Single storage mechanism | VectorStore not designed for relational queries |
| Simpler architecture | SHA-based change detection awkward with VectorStore |
| | Limited query flexibility for non-vector lookups |
| | `findByGithubPath()` not efficiently supported |

### Option 2: Separate Vector Database (Pinecone, Weaviate, Milvus)
**Approach:** PostgreSQL for entities, external vector DB for embeddings.

| Pro | Con |
|-----|-----|
| Specialized vector performance | Additional infrastructure complexity |
| Scales independently | Two databases to manage |
| | Network latency between DBs |
| | Additional cost |
| | More complex deployment |

### Option 3: Elasticsearch
**Approach:** Use Elasticsearch for both text search and vector similarity.

| Pro | Con |
|-----|-----|
| Full-text search + vectors | Heavy resource consumption |
| Good scaling | Complex configuration |
| | Overkill for ~28,000 documents |
| | Higher operational complexity |

### Option 4: PostgreSQL + PGVector (chosen)
**Approach:** Single PostgreSQL instance with PGVector extension.

| Pro | Con |
|-----|-----|
| **Single database** instance | Slightly more complex than pure approaches |
| JPA for relational queries | Vector performance not as specialized |
| PgVectorStore for similarity search | |
| Native PostgreSQL ecosystem (backups, replication) | |
| Testcontainers integration for dev/test | |
| Docker-compose for local development | |
| **Cost-effective** (one DB) | |

## Implementation Details

### Database Structure
```
pathfinder_items (JPA)              pathfinder_vectors (Spring AI)
├── id (PK, UUID)                   ├── id (references pathfinder_items.id)
├── foundry_id                      ├── content (processed text)
├── item_type                       ├── metadata (JSON: type, traits, level)
├── item_name                       └── embedding (vector)
├── raw_json_content
├── github_sha
├── last_sync
└── github_path (unique)
```

### Spring Profiles
| Profile | Database | Use Case |
|---------|----------|----------|
| (none) | Testcontainers | IDE debugging via `TestPf2eOracleApplication` |
| `local` | docker-compose | Local development with persistent data |
| `prod` | External DB | Production deployment |

### Technology Stack
- PostgreSQL 17 with PGVector extension
- Spring Data JPA for entity management
- Spring AI PgVectorStore for vector operations
- Testcontainers for automated testing
- Docker Compose for local development

### Vector Metadata for RAG Filtering (Future)
```kotlin
mapOf(
    "itemType" to "SPELL",
    "level" to 3,
    "traits" to "Fire,Evocation",
    "source" to "Core Rulebook"
)
```

## Consequences

### Positive
- Data persists across restarts
- Efficient RAG queries with metadata filtering (future)
- SHA-based change detection works efficiently
- Single database simplifies operations
- Testcontainers enables easy local development
- No additional infrastructure cost

### Negative
- Requires PostgreSQL with PGVector extension
- Docker required for local development
- Migration needed from in-memory store

### Risks
- PGVector performance at scale (mitigated by HNSW index)
- Embedding model dimension changes require re-indexing
