CREATE TABLE product_image
(
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT       NOT NULL,
    image_url      TEXT NOT NULL,
    sort_order     INTEGER,
    representative BOOLEAN      NOT NULL,
    created_at     TIMESTAMP    NOT NULL,

    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
);

CREATE INDEX idx_product_image_product_id
    ON product_image (product_id);