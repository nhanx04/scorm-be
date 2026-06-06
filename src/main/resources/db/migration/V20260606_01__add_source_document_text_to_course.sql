-- Persist the raw text extracted from the uploaded document when a course is
-- created from a file. This becomes the grounding source for AI quiz generation
-- so questions can be anchored to the original material instead of an empty page.
ALTER TABLE course
ADD COLUMN IF NOT EXISTS source_document_text TEXT;
