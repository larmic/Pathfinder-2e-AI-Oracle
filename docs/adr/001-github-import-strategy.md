# ADR-001: GitHub Import Strategy for PF2e Data

## Status
Accepted

## Context
The Pathfinder 2e AI Oracle requires rules data from the Foundry VTT PF2e repository (~27,816 JSON files under `packs/pf2e/`). This data needs to be initially imported and regularly updated.

## Decision
We use the **GitHub Tree API + Raw Download** strategy.

## Considered Alternatives

### Option 1: GitHub Contents API
**Process:** Recursively navigate through directories, fetch each file individually.

| Pro | Con |
|-----|-----|
| Easy to implement | ~28,000+ API calls for initial import |
| Direct SHA information per file | Rate limit: 60/h (no token), 5000/h (with token) |
| | Import takes 6+ hours (with token) |
| | Updates also require many API calls for SHA checks |

### Option 2: Git Clone
**Process:** Clone repository, read locally, update via `git pull`.

| Pro | Con |
|-----|-----|
| No API limits | ~500MB-1GB permanent storage |
| Fast updates via `git pull` | Resources must be permanently available |
| Local SHA comparison possible | Hosting costs with flexible storage |
| | Not green-IT friendly |

### Option 3: ZIP Download
**Process:** Download repository as ZIP, extract, parse.

| Pro | Con |
|-----|-----|
| Few API calls | ~100-200MB temporary storage |
| Fast download | No incremental update mechanism |
| | Full download required for each update |

### Option 4: Tree API + Raw Download (chosen)
**Process:**
1. One Tree API call returns all file paths + SHAs
2. Raw URLs (`raw.githubusercontent.com`) for file contents
3. Local SHA comparison for change detection

| Pro | Con |
|-----|-----|
| **1 API call** for complete file structure | Requires throttling for fair use |
| Raw URLs have **no rate limit** (CDN) | |
| No local storage needed for repo | |
| Incremental updates (only changed SHAs) | |
| Green-IT: Only load what changed | |

## Implementation Details

### API Calls
- **Tree API:** `GET /repos/{owner}/{repo}/git/trees/{branch}?recursive=1`
- **Raw Download:** `https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{path}`

### Fair Use Principles
- Max 10 parallel downloads (throttling)
- User-Agent header for identification
- Sync frequency: max once daily
- SHA-based caching: Only load changed files

### Update Flow
1. Tree API call -> current SHAs of all files
2. Compare with stored SHAs (local)
3. Only load files with changed SHA via raw URL
4. Save new SHAs

### Volume Estimates
| Operation | API Calls | Raw Downloads |
|-----------|-----------|---------------|
| Initial | 1 | ~27,816 |
| Update | 1 | ~10-100 (changes only) |

## Consequences
- Import takes minutes instead of hours
- Minimal server resources required
- Respectful use of GitHub infrastructure
- Simple change detection via SHA comparison
