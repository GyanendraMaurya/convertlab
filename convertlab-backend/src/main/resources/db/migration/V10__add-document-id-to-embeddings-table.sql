-- Delete existing embeddings_1536 data if it exists
DELETE FROM embeddings_1536;

-- Add document_id column to embeddings_1536 table
ALTER TABLE embeddings_1536
ADD COLUMN document_id VARCHAR(255);

