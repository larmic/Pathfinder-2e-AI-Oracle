-- Reset vectorizedSha to force re-ingestion with deterministic document IDs
-- This migration is necessary because the DocumentBuilder now uses entry.id as
-- the document ID instead of random UUIDs. Re-ingestion ensures all documents
-- in the vector store have IDs matching their corresponding database entries,
-- enabling proper orphan cleanup.
UPDATE foundry_raw_entries SET vectorized_sha = NULL;
