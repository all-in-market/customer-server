CREATE TABLE product_image
(
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT       NOT NULL,
    image_url      TEXT NOT NULL,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    representative BOOLEAN      NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT ck_product_image_sort_order_non_negative
        CHECK (sort_order >= 0),

    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
);

CREATE INDEX idx_product_image_product_id
    ON product_image (product_id);