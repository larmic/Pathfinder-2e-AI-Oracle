-- Rename table to reflect its purpose as raw data storage
ALTER TABLE pathfinder_items RENAME TO foundry_raw_entries;

-- Rename and change item_type to foundry_type (String instead of Enum)
ALTER TABLE foundry_raw_entries RENAME COLUMN item_type TO foundry_type;
ALTER TABLE foundry_raw_entries ALTER COLUMN foundry_type TYPE VARCHAR(64);

-- Rename item_name to name
ALTER TABLE foundry_raw_entries RENAME COLUMN item_name TO name;

-- Rename indexes
ALTER INDEX idx_item_type RENAME TO idx_foundry_type;
