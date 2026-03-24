ALTER TABLE course
    ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE course
    ADD COLUMN IF NOT EXISTS cover_image_url TEXT;

