CREATE UNIQUE INDEX ux_address_default
    ON addresses (buyer_id)
    WHERE is_default = true;