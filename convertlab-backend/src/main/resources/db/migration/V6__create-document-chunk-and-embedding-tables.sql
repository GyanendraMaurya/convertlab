CREATE TABLE document_chunks
(
    id           BIGSERIAL PRIMARY KEY,
    document_id  VARCHAR(255) NOT NULL,
    chunk_index  INTEGER      NOT NULL,
    content      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_document_chunk
        UNIQUE (document_id, chunk_index)
);

CREATE TABLE embeddings_1536
(
    id                    BIGSERIAL PRIMARY KEY,
    chunk_id              BIGINT      NOT NULL,
    embedding_model       VARCHAR(100) NOT NULL,
    embedding_dimension   INTEGER     NOT NULL,
    embedding             VECTOR(1536) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_embedding_chunk
        FOREIGN KEY (chunk_id)
            REFERENCES document_chunks (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_chunk_model
        UNIQUE (chunk_id, embedding_model)
);