-- Track which version of the entry has been vectorized
-- Allows incremental re-vectorization when data changes

ALTER TABLE foundry_raw_entries ADD COLUMN vectorized_sha VARCHAR(64);

CREATE INDEX idx_vectorized_sha ON foundry_raw_entries(vectorized_sha);
