CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_langchain4j_embedding_store_text_trgm
    ON langchain4j_embedding_store
    USING gin(text gin_trgm_ops);