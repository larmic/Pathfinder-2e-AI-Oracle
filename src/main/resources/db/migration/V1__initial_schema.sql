-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Pathfinder Items table
CREATE TABLE pathfinder_items (
    id UUID PRIMARY KEY,
    foundry_id VARCHAR(255) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    raw_json_content JSONB NOT NULL,
    github_sha VARCHAR(64) NOT NULL,
    last_sync TIMESTAMP WITH TIME ZONE NOT NULL,
    github_path VARCHAR(255) NOT NULL
);

-- Indexes
CREATE UNIQUE INDEX idx_github_path ON pathfinder_items(github_path);
CREATE INDEX idx_foundry_id ON pathfinder_items(foundry_id);
CREATE INDEX idx_item_type ON pathfinder_items(item_type);
