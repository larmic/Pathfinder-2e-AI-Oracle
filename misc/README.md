# misc/

Development and testing resources for the Pathfinder 2e AI Oracle.

## Files

| File | Description |
|------|-------------|
| `docker-compose.yml` | Development containers (PostgreSQL with pgvector, Ollama) |
| `ollama-entrypoint.sh` | Ollama startup script with auto-pull for `nomic-embed-text` model |
| `api-requests.http` | HTTP requests for local API testing (IntelliJ HTTP Client format) |

## Directories

| Directory | Description |
|-----------|-------------|
| `experiments/` | Early experiments and explorations (not part of current implementation) |

## Usage

### Start Development Environment

```bash
make dev-start    # Start PostgreSQL + Ollama containers
make run-local    # Start app with local profile (auto-starts containers)
```

### Test API Endpoints

Open `api-requests.http` in IntelliJ IDEA and run requests against `http://localhost:8080`.

**Import API** (`/api/import/`)
- Import Foundry VTT PF2e data from GitHub
- Track import jobs and statistics

**Ingestion API** (`/api/ingestion/`)
- Vectorize imported data for RAG similarity search
- Uses Ollama `nomic-embed-text` for embeddings

### Container Management

```bash
make dev-stop     # Stop containers
make dev-clean    # Stop and remove volumes
make db-logs      # View PostgreSQL logs
make ollama-logs  # View Ollama logs
make db-psql      # Open PostgreSQL CLI
```
