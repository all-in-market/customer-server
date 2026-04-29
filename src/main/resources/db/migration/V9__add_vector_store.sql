CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS langchain4j_embedding_store (
                                                           embedding_id  UUID PRIMARY KEY,
                                                           embedding     vector(1536),
                                                           text          TEXT NULL,
                                                           metadata      JSON NULL
);