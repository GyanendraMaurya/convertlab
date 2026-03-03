CREATE INDEX idx_embeddings_vector
ON embeddings_1536
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);