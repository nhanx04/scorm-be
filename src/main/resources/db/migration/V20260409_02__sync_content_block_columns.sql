ALTER TABLE content_block
ADD COLUMN IF NOT EXISTS order_index INTEGER;

ALTER TABLE content_block
ADD COLUMN IF NOT EXISTS block_type VARCHAR(50);

ALTER TABLE content_block
ADD COLUMN IF NOT EXISTS text_html TEXT;

ALTER TABLE content_block
ADD COLUMN IF NOT EXISTS theme_override JSONB;

ALTER TABLE content_block
ADD COLUMN IF NOT EXISTS layout_meta JSONB;
